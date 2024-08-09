package cn.labzen.network.tcp.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.EventLoopGroup
import io.netty.channel.ServerChannel
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import kotlin.concurrent.thread

class TcpServerContainer internal constructor(val config: TcpServerConfig) {

  private val logger = LoggerFactory.getLogger(TcpServerContainer::class.java)

  private lateinit var boss: EventLoopGroup
  private lateinit var worker: EventLoopGroup
  private lateinit var bootstrap: ServerBootstrap

  internal lateinit var bootstrapCustomer: Consumer<ServerBootstrap>
  internal lateinit var bootListener: TcpServerBootListener

  internal fun execute() {
    val threadName = "NETTY-TCP-SERVER-${config.port}"
    logger.info("Netty Server is Ready to Boot at port ${config.port}")
    thread(start = true, name = threadName) {
      synchronized(this) {
        init()

        connect()
      }
    }
  }

  private fun init() {
    bootstrap = ServerBootstrap()

    groups().apply {
      boss = this.first
      worker = this.second
      bootstrap.group(boss, worker).channel(this.third)
    }
    bootstrapCustomer.accept(bootstrap)
  }

  @Suppress("DuplicatedCode")
  private fun groups(): Triple<EventLoopGroup, EventLoopGroup, Class<out ServerChannel>> =
    if (Epoll.isAvailable()) {
      // 如果支持epoll技术（Linux下），优先使用
      logger.info("Netty Server using Linux EPOLL Mode.. Port [${config.port}] Starting")
      Triple(EpollEventLoopGroup(1), EpollEventLoopGroup(), EpollServerSocketChannel::class.java)
    } else {
      logger.info("Netty Server using NIO Mode..  Port [${config.port}] Starting")
      Triple(NioEventLoopGroup(1), NioEventLoopGroup(), NioServerSocketChannel::class.java)
    }

  fun connect() {
    var channelFuture: ChannelFuture? = null
    try {
      channelFuture = bootstrap.bind(config.port).addListener {
        if (it.isSuccess) {
          logger.info("Server at ${config.port} Started Successful")
          bootListener.success()
        } else {
          logger.warn("Server at ${config.port} Start Failed")
          bootListener.failed()
        }
      }.sync()

      // 线程在这里会阻塞住
      channelFuture.channel().closeFuture().sync()
    } catch (e: InterruptedException) {
      logger.error("Server at ${config.port} Start With Exception", e)
    } finally {
      channelFuture?.run {
        val ch = this.channel()
        if (ch != null && ch.isOpen) {
          ch.close()
        }
      }

      // 优雅退出，释放线程池资源
      worker.shutdownGracefully()
      boss.shutdownGracefully()
    }
  }
}
