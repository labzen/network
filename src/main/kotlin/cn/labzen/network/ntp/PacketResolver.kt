package cn.labzen.network.ntp

import kotlin.experimental.and
import kotlin.math.pow

internal object PacketResolver {

  fun resolve(data: ByteArray): Packet {
    val data0 = data[0].toInt()
    val li = (data0.shr(6) and 0x3).toByte()
    val vn = (data0.shr(3) and 0x7).toByte()
    val mode = (data0 and 0x7).toByte()

    val stratum = unsignedByteToShort(data[1])

    val poll = data[2]
    val precision = data[3]

    val rde = data[4] * 256.0 +
        unsignedByteToShort(data[5]) +
        unsignedByteToShort(data[6]) / 256.0 +
        unsignedByteToShort(data[7]) / 65536.0

    val rdi = unsignedByteToShort(data[8]) * 256.0 +
        unsignedByteToShort(data[9]) +
        unsignedByteToShort(data[10]) / 256.0 +
        unsignedByteToShort(data[11]) / 65535.0

    val rid = byteArrayOf(data[12], data[13], data[14], data[15])

    val refTime = decodeTimestamp(data, 16)
    val oriTime = decodeTimestamp(data, 24)
    val recTime = decodeTimestamp(data, 32)
    val transTime = decodeTimestamp(data, 40)

    return Packet(li, vn, mode, stratum, poll, precision, rde, rdi, rid, refTime, oriTime, recTime, transTime)
  }

  fun structuring(packet: Packet): ByteArray {
    val data = ByteArray(48)

    data[0] = ((packet.li ?: 0).toInt().shl(6) or packet.vn.toInt().shl(3) or packet.mode.toInt()).toByte()
    packet.stratum?.let { data[1] = it.toByte() }
    packet.poll?.let { data[2] = it }
    packet.precision?.let { data[3] = it }

    packet.rde?.let {
      val l = (it * 65535.0).toInt()
      data[4] = (l.shr(24) and 0xFF).toByte()
      data[5] = (l.shr(16) and 0xFF).toByte()
      data[6] = (l.shr(8) and 0xFF).toByte()
      data[7] = (l and 0xFF).toByte()
    }

    packet.rdi?.let {
      val l = (it * 65535.0).toInt()
      data[8] = (l.shr(24) and 0xFF).toByte()
      data[9] = (l.shr(16) and 0xFF).toByte()
      data[10] = (l.shr(8) and 0xFF).toByte()
      data[11] = (l and 0xFF).toByte()
    }

    packet.rid?.let {
      data[12] = it[0]
      data[13] = it[1]
      data[14] = it[2]
      data[15] = it[3]
    }

    packet.refTime?.let { encodeTimestamp(data, 16, it) }
    packet.oriTime?.let { encodeTimestamp(data, 24, it) }
    packet.recTime?.let { encodeTimestamp(data, 32, it) }
    encodeTimestamp(data, 40, packet.transTime)

    return data
  }

  private fun unsignedByteToShort(b: Byte): Short =
    if ((b.toInt() and 0x80) == 0x80)
      (128 + (b and 0x7f)).toShort()
    else
      b.toShort()

  private fun decodeTimestamp(data: ByteArray, pointer: Int): Double {
    var r = 0.0

    for (i in 0..7) {
      r += unsignedByteToShort(data[pointer + i]) * 2.0.pow((3.0 - i) * 8)
    }

    return r
  }

  private fun encodeTimestamp(data: ByteArray, pointer: Int, timestamp: Double) {
    var tt: Double = timestamp
    // Converts a double into a 64-bit fixed point
    for (i in 0..7) {
      // 2^24, 2^16, 2^8, .. 2^-32
      val base = 2.0.pow((3 - i) * 8)

      // Capture byte value
      data[pointer + i] = (tt / base).toInt().toByte()

      // Subtract captured value from remaining total
      tt -= unsignedByteToShort(data[pointer + i]) * base
    }

    // From RFC 2030: It is advisable to fill the non-significant
    // low order bits of the timestamp with a random, unbiased
    // bit string, both to avoid systematic roundoff errors and as
    // a means of loop detection and replay detection.
    data[7] = (Math.random() * 255.0).toInt().toByte()
  }
}
