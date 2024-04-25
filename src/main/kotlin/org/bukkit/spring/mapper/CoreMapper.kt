package org.bukkit.spring.mapper

import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.springframework.stereotype.Repository

/**
 * @Author 二木
 * @Description
 * @Date 2023/11/3 18:08
 */
@Repository
interface CoreMapper {
    /**
     * 获取数据库当前版本
     */
    fun selectVersion(): String

    /**
     * 获取数据库当前使用库
     */
    @Select("select database()")
    fun selectDataBase(): String?

    @Select("show tables like '\${table}'")
    fun checkTableExists(@Param("table") table: String): String?
}