package org.bukkit.spring.function

import com.ning.spring.api.SpringApi

/**
 * @Author 二木
 * @Description
 * @Date 2023/11/5 12:09
 */

/**
 * 在 aop 代理的方法中, this调用当前对象方法会导致AOP使用, 故需使用aopThis
 */
val <reified T> T.aopThis: T
    inline get(){
        return SpringApi.getApplicationContext().getBean(T::class.java)
    }