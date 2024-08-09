@file:Suppress("unused")

package cn.labzen.network.tcp

/**
 * 命令元信息
 *
 * @param event 命令的唯一类型标识，用于区分命令（事件，任务，消息等）的分类
 * @param identifier 用于客户端排重（有时命令信息在客户端无应答，会间隔一段时间后再次发出）， 或用于客户端应答服务器时，回传以便于服务器端处理业务使用
 */
data class CommandMeta(val event: String, val identifier: String) {

  /**
   * 本命令信息是否必须客户端回馈
   */
  var feedbackRequired = false
    private set

  /**
   * 限制客户端必须在规定时间内给服务器端回馈，时间的计算以在服务器端为基准，
   * 单位：毫秒，当[CommandMeta.feedbackRequired]=false时无效
   */
  var feedbackLimit = DEFAULT_FEEDBACK_LIMIT
    private set

  /**
   * 命令发出的时间
   */
  var sendAt: Long? = null

  /**
   * 命令重复的次数，存在命令发送失败、无回馈等异常情况下，重复发送相同的命令，并每次将repeat+1
   */
  var repeat: Int = 0

  /**
   * 命令携带的数据是否为空
   */
  var empty: Boolean = true

  /**
   * 描述数据的资源信息，例如：命令携带的数据为查询出的User信息
   */
  var resource: String? = null

  /**
   * 数据的分页信息
   */
  var page: Page? = null

  /**
   * 在发送exchange命令时，应该有上层的超时机制。这里设置的回馈时间限制，应该低于exchange超时
   *
   * @param limit 命令回馈时间限制，默认300毫秒
   */
  fun feedback(limit: Long = DEFAULT_FEEDBACK_LIMIT) {
    feedbackRequired = true
    feedbackLimit = limit
  }

  companion object {
    internal const val DEFAULT_FEEDBACK_LIMIT = 300L
  }
}

/**
 * 当使用exchange命令，为响应对方端的命令，用于记录对方端的命令标识（条件：收到的命令[CommandMeta.feedbackRequired] is true）
 *
 * @param identifier 指向对方端发来的命令[CommandMeta.identifier]属性
 * @param receivedAt 接收到对方端命令的时间
 */
data class CommandOriginal @JvmOverloads constructor(val identifier: String, val receivedAt: Long? = null) {

  /**
   * 相应对方端命令时产生的错误（异常）信息
   */
  var error: String? = null
}

/**
 * 命令，主要用于TCP等底层协议的信息传递
 *
 * @param data 命令传输的数据主体
 */
data class Command @JvmOverloads constructor(val data: Any? = null) {

  /**
   * 响应命令原数据
   */
  var original: CommandOriginal? = null

  /**
   * 命令元信息
   */
  var meta: CommandMeta? = null
}
