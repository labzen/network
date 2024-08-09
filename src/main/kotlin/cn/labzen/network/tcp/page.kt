@file:Suppress("unused")

package cn.labzen.network.tcp

enum class Direction {
  ASC, DESC
}

data class Order @JvmOverloads constructor(val field: String, val direction: Direction = Direction.ASC) {

  override fun toString(): String =
    if (direction == Direction.DESC) "$field desc" else field
}

/**
 * 分页
 *
 * @param current 当前页码，从0开始计数
 * @param size 页面大小
 */
@Suppress("MemberVisibilityCanBePrivate")
open class Page @JvmOverloads constructor(val current: Int, val size: Int = DEFAULT_PAGE_SIZE) {

  var range: LongRange? = null
    protected set
  var pages: Int? = null
    protected set
  var total: Long? = null
    protected set
  var autoCount: Boolean = false
    protected set
  var countField: String? = null
    protected set
  var orders: MutableList<Order>? = null
    protected set

  /**
   * 自动获取总条数
   * @param autoCountField count field name
   */
  @JvmOverloads
  fun autoCount(autoCountField: String = DEFAULT_COUNT_FIELD_NAME): Page {
    this.autoCount = true
    this.countField = autoCountField
    return this
  }

  /**
   * 标识SQL在[number]行之后进行查询，包含[number]行
   *
   * **只提供数据，具体能力由使用本BEAN的功能体实现**
   */
  fun after(number: Long): Page {
    this.range = LongRange(number, Long.MAX_VALUE)
    return this
  }

  /**
   * 标识SQL在[number]行之前进行查询，包含[number]行
   *
   * **只提供数据，具体能力由使用本BEAN的功能体实现**
   */
  fun before(number: Long): Page {
    this.range = LongRange(0, number)
    return this
  }

  /**
   * 标识SQL在[start]行与[end]行之间进行查询，包含[start]与[end]行，start > end
   *
   * **只提供数据，具体能力由使用本BEAN的功能体实现**
   */
  fun between(start: Long, end: Long): Page {
    this.range = LongRange(start, end)
    return this
  }

  fun orderBy(vararg order: Order): Page {
    orders?.addAll(order.toList()) ?: run {
      orders = order.toMutableList()
    }
    return this
  }

  fun results(total: Long) {
    this.total = total
    this.pages = total.div(size).toInt() + if (total.rem(size) == 0L) 0 else 1
  }

  override fun toString(): String =
    """
      Page {
        current: $current
        size: $size
        auto_count: $autoCount
        count_field_name: ${if (autoCount) countField!! else UNDEFINED_VALUE}
        query_range: [${range?.first ?: 0} , ${range?.last?.takeIf { it != Long.MAX_VALUE } ?: "∞"}]
        order_by: ${orders?.joinToString { it.toString() } ?: UNDEFINED_VALUE}
        total_records: ${total ?: UNKNOWN_RESULTS}
        pages: ${pages ?: UNKNOWN_RESULTS}
      }
    """.trimIndent()

  companion object {
    const val DEFAULT_PAGE_SIZE = 20
    const val DEFAULT_COUNT_FIELD_NAME = "id"
    internal const val UNDEFINED_VALUE = "[undefined it]"
    internal const val UNKNOWN_RESULTS = "[unknown results]"
  }
}
