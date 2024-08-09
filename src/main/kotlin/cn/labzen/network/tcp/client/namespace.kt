package cn.labzen.network.tcp.client

import cn.labzen.network.exception.TcpClientException
import cn.labzen.network.tcp.*
import cn.labzen.tool.feature.SystemClock
import cn.labzen.tool.util.Randoms
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption.*
import io.netty.channel.socket.SocketChannel
import org.slf4j.LoggerFactory
import java.util.function.Consumer

interface Client {

  fun isAvailable(): Boolean

  fun start()

  fun stop()

  fun reboot()

  fun notify(command: Command)

  fun exchange(command: Command): Command?
}

/**
 * 客户端
 */
abstract class AbstractClient(protected val config: TcpClientConfig) : Client {

  val host = config.host
  val port = config.port

  protected var reconnectTries = 0

  protected val core: TcpClientContainer by lazy {
    TcpClientContainer(config).also {
      TcpClientLifecycleController.warmup(config, it)
    }
  }

  protected abstract fun prepare()

  override fun isAvailable(): Boolean =
    TcpClientLifecycleController.containerConnected(host, port)

  override fun start() {
    prepare()

    TcpClientLifecycleController.boot(host, port)
  }

  override fun stop() {
    TcpClientLifecycleController.shutdown(host, port)
  }

  override fun reboot() {
    TODO("Not yet implemented")
  }

  protected fun customBootstrap(consumer: Consumer<Bootstrap>) {
    core.bootstrapCustomer = consumer
  }
}

open class SimpleTcpClient internal constructor(config: TcpClientConfig) :
  AbstractClient(config), TcpConnect2ServerListener {

  private val logger = LoggerFactory.getLogger(SimpleTcpClient::class.java)

  override fun notify(command: Command) {
    if (isAvailable()) {
      ensureNotifyCommand(command)
      val content = TCP.jsonString(command)
      val channel = core.availableChannel()
      logger.info("Client notify using channel [${channel.id().asShortText()}]")
      channel.writeAndFlush(content)
    }
  }

  private fun ensureNotifyCommand(command: Command) {
    command.meta ?: run { command.meta = CommandMeta(UNDEFINED_EVENT, Randoms.string(5)) }
    command.meta!!.sendAt = SystemClock.now()
  }

  override fun exchange(command: Command): Command? {
    throw TcpClientException("TcpClient 不支持 exchange 功能")
  }

  override fun prepare() {
    core.connect2ServerListener = this

    super.customBootstrap { bootstrap: Bootstrap ->
      bootstrap.option(CONNECT_TIMEOUT_MILLIS, config.connectTimeout)
      bootstrap.option(SO_REUSEADDR, true)
      bootstrap.option(TCP_NODELAY, true)
      bootstrap.option(SO_SNDBUF, Integer.MAX_VALUE)
      bootstrap.option(AUTO_CLOSE, false)

      val initializer: ChannelInitializer<SocketChannel> = TcpClientChannelInitializer(config)
      bootstrap.handler(initializer)
    }
  }

  override fun success() {
    reconnectTries = 0
    config.connect2ServerListener?.success()
  }

  override fun failed(throwable: Throwable?) {
    config.connect2ServerListener?.failed(throwable)
  }

  override fun reconnectFailed(throwable: Throwable?) {
    config.connect2ServerListener?.reconnectFailed(throwable)
  }

}

class BothWayTcpClient(config: TcpClientConfig) : SimpleTcpClient(config) {

  private val logger = LoggerFactory.getLogger(BothWayTcpClient::class.java)

  override fun exchange(command: Command): Command? {
    if (!isAvailable()) {
      return null
    }

    ensureExchangeCommand(command)
    val content = TCP.jsonString(command)

    val identifier = command.meta!!.identifier
    TcpCommandExchanger.enroll(identifier, command)
    val channel = core.availableChannel()
    logger.info("Client exchange using channel [${channel.id().asShortText()}]")
    channel.writeAndFlush(content)
    return TcpCommandExchanger.waiting(identifier)
  }

  private fun ensureExchangeCommand(command: Command) {
    command.meta ?: run { command.meta = CommandMeta(UNDEFINED_EVENT, Randoms.string(5)) }
    with(command.meta!!) {
      feedback(1000)
      sendAt = SystemClock.now()
    }
  }
}
