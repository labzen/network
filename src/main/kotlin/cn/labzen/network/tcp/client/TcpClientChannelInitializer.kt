package cn.labzen.network.tcp.client

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.json.JsonObjectDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class TcpClientChannelInitializer internal constructor(private val config: TcpClientConfig) :
  ChannelInitializer<SocketChannel>() {

  private val handlerAdapter = TcpClientHandlerAdapter(config.handlerClass.getConstructor().newInstance(), config)

  override fun initChannel(ch: SocketChannel) {
    ch.pipeline()
      .addLast(
        JsonObjectDecoder(),
        StringEncoder(StandardCharsets.UTF_8),
        IdleStateHandler(0, 0, config.pulseInterval, TimeUnit.SECONDS),
        handlerAdapter
      )
  }
}
