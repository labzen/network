package cn.labzen.network.tcp.client

interface TcpConnect2ServerListener {
  fun success()

  fun failed(throwable: Throwable? = null)

  fun reconnectFailed(throwable: Throwable? = null)
}

interface TcpServerLeftListener {
  fun trigger()
}

interface TcpServerLostListener {
  fun trigger()
}
