package org.bukkit.spring.annotations

import com.ning.spring.configuration.datasource.DataSourceType

/**
 * 切换数据源
 * 可用于class或具体方法上
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class SwitchDataSource(val type: DataSourceType)
