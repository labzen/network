@file:Suppress("unused")

package cn.labzen.network.tcp.server

import cn.labzen.network.tcp.TcpHandler

class TcpServerBuilder internal constructor(private val config: TcpServerConfig) {

  fun handlerClass(clazz: Class<out TcpHandler>): TcpServerBuilder {
    config.handlerClass = clazz
    return this
  }

  /**
   * 设置TCP服务器端心跳检测超时时间，单位：秒（假如超过这个时间，通道中没有读到任何数据，
   * 则断开连接）。建议客户端的心跳时间为该值的公因数。
   *
   * ###########
   *
   * 例：
   * 1. 客户端Client的读超时设置为5
   * 2. 当Client 5秒没有接收到来自Server的数据，则主动发一次心跳（需在userEventTriggered中主动向Server发心跳）
   * 3. Client每次发心跳，维护心跳发送次数
   * 4. 服务端Server的读超时设置为20（即Client向Server发4次心跳均无法收到的情况）
   * 5. Server收到心跳Ping，回复Pong。Client收到Pong，复位心跳发送次数（Client在收到其他数据时，也复位）
   * 6. Client心跳发送次数达到4次，则认为与服务器的连接已断掉，可主动关掉Channel
   * 7. Client的channelInactive中，可延时建立与Server的重连
   * 8. Server的userEventTriggered中读超时触发，则主动断开连接
   */
  fun pulseTimeout(timeout: Long): TcpServerBuilder {
    config.pulseTimeout = timeout
    return this
  }

  /**
   * 监听服务器启动事件
   */
  fun listenServerBoot(listener: TcpServerBootListener): TcpServerBuilder {
    config.bootListener = listener
    return this
  }

  /**
   * 监听客户端建立连接事件
   */
  fun listenClientComing(listener: TcpClientComingListener): TcpServerBuilder {
    config.clientComingListener = listener
    return this
  }

  /**
   * 监听客户端释放连接事件
   */
  fun listenClientLeft(listener: TcpClientLeftListener): TcpServerBuilder {
    config.clientLeftListener = listener
    return this
  }

  /**
   * 监听客户端掉线事件（会有一点儿延迟触发）
   */
  fun listenClientLost(listener: TcpClientLostListener): TcpServerBuilder {
    config.clientLostListener = listener
    return this
  }

  fun build() = DefaultTcpServer(config.apply { check() })
}
