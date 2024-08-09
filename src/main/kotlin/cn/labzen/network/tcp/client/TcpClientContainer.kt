package cn.labzen.network.tcp.client

import cn.labzen.tool.kotlin.runIf
import cn.labzen.tool.kotlin.runUnless
import cn.labzen.network.exception.TcpClientException
import cn.labzen.network.exception.TcpServerException
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

/**
 * 客户端核心容器
 */
open class TcpClientContainer internal constructor(val config: TcpClientConfig) {

  private val logger = LoggerFactory.getLogger(TcpClientContainer::class.java)

  private lateinit var worker: EventLoopGroup
  private lateinit var bootstrap: Bootstrap

  private val channel = AtomicReference<Channel?>(null)

  internal lateinit var bootstrapCustomer: Consumer<Bootstrap>
  internal lateinit var connect2ServerListener: TcpConnect2ServerListener

  internal fun availableChannel(): Channel {
    channel.get() ?: throw TcpServerException("无可靠连接可用")
    return channel.get()!!
  }

  internal fun init() {
    bootstrap = Bootstrap()

    group().apply {
      worker = this.first
      bootstrap.group(worker).channel(this.second)
    }
    bootstrap.remoteAddress(InetSocketAddress(config.host, config.port))
    bootstrapCustomer.accept(bootstrap)
  }

  /**
   * 如果支持epoll技术（Linux下），优先使用
   */
  private fun group() =
    if (Epoll.isAvailable()) {
      // 如果支持epoll技术（Linux下），优先使用
      logger.info("Netty Client using Linux EPOLL Mode.. [${config.host}:${config.port}] Connecting")
      Pair(EpollEventLoopGroup(), EpollSocketChannel::class.java)
    } else {
      logger.info("Netty Client using NIO Mode.. [${config.host}:${config.port}] Connecting")
      Pair(NioEventLoopGroup(), NioSocketChannel::class.java)
    }

  /**
   * @return exit without exception
   */
  internal fun connect(isLastChance: Boolean = false, connectResultCallback: Consumer<Boolean>): Boolean {
    var channelFuture: ChannelFuture? = null
    logger.info("Netty Client Container is connecting to ${config.host}:${config.port}")
    return try {
      channelFuture = bootstrap.connect().addListener {
        when {
          it.isSuccess -> {
            val ch = (it as ChannelFuture).channel()
            logger.info(
              "Netty Client Connect to ${config.host}:${config.port} Successful, " +
                  "with channel [${ch.id().asShortText()}:${(ch.localAddress() as InetSocketAddress).port}]"
            )

            channel.set(ch)
            connectResultCallback.accept(true)
            connect2ServerListener.success()
          }
          (it.cause() is ConnectException && it.cause().message?.startsWith("connection timed out: ") == true) -> {
            if (!isLastChance) {
              connect2ServerListener.reconnectFailed(TcpClientException(it.cause(), "连接超时"))
            } else {
              connect2ServerListener.failed(TcpClientException(it.cause(), "连接超时"))
            }
            connectResultCallback.accept(false)
          }
          else -> {
            logger.warn("Netty Client Connect to ${config.host}:${config.port} Failed")
            if (!isLastChance) {
              connect2ServerListener.reconnectFailed()
            } else {
              connect2ServerListener.failed()
            }
            connectResultCallback.accept(false)
          }
        }
      }.sync()

      // 线程在这里会阻塞住
      channelFuture.channel().closeFuture().sync()
      val channel = channelFuture.channel()
      logger.info(
        "Netty Client Channel [${
          channel.id().asShortText()
        }:${(channel.localAddress() as InetSocketAddress).port}] is disconnected"
      )

      true
    } catch (e: Exception) {
      logger.error("Netty Client Connect to ${config.host}:${config.port} With Exception - ${e.message}")
      connectResultCallback.accept(false)

      false
    } finally {
      channel.set(null)
      channelFuture?.run {
        closeFully(this.channel())
      }
      worker.shutdownGracefully()
    }
  }

  internal fun destroy() {
    channel.get()?.let {
      channel.set(null)
      closeFully(it)
    }
  }

  private fun closeFully(channel: Channel) {
    channel.metadata().hasDisconnect().runUnless {
      channel.disconnect()
    }

    channel.isOpen.runIf {
      channel.close()
    }

    channel.isRegistered.run {
      channel.deregister()
    }
  }
}
