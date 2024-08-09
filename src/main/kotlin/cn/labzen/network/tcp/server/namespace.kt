package cn.labzen.network.tcp.server

import cn.labzen.tool.feature.SystemClock
import cn.labzen.tool.util.Randoms
import cn.labzen.network.tcp.*
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption.*
import java.util.function.Consumer

interface Server {

  fun start()

  fun notify(ip: String, command: Command)

  fun exchange(ip: String, command: Command): Command?

  fun stop()
}

abstract class AbstractServer(protected val config: TcpServerConfig) : Server {

  val port = config.port

  protected val core: TcpServerContainer by lazy {
    TcpServerContainer(config)
  }

  protected abstract fun prepare()

  override fun start() {
    prepare()

    core.execute()
  }

  override fun stop() {
    TODO("Not yet implemented")
  }

  protected fun customBootstrap(consumer: Consumer<ServerBootstrap>) {
    core.bootstrapCustomer = consumer
  }
}

class DefaultTcpServer internal constructor(config: TcpServerConfig) : AbstractServer(config), TcpServerBootListener {

  init {
    core.bootListener = this
  }

  override fun notify(ip: String, command: Command) {
    ensureNotifyCommand(command)
    val content = TCP.jsonString(command)

    val channel = TcpChannelHolder.available(ip)
    channel.writeAndFlush(content)
  }

  override fun exchange(ip: String, command: Command): Command? {
    ensureExchangeCommand(command)
    val content = TCP.jsonString(command)

    val identifier = command.meta!!.identifier
    TcpCommandExchanger.enroll(identifier, command)

    val channel = TcpChannelHolder.available(ip)
    channel.writeAndFlush(content)
    return TcpCommandExchanger.waiting(identifier)
  }

  private fun ensureNotifyCommand(command: Command) {
    command.meta ?: run { command.meta = CommandMeta(UNDEFINED_EVENT, Randoms.string(5)) }
    command.meta!!.sendAt = SystemClock.now()
  }

  private fun ensureExchangeCommand(command: Command) {
    command.meta ?: run { command.meta = CommandMeta(UNDEFINED_EVENT, Randoms.string(5)) }
    with(command.meta!!) {
      feedback(1000)
      sendAt = SystemClock.now()
    }
  }

  override fun prepare() {
    super.customBootstrap { bootstrap: ServerBootstrap ->
      val initializer = TcpServerChannelInitializer(config)
      bootstrap.childHandler(initializer)
      bootstrap.option(SO_REUSEADDR, true)
      bootstrap.option(TCP_NODELAY, true)
      bootstrap.option(SO_BACKLOG, 512)
      bootstrap.option(SO_RCVBUF, Integer.MAX_VALUE)
      bootstrap.option(AUTO_CLOSE, false)
    }
  }

  override fun success() {
    config.bootListener?.success()
  }

  override fun failed(throwable: Throwable?) {
    config.bootListener?.failed(throwable)
  }
}
