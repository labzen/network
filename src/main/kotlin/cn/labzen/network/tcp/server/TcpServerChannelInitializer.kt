package cn.labzen.network.tcp.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.json.JsonObjectDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class TcpServerChannelInitializer internal constructor(private val config: TcpServerConfig) :
  ChannelInitializer<SocketChannel>() {

  private val handlerAdapter = TcpServerHandlerAdapter(config.handlerClass.getConstructor().newInstance(), config)

  override fun initChannel(ch: SocketChannel) {
    ch.pipeline()
      .addLast(
        JsonObjectDecoder(),
        StringEncoder(StandardCharsets.UTF_8),
        IdleStateHandler(config.pulseTimeout, 0, 0, TimeUnit.SECONDS),
        handlerAdapter
      )
  }
}
