package cn.labzen.network.ntp;

import java.util.random.RandomGenerator;

class PacketResolver {

  private PacketResolver() {
  }

  public static Packet resolve(byte[] data) {
    int data0 = data[0];
    byte li = (byte) ((data0 >> 6) & 0x3);
    byte vn = (byte) ((data0 >> 3) & 0x7);
    byte mode = (byte) (data0 & 0x7);

    short stratum = unsignedByteToShort(data[1]);

    byte poll = data[2];
    byte precision = data[3];

    double rde = unsignedByteToShort(data[4]) * 256.0 +
                 unsignedByteToShort(data[5]) +
                 unsignedByteToShort(data[6]) / 256.0 +
                 unsignedByteToShort(data[7]) / 65536.0;

    double rdi = unsignedByteToShort(data[8]) * 256.0 +
                 unsignedByteToShort(data[9]) +
                 unsignedByteToShort(data[10]) / 256.0 +
                 unsignedByteToShort(data[11]) / 65536.0;

    byte[] rid = {data[12], data[13], data[14], data[15]};

    double refTime = decodeTimestamp(data, 16);
    double oriTime = decodeTimestamp(data, 24);
    double recTime = decodeTimestamp(data, 32);
    double transTime = decodeTimestamp(data, 40);

    return new Packet(li, vn, mode, stratum, poll, precision, rde, rdi, rid, refTime, oriTime, recTime, transTime);
  }

  public static byte[] structuring(Packet packet) {
    byte[] data = new byte[48];

    byte vn = packet.vn() != null ? packet.vn() : 4;  // 默认NTP版本4
    byte mode = packet.mode() != null ? packet.mode() : 3;  // 默认客户模式
    data[0] = (byte) (((packet.li() != null ? packet.li() : 0) << 6) | (vn << 3) | mode);
    if (packet.stratum() != null) {
      data[1] = (byte) (packet.stratum() & 0xFF);
    }
    if (packet.poll() != null) {
      data[2] = packet.poll();
    }
    if (packet.precision() != null) {
      data[3] = packet.precision();
    }

    if (packet.rde() != null) {
      int l = (int) (packet.rde() * 65536.0);
      data[4] = (byte) ((l >> 24) & 0xFF);
      data[5] = (byte) ((l >> 16) & 0xFF);
      data[6] = (byte) ((l >> 8) & 0xFF);
      data[7] = (byte) (l & 0xFF);
    }

    if (packet.rdi() != null) {
      int l = (int) (packet.rdi() * 65536.0);
      data[8] = (byte) ((l >> 24) & 0xFF);
      data[9] = (byte) ((l >> 16) & 0xFF);
      data[10] = (byte) ((l >> 8) & 0xFF);
      data[11] = (byte) (l & 0xFF);
    }

    if (packet.rid() != null) {
      byte[] rid = packet.rid();
      data[12] = rid[0];
      data[13] = rid[1];
      data[14] = rid[2];
      data[15] = rid[3];
    }

    if (packet.refTime() != null) {
      encodeTimestamp(data, 16, packet.refTime());
    }
    if (packet.oriTime() != null) {
      encodeTimestamp(data, 24, packet.oriTime());
    }
    if (packet.recTime() != null) {
      encodeTimestamp(data, 32, packet.recTime());
    }
    encodeTimestamp(data, 40, packet.transTime());

    return data;
  }

  private static short unsignedByteToShort(byte b) {
    return (short) (b & 0xFF);
  }

  private static double decodeTimestamp(byte[] data, int pointer) {
    // NTP时间戳是网络字节序（大端序）
    // 前32位是整数部分（秒），后32位是小数部分
    long seconds = ((long) (data[pointer] & 0xFF) << 24) |
                  ((long) (data[pointer + 1] & 0xFF) << 16) |
                  ((long) (data[pointer + 2] & 0xFF) << 8) |
                  (data[pointer + 3] & 0xFF);

    long fraction = ((long) (data[pointer + 4] & 0xFF) << 24) |
                   ((long) (data[pointer + 5] & 0xFF) << 16) |
                   ((long) (data[pointer + 6] & 0xFF) << 8) |
                   (data[pointer + 7] & 0xFF);

    double timestamp = seconds + (fraction / 4294967296.0);

    // NTP时间戳从1900年1月1日开始，Java时间从1970年1月1日开始
    // 需要减去2208988800秒的偏移量转换为Unix时间戳
    return timestamp - Packet.TIMEZONE_8;
  }

  private static void encodeTimestamp(byte[] data, int pointer, double timestamp) {
    // 将Unix时间戳转换为NTP时间戳
    // NTP时间戳 = Unix时间戳 + 2208988800秒(NTP纪元偏移)
    double ntpTimestamp = timestamp + Packet.TIMEZONE_8;

    long seconds = (long) ntpTimestamp;
    long fraction = (long) ((ntpTimestamp - seconds) * 4294967296.0);

    data[pointer] = (byte) ((seconds >> 24) & 0xFF);
    data[pointer + 1] = (byte) ((seconds >> 16) & 0xFF);
    data[pointer + 2] = (byte) ((seconds >> 8) & 0xFF);
    data[pointer + 3] = (byte) (seconds & 0xFF);

    data[pointer + 4] = (byte) ((fraction >> 24) & 0xFF);
    data[pointer + 5] = (byte) ((fraction >> 16) & 0xFF);
    data[pointer + 6] = (byte) ((fraction >> 8) & 0xFF);
    // 根据RFC 2030，建议用随机数填充低有效位
    data[pointer + 7] = (byte) (RandomGenerator.getDefault().nextDouble() * 255.0);
  }
}
