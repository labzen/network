package cn.labzen.network.exception

import cn.labzen.meta.exception.LabzenRuntimeException


/**
 * TCP服务器异常
 */
class TcpServerException : LabzenRuntimeException {

  constructor(message: String) : super(message)
  constructor(message: String, vararg arguments: Any?) : super(message, *arguments)
  constructor(cause: Throwable) : super(cause)
  constructor(cause: Throwable, message: String) : super(cause, message)
  constructor(cause: Throwable, message: String, vararg arguments: Any?) : super(cause, message, *arguments)
}
