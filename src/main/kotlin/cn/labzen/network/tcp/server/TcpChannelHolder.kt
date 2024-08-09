package cn.labzen.network.tcp.server

import cn.labzen.tool.kotlin.applyIf
import cn.labzen.tool.kotlin.applyUnless
import cn.labzen.tool.kotlin.runIf
import cn.labzen.network.exception.TcpServerException
import cn.labzen.network.tcp.TCP
import com.google.common.collect.HashMultimap
import com.google.common.collect.Maps
import io.netty.channel.Channel
import org.slf4j.LoggerFactory
import java.util.*

internal object TcpChannelHolder {

  private val logger = LoggerFactory.getLogger(TcpChannelHolder::class.java)

  private val channels = HashMultimap.create<String, ChannelArchive>()

  // key 为 IP
  private val latestAvailableChannels = Maps.newHashMap<String, ChannelArchive>()

  @Synchronized
  fun hold(channel: Channel) {
    val (ip, port) = TCP.addr(channel)
    val id = channel.id().asShortText()
    val archive = ChannelArchive(id, ip, port, channel, Date())

    val exists = channels[ip].filter { !it.obsolete && it.channel.isWritable }
    exists.isNotEmpty().runIf {
      val existChannels = exists.joinToString { it.id }
      logger.warn("Channel cache have available channels [$existChannels], but there is new channel [$id] coming again")
      logger.warn("Available channel is change from [${latestAvailableChannels[ip]}] to [$id]")
    }

    channels.put("$ip:$port", archive)
    latestAvailableChannels[ip] = archive
    logger.info("Channel [$id] from client [$ip:$port] has been saved into TcpChannelHolder")
  }

  fun disconnected(channel: Channel) {
    val id = channel.id().asShortText()

    val (ip, port) = TCP.addr(channel)
    val archive = channelsUnderClient("$ip:$port").find {
      it.id == id
    } ?: throw TcpServerException("服务端从未登记过 channel [$id]")

    val latestArchive = latestAvailableChannels[ip]
    val uniform = archive.id == latestArchive?.id

    // 如果 channel 已经不可用，可忽略
    archive.obsolete.applyUnless {
      archive.obsolete = true
      archive.disconnectedAt = Date()

      archive.channel.disconnect()
      archive.channel.close()
      archive.channel.deregister()
      logger.info("Channel [$id] from client [$ip:$port] has been disconnected and destroyed")
    }.applyIf {
      logger.warn("Channel [$id] is already obsolete")
    }

    if (uniform) {
      latestAvailableChannels.remove(ip)
      logger.info("No channel is currently available")
    } else if (latestArchive != null) {
      logger.warn("服务端最后一次有效的 channel [${latestArchive.id}] 与目前中断的 channel [$id] 不一致")
    }
  }

  fun available(ip: String): Channel {
    val latestArchive = latestAvailableChannels[ip]

    if (latestArchive?.obsolete == false) {
      logger.debug("Server using channel [${latestArchive.id}] to message client [${latestArchive.address}]")
      return latestArchive.channel
    }

    latestArchive ?: logger.warn("Server have not recorded fast cache of channel with client [$ip], will find it")

    val cuc = channelsUnderClient(ip)
    val archive = cuc.find {
      !it.obsolete && it.channel.isWritable
    } ?: throw TcpServerException("在所有登记过的(size = ${cuc.size}) channel中，没有可用的有效channel")

    logger.debug("Server find channel [${archive.id}], and set it to fast cache with client [${archive.address}]")
    latestAvailableChannels[ip] = archive
    return archive.channel
  }

  private fun channelsUnderClient(ip: String) =
    channels.let {
      val key = it.keys().find { key ->
        key.startsWith(ip)
      } ?: throw TcpServerException("从未建立与[$ip]之间的连接")
      it.get(key)
    }

  fun all(address: String): MutableSet<ChannelArchive>? =
    channels.get(address)

  internal data class ChannelArchive(
    val id: String,
    val ip: String,
    val port: Int,
    val channel: Channel,
    val connectedAt: Date,
    var obsolete: Boolean = false,
    var disconnectedAt: Date? = null
  ) {
    val address = "$ip:$port"
  }
}
