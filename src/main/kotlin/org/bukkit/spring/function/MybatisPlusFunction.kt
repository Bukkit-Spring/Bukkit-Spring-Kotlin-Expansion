package org.bukkit.spring.function

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.github.pagehelper.Page
import com.github.pagehelper.PageHelper
import org.bukkit.spring.pojo.entity.AsPageInfo
import taboolib.common.platform.function.warning
import kotlin.concurrent.getOrSet
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KCallable

/**
 * @Author 二木
 * @Description 封装条件操作
 * @Date 2024/3/17 22:43
 */

val order: OrderPlaceholder = OrderPlaceholder()

class OrderPlaceholder{}

val desc: QueryWrapperBuilder.OrderType = QueryWrapperBuilder.OrderType.DESC

val asc: QueryWrapperBuilder.OrderType = QueryWrapperBuilder.OrderType.ASC

infix fun OrderPlaceholder.by(property: KCallable<*>): Pair<String, QueryWrapperBuilder.OrderType>{
    //默认倒序
    return by(property.name.cameToUnderlineCase())
}

infix fun OrderPlaceholder.by(column: String): Pair<String, QueryWrapperBuilder.OrderType>{
    //默认倒序
    return (column to QueryWrapperBuilder.OrderType.DESC).also {
        (currentWrapperBuilder() as? QueryWrapperBuilder)?.orderColumnList?.add(it)
    }
}

infix fun Pair<String, QueryWrapperBuilder.OrderType>.with(orderType: QueryWrapperBuilder.OrderType){
    (currentWrapperBuilder() as? QueryWrapperBuilder)?.orderColumnList?.also {
        //都是并联by用的 by已经塞进去一个了
        it.removeLast()
        it.add(this.first to orderType)
    }
}

/**
 * 构建快捷查询器
 */
inline fun <reified T> BaseMapper<T>.query(args: T.() -> Unit = {}): QueryWrapperBuilder<T> {
    return QueryWrapperBuilder(this, QueryWrapper()).also { it.init0<T>(args) }
}

/**
 * 构建快捷修改器 (包括删除)
 */
inline fun <reified T> BaseMapper<T>.update(args: T.() -> Unit = {}): UpdateWrapperBuilder<T> {
    return UpdateWrapperBuilder(this, UpdateWrapper()).also { it.init0<T>(args) }
}

infix fun KCallable<*>.eq(other: Any?) {
    val wrapper = currentWrapperBuilder().wrapper
    val fieldName = this.name.cameToUnderlineCase()
    wrapper.eq(fieldName, other)
}


infix fun KCallable<*>.ne(other: Any?) {
    val wrapper = currentWrapperBuilder().wrapper
    val fieldName = this.name.cameToUnderlineCase()
    wrapper.ne(fieldName, other)
}

infix fun String.eq(other: Any?) {
    currentWrapperBuilder().wrapper.eq(this, other)
}

infix fun KCallable<*>.`in`(other: Any?) {
    val wrapper = currentWrapperBuilder().wrapper
    val fieldName = this.name.cameToUnderlineCase()
    wrapper.`in`(fieldName, other)
}

infix fun String.`in`(other: Any?) {
    currentWrapperBuilder().wrapper.`in`(this, other)
}

open class WrapperBuilder<T>(val mapper: BaseMapper<T>, val wrapper: AbstractWrapper<T, String, *>) {
    companion object {
        val threadLocal = ThreadLocal<WrapperBuilder<*>>()

        val pojoClassThreadLocal = ThreadLocal<Class<*>>()
    }

    inline fun <reified T> init0(args: T.() -> Unit = {}) {
        //缓存wrapper到线程上下文
        try {
            threadLocal.getOrSet { this }
            pojoClassThreadLocal.getOrSet { T::class.java }
            args(T::class.java.newInstance())
        } finally {
            threadLocal.remove()
            pojoClassThreadLocal.remove()
        }
    }

    fun limit(count: Long) {
        wrapper.last("LIMIT $count")
    }
}

