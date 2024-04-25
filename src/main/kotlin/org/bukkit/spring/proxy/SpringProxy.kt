package org.bukkit.spring.proxy

import com.ning.spring.api.SpringApi
import kotlin.reflect.KProperty

/**
 * @Author 二木
 * @Description
 * @Date 2023/4/5 19:48
 */
object SpringProxy{
    /**
     * 通过代理方式 直接拿到Spring中的对象
     */
    inline operator fun <reified T> getValue(ref: Any?, property: KProperty<*>): T{
        return SpringApi.getApplicationContext().getBean(T::class.java)
    }
}