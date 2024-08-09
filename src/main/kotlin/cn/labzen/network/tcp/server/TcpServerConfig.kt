package cn.labzen.network.tcp.server

import cn.labzen.tool.kotlin.throwRuntimeUnless
import cn.labzen.network.exception.TcpServerException
import cn.labzen.network.tcp.TcpHandler

class TcpServerConfig(val port: Int) {

  internal lateinit var handlerClass: Class<out TcpHandler>
  internal var pulseTimeout: Long = 9
  internal var bootListener: TcpServerBootListener? = null
  internal var clientComingListener: TcpClientComingListener? = null
  internal var clientLeftListener: TcpClientLeftListener? = null
  internal var clientLostListener: TcpClientLostListener? = null

  fun check() {
    this::handlerClass.isInitialized.throwRuntimeUnless { TcpServerException("未指定Http Server Handler类") }
  }
}
