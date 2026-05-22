package cn.labzen.network.util;

import cn.labzen.tool.util.Strings;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.regex.Pattern;

public final class Addresses {

  private Addresses() {
  }

  private static final String X_FORWARDED_FOR = "X-Forwarded-For";
  private static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
  private static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
  private static final String HTTP_CLIENT_IP = "HTTP_CLIENT_IP";
  private static final String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";
  private static final String UNKNOWN = "unknown";

  // 修复 IPv4 正则表达式
  private static final Pattern IPV4_PATTERN = Pattern.compile(
      "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

  // 简化的 IPv6 正则表达式
  private static final Pattern IPV6_PATTERN = Pattern.compile(
      "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::1$|^::$|^([0-9a-fA-F]{1,4}:){1,7}:$|^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|^([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}$|^([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}$|^([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}$|^([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}$|^[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})$|^:((:[0-9a-fA-F]{1,4}){1,7}|:)$");

  /**
   * 获取模块下地址
   */
  public static String moduleUri(HttpServletRequest request) {
    return request.getContextPath();
  }

  /**
   * 获取相对地址
   */
  public static String relativeUri(HttpServletRequest request) {
    return request.getServletPath();
  }

  /**
   * 获取绝对地址
   */
  public static String absoluteUri(HttpServletRequest request) {
    return request.getRequestURL().toString();
  }

  /**
   * 判断IPv4
   */
  public static boolean isIpv4(String ip) {
    return ip != null && IPV4_PATTERN.matcher(ip.trim()).matches();
  }

  /**
   * 判断IPv6
   */
  public static boolean isIpv6(String ip) {
    return ip != null && IPV6_PATTERN.matcher(ip.trim()).matches();
  }

  /**
   * 判断是否为IP地址（IPv4或IPv6）
   */
  public static boolean isIp(String ip) {
    return Strings.isNotBlank(ip) && (isIpv4(ip) || isIpv6(ip));
  }

  /**
   * 获取客户端IP（优化版本）
   */
  public static String remoteIp(HttpServletRequest request) {
    // 1. 优先检查 X-Forwarded-For（可能包含多个IP，需先拆分再逐项校验）
    String forwardedFor = checkHeader(request, X_FORWARDED_FOR);
    if (Strings.isNotBlank(forwardedFor)) {
      String ip = getFirstValidIpFromList(forwardedFor);
      if (ip != null) {
        return ip;
      }
    }

    // 2. 检查单值代理头部
    for (String header : new String[]{PROXY_CLIENT_IP, WL_PROXY_CLIENT_IP, HTTP_CLIENT_IP}) {
      String ip = checkHeader(request, header);
      if (isValidIp(ip)) {
        return ip;
      }
    }

    // 3. HTTP_X_FORWARDED_FOR（也可能包含多个IP）
    String httpForwardedFor = checkHeader(request, HTTP_X_FORWARDED_FOR);
    if (Strings.isNotBlank(httpForwardedFor)) {
      String ip = getFirstValidIpFromList(httpForwardedFor);
      if (ip != null) {
        return ip;
      }
    }

    // 4. 最后使用远程地址
    return Optional.ofNullable(request.getRemoteAddr()).filter(Addresses::isIp).orElse("127.0.0.1");
  }

  /**
   * 从X-Forwarded-For头部获取第一个有效IP
   */
  private static String getFirstValidIpFromList(String ipList) {
    if (ipList == null || ipList.isBlank()) {
      return null;
    }

    String[] ips = ipList.split("\\s*,\\s*");
    for (String ip : ips) {
      String trimmedIp = ip.trim();
      if (isValidIp(trimmedIp)) {
        return trimmedIp;
      }
    }
    return null;
  }

  /**
   * 检查是否为内网IP
   */
  private static boolean isInternalIp(String ip) {
    if (!isIpv4(ip)) {
      return false;
    }

    String[] parts = ip.split("\\.");
    int first = Integer.parseInt(parts[0]);
    int second = Integer.parseInt(parts[1]);

    // 10.0.0.0 - 10.255.255.255
    if (first == 10) {
      return true;
    }

    // 172.16.0.0 - 172.31.255.255
    if (first == 172 && second >= 16 && second <= 31) {
      return true;
    }

    // 192.168.0.0 - 192.168.255.255
    if (first == 192 && second == 168) {
      return true;
    }

    // 127.0.0.0 - 127.255.255.255
    return first == 127;
  }

  /**
   * 检查头部值是否为有效IP
   */
  private static String checkHeader(HttpServletRequest request, String headerName) {
    return Optional.ofNullable(request.getHeader(headerName))
                   .map(String::trim)
                   .filter(header -> !header.isBlank())
                   .filter(header -> !UNKNOWN.equalsIgnoreCase(header))
                   .orElse(null);
  }

  /**
   * 验证IP是否有效且不是未知或内网IP
   */
  private static boolean isValidIp(String ip) {
    return ip != null && !ip.isBlank() && !UNKNOWN.equalsIgnoreCase(ip) && isIp(ip) && !isInternalIp(ip);
  }

  /**
   * 增强版IP获取方法，支持更多头部和更好的过滤
   */
  public static String getClientIp(HttpServletRequest request) {
    // 1. X-Real-IP（单值头，常见于 Nginx）
    String ip = checkHeader(request, "X-Real-IP");
    if (isValidIp(ip)) {
      return ip;
    }

    // 2. X-Forwarded-For（可能包含多个IP，需先拆分再逐项校验）
    String forwardedFor = checkHeader(request, X_FORWARDED_FOR);
    if (Strings.isNotBlank(forwardedFor)) {
      String firstValidIp = getFirstValidIpFromList(forwardedFor);
      if (firstValidIp != null) {
        return firstValidIp;
      }
    }

    // 3. 检查其他单值代理头部
    for (String header : new String[]{"Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP"}) {
      ip = checkHeader(request, header);
      if (isValidIp(ip)) {
        return ip;
      }
    }

    // 4. HTTP_X_FORWARDED_FOR（也可能包含多个IP）
    String httpForwardedFor = checkHeader(request, HTTP_X_FORWARDED_FOR);
    if (Strings.isNotBlank(httpForwardedFor)) {
      String firstValidIp = getFirstValidIpFromList(httpForwardedFor);
      if (firstValidIp != null) {
        return firstValidIp;
      }
    }

    // 5. X-Cluster-Client-IP
    ip = checkHeader(request, "X-Cluster-Client-IP");
    if (isValidIp(ip)) {
      return ip;
    }

    // 6. 最后返回远程地址
    return Optional.ofNullable(request.getRemoteAddr()).filter(addr -> !addr.isBlank()).orElse("127.0.0.1");
  }
}
