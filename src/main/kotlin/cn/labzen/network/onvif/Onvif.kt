package cn.labzen.network.onvif

import org.slf4j.LoggerFactory
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

@Suppress("unused")
object Onvif {

  private val logger = LoggerFactory.getLogger(Onvif::class.java)

  @JvmStatic
  @JvmOverloads
  fun create(timeout: Int = 5000) =
    Discoverer(timeout)

  /**
   * 获取此设备上的接口地址列表
   */
  fun interfaceAddresses() =
    try {
      NetworkInterface.getNetworkInterfaces().toList().flatMap { ni ->
        if (ni.isLoopback || !ni.isUp) {
          emptyList()
        } else {
          ni.interfaceAddresses.map { a -> a.address }.toList()
        }
      }.filterNotNull()
    } catch (e: SocketException) {
      logger.warn("on Onvif.interfaceAddresses()", e)
      emptyList()
    }

  /**
   * 获取此设备上所有广播地址的列表
   */
  fun broadcastAddresses() =
    try {
      NetworkInterface.getNetworkInterfaces().toList().flatMap { ni ->
        if (ni.isLoopback || !ni.isUp) {
          emptyList()
        } else {
          ni.interfaceAddresses.map { a -> a.broadcast }.toList()
        }
      }.filterNotNull()
    } catch (e: SocketException) {
      logger.warn("on Onvif.broadcastAddresses()", e)
      emptyList()
    }

  /**
   * 获取此设备本地IP
   */
  fun localIp() =
    try {
      NetworkInterface.getNetworkInterfaces().toList().firstNotNullOfOrNull { ni ->
        ni.inetAddresses.toList().findLast { inet -> inet.isLoopbackAddress && inet is Inet4Address }?.hostAddress
      }
    } catch (e: SocketException) {
      logger.warn("on Onvif.broadcastAddresses()", e)
      null
    }
}
