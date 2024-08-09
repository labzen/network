package cn.labzen.network.tcp.client

import cn.labzen.tool.kotlin.throwRuntimeUnless
import cn.labzen.network.exception.TcpClientException
import cn.labzen.network.tcp.TcpHandler

class TcpClientConfig(val host: String, val port: Int) {

  internal lateinit var handlerClass: Class<out TcpHandler>
  internal var connectTimeout: Int = 2000
  internal var reconnectTimes: Int = 5
  internal var reconnectInterval: Long = 300
  internal var reconnectIntervalLevel2Enabled: Boolean = false
  internal var reconnectIntervalLevel2: Long = 10000
  internal var pulseInterval: Long = 3
  internal var pulseRetryLimit: Int = 3
  internal var connect2ServerListener: TcpConnect2ServerListener? = null
  internal var serverLeftListener: TcpServerLeftListener? = null
  internal var serverLostListener: TcpServerLostListener? = null

  fun check() {
    this::handlerClass.isInitialized.throwRuntimeUnless { TcpClientException("未指定Client Handler类") }
  }
}
