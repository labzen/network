package cn.labzen.network.tcp

import cn.labzen.network.tcp.client.TcpClientBuilder
import cn.labzen.network.tcp.client.TcpClientConfig
import cn.labzen.network.tcp.server.TcpServerBuilder
import cn.labzen.network.tcp.server.TcpServerConfig
import cn.labzen.tool.bean.StrictPair
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

object TCP {

  private val mapper = ObjectMapper().apply {
    this.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    this.propertyNamingStrategy = PropertyNamingStrategies.KEBAB_CASE

    this.registerModule(JavaTimeModule())
    this.registerModule(KotlinModule.Builder().build())
  }

  @JvmStatic
  fun server(port: Int) =
    TcpServerBuilder(TcpServerConfig(port))

  @JvmStatic
  fun client(host: String, port: Int) =
    TcpClientBuilder(TcpClientConfig(host, port))

  fun jsonString(obj: Any): String = mapper.writeValueAsString(obj)

  fun <T> jsonObject(str: String, type: Class<T>): T = mapper.readValue(str, type)

  fun <T> jsonObject(buf: ByteBuf, type: Class<T>): T = jsonObject(buf.toString(StandardCharsets.UTF_8), type)

  fun <T> jsonObject(str: String, type: TypeReference<T>): T = mapper.readValue(str, type)

  fun <T> jsonObject(buf: ByteBuf, type: TypeReference<T>): T = jsonObject(buf.toString(StandardCharsets.UTF_8), type)

  internal fun addr(channel: Channel) =
    (channel.remoteAddress() as InetSocketAddress).let {
      StrictPair<String, Int>(it.hostString, it.port)
    }

  internal fun addrString(channel: Channel) =
    addr(channel).let { "${it.first}:${it.second}" }
}
