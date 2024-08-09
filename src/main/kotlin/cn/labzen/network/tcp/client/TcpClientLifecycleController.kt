package cn.labzen.network.tcp.client

import cn.labzen.tool.kotlin.applyIf
import cn.labzen.tool.kotlin.applyUnless
import cn.labzen.tool.kotlin.throwRuntimeIf
import cn.labzen.network.exception.TcpClientException
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal object TcpClientLifecycleController {

  private val logger = LoggerFactory.getLogger(TcpClientLifecycleController::class.java)

  private val lifecycles = mutableMapOf<String, TcpClientContainerLifecycle>()
  private val commands = LinkedBlockingQueue<LifecycleCommand>()

  init {
    thread(isDaemon = true, name = "TCP-Client-Lifecycle-Control") {
      while (true) {
        executeCommand(commands.take())
      }
    }

    logger.info(
      """
        Netty TCP Client Reconnect Log Format: "TCP-CLIENT-127.0.0.1:4560-T0:0:0"
          Last 3 numbers separated by a colon meaning:
          1. reconnect buffering times
          2. connect successful times
          3. reconnect try times between buffering
      """.trimIndent()
    )
  }

  private fun executeCommand(command: LifecycleCommand) =
    lifecycleFromCommand(command).let {
      when (command.event) {
        LifecycleEvent.BOOT -> {
          it.state.terminated.throwRuntimeIf { TcpClientException("TCP Client实例已终止（无法建立连接）") }
          it.state.booted.throwRuntimeIf { TcpClientException("TCP Client实例已启动") }

          it.boot()
        }
        LifecycleEvent.RECONNECT -> {
          it.state.connected.throwRuntimeIf { TcpClientException("TCP Client实例已启动") }

          it.reconnect()
        }
        LifecycleEvent.RECONNECT_BUFFERING -> it.reconnectBuffering()
        LifecycleEvent.SHUTDOWN -> {
          it.state.terminated.throwRuntimeIf { TcpClientException("TCP Client实例已终止") }

          it.shutdown()
        }
      }
    }

  private fun lifecycleFromCommand(command: LifecycleCommand) =
    lifecycles[key(command.host, command.port)]
      ?: throw TcpClientException("不存在的TCP Client实例：[${command.host}, ${command.port}]")

  fun warmup(config: TcpClientConfig, container: TcpClientContainer) {
    val key = key(config.host, config.port)
    val existLifecycle = lifecycles[key]
    existLifecycle?.run {
      when {
        state.terminated -> {
          // normal, do nothing
        }
        state.connected -> throw TcpClientException("客户端存在一个已连接的Channel ${channel().id().asShortText()}")
        state.booted -> throw TcpClientException("客户端存在一个尚未终止的Channel ${channel().id().asShortText()}")
      }
    }

    lifecycles[key] = TcpClientContainerLifecycle(config, container)
  }

  fun boot(host: String, port: Int) {
    logger.info("Command 'BOOT'!!")
    commands.put(LifecycleCommand(LifecycleEvent.BOOT, host, port))
  }

  fun shutdown(host: String, port: Int) {
    logger.info("Command 'SHUTDOWN'!!")
    commands.put(LifecycleCommand(LifecycleEvent.SHUTDOWN, host, port))
  }

  fun containerConnected(host: String, port: Int) =
    lifecycles[key(host, port)]?.state?.connected ?: false

  private fun key(host: String, port: Int) =
    "$host:$port"

  private class TcpClientContainerLifecycleExecutor : ThreadPoolExecutor(
    1, 1,
    0L, TimeUnit.MILLISECONDS,
    LinkedBlockingQueue()
  ) {

    var tn: String = "TcpClientContainerLifecycleExecutor"

    override fun beforeExecute(t: Thread, r: Runnable) {
      super.beforeExecute(t, r)
      t.name = tn
    }
  }

  private class TcpClientContainerLifecycle(
    private val config: TcpClientConfig,
    private val target: TcpClientContainer
  ) {

    private val host = config.host
    private val port = config.port
    private val executor = TcpClientContainerLifecycleExecutor()

    val state = LifecycleState()

    // todo 等稳定了，将>>>> 0.这些日志去除
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    fun boot() {
      val tn = "NETTY-TCP-CLIENT-${host}:${port}-BOOT"
      executor.tn = tn
      logger.info("Netty Client is booting in the new thread [$tn]")

      CompletableFuture.runAsync({
        target.init()
        logger.info(">>>> 0. tcp client container init finished")
      }, executor).thenRun {
        state.booted = true
        logger.info(">>>> 1. tcp client container state set to booted")
      }.thenAccept {
        logger.info(">>>> 2. tcp client container would be connected to server")
        // 直接使用 ::connectResult 传参，在某些环境下，会造成connect()方法不进入的问题
        target.connect(false) {
          connectResult(it)
        }
        logger.info(">>>> 3. tcp client container's connection is broke")
      }.whenComplete { exitWithoutException, _ ->
        // ignore exitWithoutException
        logger.info(">>>> 4. tcp client container will try to reconnect")
        state.connected = false
        tryNextReconnect()
      }.exceptionally {
        throw TcpClientException(it, "卧了个槽！！！")
      }
    }

    fun shutdown() {
      state.booted = false
      state.connected = false
      state.terminated = true
      target.destroy()
    }

    fun channel() =
      target.availableChannel()

    private fun makeReconnectTaskName() =
      "NETTY-TCP-CLIENT-${host}:${port}-T${state.retryBufferingTimes}:${state.connectCount}:${state.retryTimes}".apply {
        executor.tn = this
      }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    fun reconnect() {
      val isLastChance = state.retryTimes >= config.reconnectTimes

      val tn = makeReconnectTaskName()
      logger.warn("Client try to reestablish the connection to $host:$port for the ${state.retryTimes + 1} times in new thread [$tn] ${config.reconnectInterval} ms later.")

      CompletableFuture.runAsync({
        Thread.sleep(config.reconnectInterval)
        state.retryTimes++
      }, executor).thenRun {
        target.init()
      }.thenAccept {
        target.connect(isLastChance) {
          connectResult(it)
        }
      }.whenComplete { exitWithoutException, _ ->
        // ignore exitWithoutException
        state.connected = false
        tryNextReconnect()
      }
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    fun reconnectBuffering() {
      state.retryTimes = 0
      logger.warn("Client will start reconnect ${config.reconnectTimes} times again ${config.reconnectIntervalLevel2} ms later.")
      executor.tn = "NETTY-TCP-CLIENT-${host}:${port}-BUFFERING"
      CompletableFuture.runAsync({
        Thread.sleep(config.reconnectIntervalLevel2)
        state.retryBufferingTimes++
      }, executor).whenComplete { exitWithoutException, _ ->
        // ignore exitWithoutException
        tryNextReconnect()
      }
    }

    private fun tryNextReconnect() =
      (state.retryTimes < config.reconnectTimes).applyIf {
        if (!state.terminated) {
          logger.info("Command 'RECONNECT'!!")
          commands.put(LifecycleCommand(LifecycleEvent.RECONNECT, host, port))
        }
      }.applyUnless {
        logger.warn("Client made ${state.retryTimes} attempts to establish the connection, but all failed")
        tryReconnectBuffering()
      }

    private fun tryReconnectBuffering() {
      config.reconnectIntervalLevel2Enabled.applyIf {
        logger.info("Command 'BUFFERING'!!")
        commands.put(LifecycleCommand(LifecycleEvent.RECONNECT_BUFFERING, host, port))
      }.applyUnless {
        shutdown()
        logger.info("Invoke reboot() function to reboot client, but it probably won't work")
      }
    }

    private fun connectResult(connected: Boolean) {
      state.connected = connected
      if (connected) {
        state.retryTimes = 0
        state.connectCount++
      }
    }
  }

  private data class LifecycleState(
    var booted: Boolean = false,
    var connected: Boolean = false,
    var connectCount: Int = 0,
    var retryTimes: Int = 0,
    var retryBufferingTimes: Int = 0,
    var terminated: Boolean = false
  )

  private data class LifecycleCommand(val event: LifecycleEvent, val host: String, val port: Int)

  private enum class LifecycleEvent {
    BOOT, RECONNECT, RECONNECT_BUFFERING, SHUTDOWN
  }
}
