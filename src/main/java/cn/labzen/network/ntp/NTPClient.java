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

      long t1 = System.currentTimeMillis();  // 客户端发送请求的时间

      DatagramPacket incoming = new DatagramPacket(data, data.length);
      datagramSocket.receive(incoming);

      long t4 = System.currentTimeMillis();  // 客户端接收响应的时间

      Packet resolved = PacketResolver.resolve(incoming.getData());

      // T2 = 服务器接收时间, T3 = 服务器发送时间
      // NTP服务器返回的时间戳已经是Unix时间格式（经过decodeTimestamp转换）
      double t2 = resolved.recTime();
      double t3 = resolved.transTime();

      // 根据RFC 2030: θ = ((T2 - T1) + (T3 - T4)) / 2
      // 将T1和T4转换为秒（与服务器时间格式一致）
      double t1Seconds = t1 / 1000.0;
      double t4Seconds = t4 / 1000.0;

      // 时间偏移量 = ((T2 - T1) + (T3 - T4)) / 2
      // 为负数时，表示本机时间快于NTP服务器时间
      return ((t2 - t1Seconds) + (t3 - t4Seconds)) / 2;
    } catch (IOException e) {
      throw new NTPException(e);
    }
  }
}
