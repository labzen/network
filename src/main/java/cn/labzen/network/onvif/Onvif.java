package cn.labzen.network.onvif;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public final class Onvif {

  private static final Logger LOGGER = LoggerFactory.getLogger(Onvif.class);

  private Onvif() {
  }

  public static Discoverer create(int timeout) {
    return new Discoverer(timeout);
  }

  /**
   * 获取当前设备上的接口地址列表
   */
  public static List<InetAddress> interfaceAddresses() {
    try {
      return addresses(InterfaceAddress::getAddress);
    } catch (SocketException e) {
      LOGGER.warn("on Onvif.interfaceAddresses()", e);
      return null;
    }
  }

  /**
   * 获取当前设备上所有广播地址的列表
   */
  public static List<InetAddress> broadcastAddresses() {
    try {
      return addresses(InterfaceAddress::getBroadcast);
    } catch (SocketException e) {
      LOGGER.warn("on Onvif.broadcastAddresses()", e);
      return null;
    }
  }

  private static List<InetAddress> addresses(Function<InterfaceAddress, InetAddress> function) throws SocketException {
    Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
    ArrayList<NetworkInterface> interfaces = Collections.list(interfaceEnumeration);
    return interfaces.stream().flatMap(ni -> {
      try {
        if (ni.isLoopback() || !ni.isUp()) {
          return Stream.empty();
        }
        return ni.getInterfaceAddresses().stream().map(function);
      } catch (SocketException e) {
        return Stream.empty();
      }
    }).filter(Objects::nonNull).toList();
  }
}
