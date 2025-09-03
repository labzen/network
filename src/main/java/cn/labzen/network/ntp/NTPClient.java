package cn.labzen.network.ntp;

import cn.labzen.network.exception.NTPException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class NTPClient {

  /**
   * 请求NTP服务器，获取网络时间
   *
   * @param host NTP服务器地址，默认123端口
   * @return 本机与NTP服务器的时间偏移量，为负数时，代表本机时间大于NTP服务器时间，单位（秒）
   */
  public static Double request(String host) {
    return request(host, 123, 1500);
  }

  /**
   * 请求NTP服务器，获取网络时间
   *
   * @param host NTP服务器地址
   * @param port NTP服务器端口
   * @return 本机与NTP服务器的时间偏移量，为负数时，代表本机时间大于NTP服务器时间，单位（秒）
   */
  public static Double request(String host, int port) {
    return request(host, port, 1500);
  }

  /**
   * 请求NTP服务器，获取网络时间
   *
   * @param host    NTP服务器地址
   * @param port    NTP服务器端口
   * @param timeout 请求NTP服务器超时时间
   * @return 本机与NTP服务器的时间偏移量，为负数时，代表本机时间大于NTP服务器时间，单位（秒）
   */
  public static Double request(String host, int port, int timeout) throws NTPException {
    InetAddress address;
    try {
      address = InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      throw new NTPException(e, "无法解析NTP服务器：{}", host);
    }

    byte[] data = PacketResolver.structuring(new Packet());
    DatagramPacket outgoing = new DatagramPacket(data, data.length, address, port);

    try (DatagramSocket datagramSocket = new DatagramSocket()) {
      datagramSocket.setSoTimeout(timeout);
      datagramSocket.send(outgoing);

      DatagramPacket incoming = new DatagramPacket(data, data.length);
      datagramSocket.receive(incoming);

      // 这里要加2208988800，是因为获得到的时间是格林尼治时间，所以要变成东八区的时间，否则会与与北京时间有8小时的时差
      double destinationTimestamp = System.currentTimeMillis() / 1000.0 + Packet.TIMEZONE_8;

      Packet resolved = PacketResolver.resolve(incoming.getData());
      return ((resolved.recTime() - resolved.oriTime()) + (resolved.transTime() - destinationTimestamp)) / 2;
    } catch (IOException e) {
      throw new NTPException(e);
    }
  }
}
