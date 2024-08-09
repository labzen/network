package cn.labzen.network.tcp

import cn.labzen.network.exception.TcpClientException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * todo 还未考虑高并发问题，三个集合应该加锁
 */
object TcpCommandExchanger {

  private var exchangeTimeout: Long = 2000

  private val commands = mutableMapOf<String, Command>()
  private val countdowns = mutableMapOf<String, CountDownLatch>()
  private val replies = mutableMapOf<String, Command>()

  /**
   * 登记需要exchange的命令
   */
  fun enroll(identifier: String, command: Command) {
    commands[identifier] = command
    countdowns[identifier] = CountDownLatch(1)
  }

  /**
   * 开始阻塞当前线程
   */
  fun waiting(identifier: String): Command? {
    val countDownLatch = countdowns[identifier]
    countDownLatch ?: throw TcpClientException("应该不会发生吧，牛逼")

    countDownLatch.await(exchangeTimeout, TimeUnit.MILLISECONDS)

    val reply = replies.remove(identifier)
    commands.remove(identifier)
    countdowns.remove(identifier)
    return reply
  }

  fun reply(command: Command) {
    val originalIdentifier = command.original!!.identifier
    val originalCommand = commands[originalIdentifier]
    val countDownLatch = countdowns[originalIdentifier]

    // 获取不到，代表 1. 超时，已被清除； 2. 异常的回馈
    originalCommand ?: return
    countDownLatch ?: return

    replies[originalIdentifier] = command
    countDownLatch.countDown()
  }
}
