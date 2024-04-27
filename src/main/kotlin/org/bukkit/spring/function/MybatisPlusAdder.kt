package org.bukkit.spring.function

import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.github.pagehelper.PageHelper
import com.github.pagehelper.PageInfo
import kotlin.reflect.KProperty1

fun <T : Any> IService<T>.fastSaveOrUpdate(entity: T, lambda: UpdateWrapper<T>.() -> Unit = {}): Boolean {
    return saveOrUpdate(entity, UpdateWrapper<T>().also(lambda))
}

fun <T : Any> IService<T>.fastPutOrUpdate(entity: T, queryWrapper: QueryWrapper<T>.() -> Unit, lambda: UpdateWrapper<T>.() -> Unit = {}): Boolean {
    return if (count(QueryWrapper<T>().also(queryWrapper)) > 0) {
        update(UpdateWrapper<T>().also(lambda))
    } else {
        save(entity)
    }
}

fun <T : Any> IService<T>.fastRemove(lambda: UpdateWrapper<T>.() -> Unit = {}): Boolean {
    return remove(UpdateWrapper<T>().also(lambda))
}

fun <T : Any> IService<T>.fastUpdate(lambda: UpdateWrapper<T>.() -> Unit = {}): Boolean {
    return update(UpdateWrapper<T>().also(lambda))
}

// select
fun <T : Any> IService<T>.fastSelectOne(lambda: QueryWrapper<T>.() -> Unit = {}): T? {
    return getOne(QueryWrapper<T>().also(lambda))
}

fun <T : Any> IService<T>.fastSelectList(lambda: QueryWrapper<T>.() -> Unit = {}): List<T> {
    return list(QueryWrapper<T>().also(lambda))
}

fun <T : Any> IService<T>.fastSelectCount(lambda: QueryWrapper<T>.() -> Unit = {}): Int {
    return count(QueryWrapper<T>().also(lambda))
}

fun <T : Any> IService<T>.fastSelectMaps(lambda: QueryWrapper<T>.() -> Unit = {}): List<Map<String, Any>> {
    return listMaps(QueryWrapper<T>().also(lambda))
}

fun <T : Any> IService<T>.fastSelectObjs(lambda: QueryWrapper<T>.() -> Unit = {}): List<Any> {
    return listObjs(QueryWrapper<T>().also(lambda))
}

fun <T : Any> IService<T>.fastSelectPage(pageNumber: Int, pageSize: Int = 10, lambda: QueryWrapper<T>.() -> Unit = {}): List<T> {
    PageHelper.startPage<T>(pageNumber, pageSize)
    val apply = QueryWrapper<T>().also(lambda).lambda()
    PageInfo(list(apply)).let {
        return it.list
    }
}

fun String.camelCaseToSnakeCase(): String {
    val result = StringBuilder()
    for (i in indices) {
        val char = get(i)
        if (char.isUpperCase() && i > 0) {
            result.append('_')
        }
        result.append(char.toLowerCase())
    }
    return result.toString()
}

fun String.toSnakeCase(): String {
    return replace(Regex("([a-z])([A-Z]+)"), "$1_${"$2".toLowerCase()}")
}

fun <T, V> KProperty1<T, V>.getKey(): String {
    return this.javaClass.getKey()
}

fun <T> Class<T>.getKey(): String {
    var key = this.name
    val firstOrNull = this.annotations.firstOrNull { it is TableField }
    if (firstOrNull != null) {
        key = (firstOrNull as TableField).value
    }
    return key.camelCaseToSnakeCase().toSnakeCase()
}
