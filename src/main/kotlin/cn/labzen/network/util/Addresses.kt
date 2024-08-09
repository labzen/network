@file:Suppress("unused")

package cn.labzen.network.util

import javax.servlet.http.HttpServletRequest

object Addresses {

  private const val X_FORWARDED_FOR = "X-Forwarded-For"
  private const val PROXY_CLIENT_IP = "Proxy-Client-IP"
  private const val WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP"
  private const val HTTP_CLIENT_IP = "HTTP_CLIENT_IP"
  private const val HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR"

  private val IPV4_PATTERN =
    Regex("^((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))$")
  private val IPV6_PATTERN = Regex(
    "^\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:)(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?\\s*$"
  )

  // ==============================================================

  /**
   * 获取模块下地址
   */
  @JvmStatic
  fun moduleUri(request: HttpServletRequest): String =
    request.contextPath

  /**
   * 获取相对地址
   */
  @JvmStatic
  fun relativeUri(request: HttpServletRequest): String =
    request.servletPath

  /**
   * 获取绝对地址
   */
  @JvmStatic
  fun absoluteUri(request: HttpServletRequest): String =
    request.requestURL.toString()

  /**
   * 判断IPv4
   */
  @JvmStatic
  fun isIpv4(ip: String) = ip.matches(IPV4_PATTERN)

  /**
   * 判断IPv6
   */
  @JvmStatic
  fun isIpv6(ip: String) = ip.matches(IPV6_PATTERN)

  @JvmStatic
  fun isIp(ip: String?) = ip?.let {
    isIpv4(it) || isIpv6(it)
  } ?: false

  /**
   * 获取客户端IP
   */
  @JvmStatic
  fun remoteIp(request: HttpServletRequest): String? {
    request.getHeader(X_FORWARDED_FOR)?.apply {
      val first = split(",").singleOrNull()
      if (isIp(first)) {
        return first
      }
    }

    return remoteIpFromHeader(request, PROXY_CLIENT_IP)
      ?: remoteIpFromHeader(request, WL_PROXY_CLIENT_IP)
      ?: remoteIpFromHeader(request, HTTP_CLIENT_IP)
      ?: remoteIpFromHeader(request, HTTP_X_FORWARDED_FOR)
      ?: request.remoteAddr
  }

  private fun remoteIpFromHeader(request: HttpServletRequest, key: String): String? =
    request.getHeader(key)?.let {
      if (isIp(it)) it else null
    }
}
