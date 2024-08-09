@file:Suppress("unused", "KotlinConstantConditions", "MemberVisibilityCanBePrivate")

package cn.labzen.network.tcp.client

import cn.labzen.tool.kotlin.throwRuntimeIf
import cn.labzen.network.exception.TcpClientException
import cn.labzen.network.tcp.SimpleTcpHandler
import cn.labzen.network.tcp.TcpHandler

@Suppress("UNCHECKED_CAST")
class TcpClientBuilder internal constructor(private val config: TcpClientConfig) {

  fun handlerClass(clazz: Class<out TcpHandler>): TcpClientBuilder {
    (clazz is TcpHandler).throwRuntimeIf { TcpClientException("需实现TcpClientHandler的子接口") }
    config.handlerClass = clazz
    return this
  }

  /**
   * 建立连接超时时间，单位：毫秒，默认2000
   */
  fun connectTimeout(connectTimeout: Int): TcpClientBuilder {
    config.connectTimeout = connectTimeout
    return this
  }

  /**
   * 当断网时的自动重连次数，默认5次
   */
  fun reconnectTimes(reconnectTimes: Int): TcpClientBuilder {
    config.reconnectTimes = reconnectTimes
    return this
  }

  /**
   * 当断网时的自动重连，间隔时间，单位：毫秒，默认5000
   */
  fun reconnectInterval(reconnectInterval: Long): TcpClientBuilder {
    config.reconnectInterval = reconnectInterval
    return this
  }

  /**
   * 启用断网自动重连2级缓冲，在[reconnectTimes]次重连失败后，会开启[reconnectIntervalLevel2]间隔时间后，再次尝试[reconnectTimes]次重连
   *
   * **IMPORTANT:** 开启重连2级缓冲，是没有次数限制的
   */
  fun enableReconnectLevel2(): TcpClientBuilder {
    config.reconnectIntervalLevel2Enabled = true
    return this
  }

  /**
   * 重连2级缓冲间隔时间，两个[reconnectTimes]连续重连尝试之间的间隔时间，建议大于[reconnectTimes] x [reconnectInterval] x 2
   */
  fun reconnectIntervalLevel2(reconnectIntervalLevel2: Long): TcpClientBuilder {
    config.reconnectIntervalLevel2 = reconnectIntervalLevel2
    return this
  }

  /**
   * 设置TCP客户端心跳发送间隔时间，单位：秒
   *
   * 假设在[interval]时间内，客户端没有读到任何数据，则会触发userEventTriggered，
   * 此时无法判定服务器是已断开连接还是单纯没有命令发过来，所以这时主动发送心跳，看能否收到回复。
   * 收不到，则记录心跳发送失败次数，如收到则将失败次数归0。
   *
   * 建议客户端的 pulseInterval X pulseRetryLimit = 服务器端的 pulseTimeout
   *
   * ###########
   *
   * 例：
   * 1. 客户端Client的间隔心跳设置为5
   * 2. 当Client 5秒没有接收到来自Server的数据，则主动发一次心跳（需在userEventTriggered中主动向Server发心跳）
   * 3. Client每次发心跳，维护心跳发送次数
   * 4. 服务端Server的读超时设置为20（即Client向Server发4次心跳均无法收到的情况）
   * 5. Server收到心跳Ping，回复Pong。Client收到Pong，复位心跳发送次数（Client在收到其他数据时，也复位）
   * 6. Client心跳发送次数达到4次，则认为与服务器的连接已断掉，可主动关掉Channel
   * 7. Client的channelInactive中，可延时建立与Server的重连
   * 8. Server的userEventTriggered中读超时触发，则主动断开连接
   */
  fun pulseTimeout(interval: Long): TcpClientBuilder {
    config.pulseInterval = interval
    return this
  }

  /**
   * 设置客户端心跳发送的次数，成功即归0重计
   */
  fun pulseRetryTimes(times: Int): TcpClientBuilder {
    config.pulseRetryLimit = times
    return this
  }

  /**
   * 监听客户端连接服务器事件
   */
  fun listenConnect2Server(listener: TcpConnect2ServerListener): TcpClientBuilder {
    config.connect2ServerListener = listener
    return this
  }

  /**
   * 监听服务器端释放连接事件
   */
  fun listenServerLeft(listener: TcpServerLeftListener): TcpClientBuilder {
    config.serverLeftListener = listener
    return this
  }

  fun listenServerLost(listener: TcpServerLostListener): TcpClientBuilder {
    config.serverLostListener = listener
    return this
  }

  fun build() =
    config.let {
      it.check()
      if (it.handlerClass is SimpleTcpHandler) {
        SimpleTcpClient(it)
      } else {
        BothWayTcpClient(it)
      }
    }
}
