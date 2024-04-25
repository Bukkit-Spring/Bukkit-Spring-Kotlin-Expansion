package org.bukkit.spring.annotations

@Retention(AnnotationRetention.RUNTIME)
/**
 * @param value 锁名称
 * @param fair 是否公平锁 默认非公平锁
 */
annotation class Rlock(vararg val value: String, val fair: Boolean = false)
