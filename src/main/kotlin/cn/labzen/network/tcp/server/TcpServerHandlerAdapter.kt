package cn.labzen.network.tcp.server

import cn.labzen.network.tcp.TCP
import cn.labzen.network.tcp.TcpCommandDispatcher
import cn.labzen.network.tcp.TcpCommunicationMode
import cn.labzen.network.tcp.TcpHandler
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.slf4j.LoggerFactory

@ChannelHandler.Sharable
class TcpServerHandlerAdapter(
  private val internalHandler: TcpHandler,
  private val config: TcpServerConfig
) : ChannelInboundHandlerAdapter() {

  private val logger = LoggerFactory.getLogger(TcpServerHandlerAdapter::class.java)

  override fun channelActive(ctx: ChannelHandlerContext) {
    super.channelActive(ctx)
    val channel = ctx.channel()
    TcpChannelHolder.hold(channel)
    val address = TCP.addr(channel)
    config.clientComingListener?.trigger(address.first, address.second)
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    super.channelInactive(ctx)
    val channel = ctx.channel()
    TcpChannelHolder.disconnected(channel)
    val address = TCP.addr(channel)
    config.clientLeftListener?.trigger(address.first, address.second)
  }

  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    TcpCommandDispatcher.dispatch(ctx, msg, internalHandler, TcpCommunicationMode.BOTH_WAY)
  }

  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    internalHandler.failed(cause)
  }

  override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
    if (evt is IdleStateEvent) {
      if (evt.state() == IdleState.READER_IDLE) {
        val channel = ctx.channel()
        val address = TCP.addr(channel)
        logger.warn("与客户端失联，正在断开与 [${address.first}:${address.second}] 的连接channel")
        config.clientLostListener?.trigger(address.first, address.second)
//        ctx.channel().close()
        TcpChannelHolder.disconnected(channel)
      }
    } else {
      super.userEventTriggered(ctx, evt)
    }
  }
}
