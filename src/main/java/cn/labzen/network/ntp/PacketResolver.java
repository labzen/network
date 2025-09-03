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

    double rde = data[4] * 256.0 +
                 unsignedByteToShort(data[5]) +
                 unsignedByteToShort(data[6]) / 256.0 +
                 unsignedByteToShort(data[7]) / 65536.0;

    double rdi = unsignedByteToShort(data[8]) * 256.0 +
                 unsignedByteToShort(data[9]) +
                 unsignedByteToShort(data[10]) / 256.0 +
                 unsignedByteToShort(data[11]) / 65535.0;

    byte[] rid = {data[12], data[13], data[14], data[15]};

    double refTime = decodeTimestamp(data, 16);
    double oriTime = decodeTimestamp(data, 24);
    double recTime = decodeTimestamp(data, 32);
    double transTime = decodeTimestamp(data, 40);

    return new Packet(li, vn, mode, stratum, poll, precision, rde, rdi, rid, refTime, oriTime, recTime, transTime);
  }

  public static byte[] structuring(Packet packet) {
    byte[] data = new byte[48];

    data[0] = (byte) (((packet.li() != null ? packet.li() : 0) << 6) | (packet.vn() << 3) | packet.mode());
    if (packet.stratum() != null) {
      data[1] = (byte) (short) packet.stratum();
    }
    if (packet.poll() != null) {
      data[2] = packet.poll();
    }
    if (packet.precision() != null) {
      data[3] = packet.precision();
    }

    if (packet.rde() != null) {
      int l = (int) (packet.rde() * 65535.0);
      data[4] = (byte) ((l >> 24) & 0xFF);
      data[5] = (byte) ((l >> 16) & 0xFF);
      data[6] = (byte) ((l >> 8) & 0xFF);
      data[7] = (byte) (l & 0xFF);
    }

    if (packet.rdi() != null) {
      int l = (int) (packet.rdi() * 65535.0);
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
    return (b & 0x80) == 0x80 ? (short) (128 + (b & 0x7f)) : (short) b;
  }

  private static double decodeTimestamp(byte[] data, int pointer) {
    double r = 0.0;

    for (int i = 0; i <= 7; i++) {
      r += unsignedByteToShort(data[pointer + i]) * Math.pow(2.0, (3.0 - i) * 8);
    }

    return r;
  }

  private static void encodeTimestamp(byte[] data, int pointer, double timestamp) {
    double tt = timestamp;
    // Converts a double into a 64-bit fixed point
    for (int i = 0; i <= 7; i++) {
      // 2^24, 2^16, 2^8, .. 2^-32
      double base = Math.pow(2.0, (3 - i) * 8);

      // Capture byte value
      data[pointer + i] = (byte) ((int) (tt / base));

      // Subtract captured value from remaining total
      tt -= unsignedByteToShort(data[pointer + i]) * base;
    }

    // From RFC 2030: It is advisable to fill the non-significant
    // low order bits of the timestamp with a random, unbiased
    // bit string, both to avoid systematic roundoff errors and as
    // a means of loop detection and replay detection.
    data[pointer + 7] = (byte) (RandomGenerator.getDefault().nextDouble() * 255.0);
  }
}
