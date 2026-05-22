package cn.labzen.network.ntp;

/**
 * NTP协议：时钟同步报文
 *
 * @param li        Leap Indicator - 闰秒标识器。长度=2bit。'00'=无预告，'01'=一天的最后一分钟有61秒，'10'=一天的最后一分钟有59秒，'11'=警告状态（始终未同步）。非'11'值时NTP本身不做处理
 * @param vn        Version Number - 协议版本。长度=3bit。目前最新版本=4（RFC 5905）。NTP协议是向下兼容版本3的
 * @param mode      Mode - NTP的工作模式。长度=3bit。0=预留，1=主动对等体模式，2=被动对等体模式，3=客户模式，4=服务器模式，5=广播模式或组播模式，6=此报文为NTP控制报文，7预留给内部使用
 * @param stratum   Stratum - 系统时钟的层数。长度=8bit。取值范围为1-16，它定义了时钟的准确度。层数为1的时钟准确度最高，准确度从1到16依次递减。0=未指定或无效，1=主要参考服务器，2-15辅助参考服务器，16=未同步状态（不能作为参考时钟），17-255=预留
 * @param poll      Poll - 轮询时间（秒）。长度=8bit。两个连续NTP报文之间的最大时间间隔，以2的最近幂为单位。最小和最大轮询间隔的建议缺省限制分别为6（64秒）和10（1024秒）
 * @param precision Precision - 本地时钟的精度（秒）。长度=8bit。以2的最近幂为单位。通常出现的值范围从-6（主频时钟）到-20（在某些工作站中发现的微秒时钟）
 * @param rde       Root Delay - 根延迟时间（秒）。长度=32bit。此值指示到主引用源的总往返延迟(以秒为单位)。根据相对的时间和频率偏移量，这个变量可以取正值，也可以取负值。通常出现在该字段中的值范围从几毫秒的负值到数百毫秒的正值
 * @param rdi       Root Dispersion - 根误差（秒）。长度=32bit。此值指示相对于主参考源的标称误差。通常值范围从0到几百毫秒
 * @param rid       Reference ID - 参考标识符。长度=32bit。用于标识特定的引用源，意义取决于[stratum]的取值。
 *                  <ul>
 *                    <li>stratum=0：值为4字节ASCII字符串，成为'kiss'码，用于调试和监控的目的
 *                    <li>stratum=1：值为分配给参考时钟的一个四字节、左对齐、零填充的ASCII字符串。权威的引用标识符列表由IANA维护；但是，任何以ASCII字符“X”开头的字符串都保留给未注册的实验和开发
 *                      <ul>
 *                        <li>GOES: Geosynchronous Orbit Environment Satellite
 *                        <li>GPS: Global Position System
 *                        <li>GAL: Galileo Positioning System
 *                        <li>PPS: Generic pulse-per-second
 *                        <li>IRIG: Inter-Range Instrumentation Group
 *                        <li>WWVB: LF Radio WWVB Ft. Collins, CO 60 kHz
 *                        <li>DCF: LF Radio DCF77 Mainflingen, DE 77.5 kHz
 *                        <li>HBG: LF Radio HBG Prangins, HB 75 kHz
 *                        <li>MSF: LF Radio MSF Anthorn, UK 60 kHz
 *                        <li>JJY: LF Radio JJY Fukushima, JP 40 kHz, Saga, JP 60 kHz
 *                        <li>LORC: MF Radio LORAN C station, 100 kHz
 *                        <li>TDF: MF Radio Allouis, FR 162 kHz
 *                        <li>CHU: HF Radio CHU Ottawa, Ontario
 *                        <li>WWV: HF Radio WWV Ft. Collins, CO
 *                        <li>WWVH: HF Radio WWVH Kauai, HI
 *                        <li>NIST: NIST telephone modem
 *                        <li>ACTS: NIST telephone modem
 *                        <li>USNO: USNO telephone modem
 *                        <li>PTB: European telephone modem
 *                      </ul>
 *                    <li>stratum=2-15：值为服务器的引用标识符，可用于检测定时循环
 *                      <ul>
 *                        <li>如果使用IPv4地址，标识符是4个8位的ip地址
 *                        <li>如果使用IPv6地址，标识符是IPv6地址的MD5散列的前4个8位
 *                      </ul>
 *                  </ul>
 *                  <b>注意：当在NTPv4服务器和NTPv3客户机上使用IPv6地址家族时，引用标识符字段似乎是一个随机值，可能无法检测到计时循环。</b>
 * @param refTime   Reference Timestamp - 参考时间戳。长度=64bit。系统时钟上次设置或更正的时间，NTP时间戳格式
 * @param oriTime   Origin Timestamp - 原始时间戳。长度=64bit。在客户端请求包发出时的时间，NTP时间戳格式
 * @param recTime   Receive Timestamp - 接受时间戳。长度=64bit。请求从客户端到达服务器时的时间，NTP时间戳格式
 * @param transTime Transmit Timestamp - 传送时间戳。长度=64bit。应答报文离开服务器时的时间，NTP时间戳格式
 * @param keyId     Key Identifier - 关键识别符。长度=32bit。服务器和客户端，用来指定一个128位的密匙的MD5值
 * @param digest    Message Digest - 消息摘要。长度=128bit。对消息头（不包含Key Identifier和Message Digest）的MD5值摘要
 */
public record Packet(Byte li, Byte vn, Byte mode, Short stratum, Byte poll, Byte precision, Double rde, Double rdi,
                     byte[] rid, Double refTime, Double oriTime, Double recTime, Double transTime, String keyId,
                     String digest) {

  static final double NTP_EPOCH_OFFSET = 2208988800.0;

  Packet(Byte li,
         Byte vn,
         Byte mode,
         Short stratum,
         Byte poll,
         Byte precision,
         Double rde,
         Double rdi,
         byte[] rid,
         Double refTime,
         Double oriTime,
         Double recTime,
         Double transTime) {
    this(li, vn, mode, stratum, poll, precision, rde, rdi, rid, refTime, oriTime, recTime, transTime, null, null);
  }

  Packet(byte vn, byte mode) {
    this(null,
        vn,
        mode,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        System.currentTimeMillis() / 1000.0 + NTP_EPOCH_OFFSET,
        null,
        null);
  }

  Packet() {
    this(null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        System.currentTimeMillis() / 1000.0 + NTP_EPOCH_OFFSET,
        null,
        null);
  }
}
