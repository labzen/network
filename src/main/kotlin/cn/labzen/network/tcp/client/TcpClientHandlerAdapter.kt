package cn.labzen.network.tcp.client

import cn.labzen.network.exception.TcpClientException
import cn.labzen.network.tcp.*
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.slf4j.LoggerFactory
import java.io.IOException

@ChannelHandler.Sharable
class TcpClientHandlerAdapter(
  private val internalHandler: TcpHandler,
  private val config: TcpClientConfig
) : ChannelInboundHandlerAdapter() {

  private val logger = LoggerFactory.getLogger(TcpClientHandlerAdapter::class.java)
  private val mode = when (internalHandler) {
    is BothWayTcpHandler -> TcpCommunicationMode.BOTH_WAY
    is SimpleTcpHandler -> TcpCommunicationMode.SIMPLE
    else -> throw TcpClientException("不可能的异常")
  }

  private var pulseRetryTimes = 0

  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    pulseRetryTimes = 0
    TcpCommandDispatcher.dispatch(ctx, msg, internalHandler, mode)
  }

  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    // 当本机网线主动断开时才会有异常抛出，如果中间有交换机，非本机网线的断开，这里并不会捕捉到异常，而是会借由心跳机制超时，判断服务器丢失。只有在网络恢复的一瞬间，才可能有异常抛出
    // 当网线被拔掉或断网的时候，会有异常抛出至此处，暂时没想到更好的办法，非常准确的判断该因素
    val networkDisconnected = cause is IOException && cause.message?.contains("远程主机强迫关闭了一个现有的连接") == true

    when {
      networkDisconnected -> {
        logger.warn("远程主机强迫关闭了一个现有的连接")
        ctx.close()

        config.serverLeftListener?.trigger()
      }
      else -> internalHandler.failed(cause)
    }
  }

  override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
    if (evt is IdleStateEvent) {
      if (evt.state() == IdleState.ALL_IDLE) {
        if (++pulseRetryTimes > config.pulseRetryLimit) {
          logger.warn("Lost Server [${config.host}:${config.port}] Connection")

          ctx.close()
          config.serverLostListener?.trigger()
        } else {
          logger.debug("Ping to Server [${config.host}:${config.port}] with channel [${ctx.channel().id()}]")
          ctx.writeAndFlush(PULSE_PING)
        }
      }
    } else {
      super.userEventTriggered(ctx, evt)
    }
  }
}
