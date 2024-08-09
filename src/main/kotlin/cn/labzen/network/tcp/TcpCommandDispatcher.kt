package cn.labzen.network.tcp

import cn.labzen.tool.feature.SystemClock
import cn.labzen.tool.util.Randoms
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.util.ReferenceCountUtil
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

object TcpCommandDispatcher {

  private val logger = LoggerFactory.getLogger(TcpCommandDispatcher::class.java)

  /**
   * todo 考虑对command.data使用binary序列化
   */
  internal fun dispatch(ctx: ChannelHandlerContext, msg: Any, handler: TcpHandler, mode: TcpCommunicationMode) {
    val command: Command = TCP.jsonObject(msg as ByteBuf, Command::class.java)

    if (command.meta == null) {
      logger.warn("通讯命令中无元数据meta，忽略..")
      return
    }

    val meta = command.meta!!
    val channel = ctx.channel()
    try {
      when {
        // 内置心跳机制
        meta.event == TCP_PULSE_EVENT -> {
          // 如果是 pong 则忽略
          logger.debug("received pulse {} ...", command.data)
          if (meta.feedbackRequired) {
            ctx.writeAndFlush(PULSE_PONG)
          }
        }
        // 从对方端发回exchange命令的回馈内容
        command.original?.identifier != null -> {
          TcpCommandExchanger.reply(command)
        }
        // 从对方端发来需回馈的exchange命令
        meta.feedbackRequired -> {
          if (mode == TcpCommunicationMode.SIMPLE) {
            logger.warn("接收到Exchange命令，但当前客户端设置不支持")
          } else {
            logger.debug("Received tcp exchange command in channel [${channel.id().asShortText()}]")

            val receivedTime = SystemClock.now()
            val address = TCP.addrString(channel)
            val exchangeReply = (handler as BothWayTcpHandler).handleExchange(address, command)

            exchangeReply.original = CommandOriginal(meta.identifier, receivedTime)

            exchangeReply.meta ?: run {
              exchangeReply.meta = CommandMeta(meta.event, Randoms.string(5)).also {
                it.sendAt = SystemClock.now()
              }
            }
            sendResponse(ctx, exchangeReply)
          }
        }
        // 从对方端发来不需回馈的notify命令
        !meta.feedbackRequired -> {
          logger.debug("Received tcp notify command in channel [${channel.id().asShortText()}]")
          val addr = TCP.addrString(channel)
          (handler as SimpleTcpHandler).handleNotify(addr, command)
        }
        else -> logger.warn("不规范的通讯命令格式：$msg")
      }
    } finally {
      ReferenceCountUtil.release(msg)
    }
  }

  private fun sendResponse(ctx: ChannelHandlerContext, command: Command) {
    val content = TCP.jsonString(command)
    val message = Unpooled.buffer()
    message.writeCharSequence(content, StandardCharsets.UTF_8)
    ctx.writeAndFlush(message)
  }
}
