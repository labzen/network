package cn.labzen.network.tcp.server

interface TcpServerBootListener {
  fun success()

  fun failed(throwable: Throwable? = null)
}

interface TcpClientComingListener {
  fun trigger(ip: String, port: Int)
}

interface TcpClientLeftListener {
  fun trigger(ip: String, port: Int)
}

interface TcpClientLostListener {
  fun trigger(ip: String, port: Int)
}
