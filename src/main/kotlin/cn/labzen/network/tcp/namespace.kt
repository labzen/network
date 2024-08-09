package cn.labzen.network.tcp

internal const val TCP_PULSE_EVENT = "labzen:tcp:pulse"
private const val TCP_PULSE_CONTENT_PING = "ping"
private const val TCP_PULSE_CONTENT_PONG = "pong"
internal const val UNDEFINED_EVENT = "<<no event>>"

internal val PULSE_PING = Command(TCP_PULSE_CONTENT_PING).apply {
  meta = CommandMeta(TCP_PULSE_EVENT, TCP_PULSE_CONTENT_PING).also {
    it.feedback(300)
  }
}.let { TCP.jsonString(it) }

internal val PULSE_PONG = Command(TCP_PULSE_CONTENT_PONG).apply {
  this.meta = CommandMeta(TCP_PULSE_EVENT, TCP_PULSE_CONTENT_PONG)
}.let { TCP.jsonString(it) }

// ===================================================================

enum class TcpCommunicationMode {
  SIMPLE, BOTH_WAY
}

interface TcpHandler {

  /**
   * 内部处理异常
   */
  fun failed(cause: Throwable)
}

interface SimpleTcpHandler : TcpHandler {

  fun handleNotify(addr: String, command: Command)
}

interface BothWayTcpHandler : SimpleTcpHandler {

  fun handleExchange(addr: String, command: Command): Command
}