open class QueryWrapperBuilder<T>(mapper: BaseMapper<T>, wrapper: AbstractWrapper<T, String, *>) :
    WrapperBuilder<T>(mapper, wrapper) {

    enum class OrderType(val stats: Boolean) {
        ASC(true),
        DESC(false)
    }

    val orderColumnList: MutableList<Pair<String, OrderType>> = mutableListOf()

    fun page(page: Int, pageSize: Int = 9): QueryWrapperBuilderWithPage<T> {
        PageHelper.startPage<T>(page, pageSize)
        return QueryWrapperBuilderWithPage(mapper, wrapper)
    }

    fun asc(column: String, vararg columns: String): QueryWrapperBuilder<T> {
        wrapper.orderByAsc(column, *columns)
        return this
    }

    fun desc(column: String, vararg columns: String): QueryWrapperBuilder<T> {
        wrapper.orderByAsc(column, *columns)
        return this
    }

    fun one(): T? {
        return mapper.selectOne(wrapper)
    }

    open fun list(): List<T> {
        orderColumnList.forEach {
            wrapper.orderBy(true, it.second.stats, it.first)
        }
        return mapper.selectList(wrapper)
    }


    class QueryWrapperBuilderWithPage<T>(mapper: BaseMapper<T>, wrapper: AbstractWrapper<T, String, *>): QueryWrapperBuilder<T>(mapper, wrapper){
        override fun list(): AsPageInfo<T> {
            return super.list().toPage()
        }
    }
}


class UpdateWrapperBuilder<T>(mapper: BaseMapper<T>, wrapper: AbstractWrapper<T, String, *>) :
    WrapperBuilder<T>(mapper, wrapper) {
    fun update(entity: T): Int {
        return mapper.update(entity, wrapper)
    }

    fun delete(): Int {
        return mapper.delete(wrapper)
    }
}


private fun String.cameToUnderlineCase(): String {
    val str = this.trim()
    if (str.isEmpty()) return ""
    val list = mutableListOf<String>()
    var i = 1
    var j = 0
    while (i < str.length) {
        if (str[i] in 'A'..'Z') {
            list.add(str.substring(j, i))
            j = i
        }
        i++
    }
    list.add(str.substring(j))
    return list.joinToString("_") { it.lowercase() }
}


private fun currentWrapperBuilder(): WrapperBuilder<*> {
    return WrapperBuilder.threadLocal.get() ?: error("当前上下文错误")
}

private fun currentPojo(): Class<*> {
    return WrapperBuilder.pojoClassThreadLocal.get() ?: error("当前上下文错误")
}


/**
 * PageHelper转为page对象
 * PageInfo如果说没页数 他的page是-1
 */
fun <T> List<T>.toPage(): AsPageInfo<T>{
    if(this is Page<T>){
        //PageHelper分页
        return AsPageInfo(this.pageNum.coerceAtLeast(1), this.pages, this.total.toInt(), this.pageSize, this)
    }
    //降级为list分页
    warning("$this 非Page对象 已降级为普通List分页")
    return this.page(page = 1)
}

/**
 * 对一个list进行逻辑分页
 * @param page 获取的页数
 * @param pageSize 每页大小
 */
fun <T> List<T>.page(page: Int, pageSize: Int = 9): AsPageInfo<T> {
    if (this.isEmpty()) {
        return AsPageInfo(1, 0, 0, pageSize, listOf())
    }
    val totalCount = this.size
    //总页数
    val pages = ceil(totalCount * 1.0 / pageSize).toInt()
    val currentPage = if (page < 1) {
        1
    } else if (page > pages) {
        pages
    } else {
        page
    }
    val startIndex = max((currentPage - 1) * pageSize, 0)
    val endIndex = min(totalCount - 1, currentPage * pageSize - 1)
    val data = this.subList(startIndex, endIndex + 1)
    return AsPageInfo(currentPage, pages, totalCount, pageSize, data)
}
