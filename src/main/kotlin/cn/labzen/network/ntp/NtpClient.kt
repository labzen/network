@file:Suppress("unused")

package cn.labzen.network.ntp

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object NtpClient {

  /**
   * 请求NTP服务器，获取网络时间
   *
   * @param host NTP服务器地址
   * @param port NTP服务器端口
   * @param timeout 请求NTP服务器超时时间
   *
   * @return 本机与NTP服务器的时间偏移量，为负数时，代表本机时间大于NTP服务器时间，单位（秒）
   */
  @JvmStatic
  @JvmOverloads
  fun request(host: String, port: Int = 123, timeout: Int = 1500): Double {
    val address = InetAddress.getByName(host)

    val data = PacketResolver.structuring(Packet())
    val outgoing = DatagramPacket(data, data.size, address, port)

    val socket = DatagramSocket().apply {
      this.soTimeout = timeout
    }
    socket.send(outgoing)

    val incoming = DatagramPacket(data, data.size)
    socket.receive(incoming)

    // 这里要加2208988800，是因为获得到的时间是格林尼治时间，所以要变成东八区的时间，否则会与与北京时间有8小时的时差
    val destinationTimestamp = System.currentTimeMillis() / 1000.0 + Packet.TIMEZONE_8

    val packet = PacketResolver.resolve(incoming.data)

    return ((packet.recTime!! - packet.oriTime!!) + (packet.transTime - destinationTimestamp)) / 2
  }
}
