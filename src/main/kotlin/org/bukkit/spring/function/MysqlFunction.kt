package org.bukkit.spring.function

import com.ning.spring.configuration.datasource.DataSourceType
import com.ning.spring.configuration.datasource.DynamicDataSourceContext
import org.apache.ibatis.session.SqlSessionFactory
import org.bukkit.spring.mapper.CoreMapper
import org.bukkit.spring.proxy.SpringProxy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import taboolib.common.platform.function.info
import javax.annotation.Resource

/**
 * @Author 二木
 * @Description
 * @Date 2023/11/3 23:57
 */

@Service
class MysqlService{
    @Resource
    lateinit var coreMapper: CoreMapper

    @Resource
    lateinit var sqlSessionFactory: SqlSessionFactory

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun generatorTable(table: String, createSql: String): Boolean{
        //ddl语句会隐式commit 所以得新开一个事务
        rlock("mysql"){
            if(checkTypeTable(table)){
                return false
            }
            //创建表
            sqlSessionFactory.openSession().use {
                val connection = it.connection
                connection.prepareStatement(createSql).execute()
            }
            info("创建数据表 $table 成功")
            return true
        }
    }
}

private val coreMapper: CoreMapper by SpringProxy

private val mysqlService: MysqlService by SpringProxy

/**
 * 检查某一类型表是否存在
 */
fun checkTypeTable(table: String): Boolean {
    rlock("mysql") {
        return coreMapper.checkTableExists(table) != null
    }
}

/**
 * 获取当前线程数据源
 */
fun currentDataSource(): DataSourceType{
    return DynamicDataSourceContext.get()?.let {
        DataSourceType.findByDataSourceName(it)
    } ?: DataSourceType.DEFAULT_DATASOURCE
}

/**
 * 生成表 如果表已经存在则不会创建
 * @return 如果表已存在返回 false 否则创建成功 true
 */
fun generatorTable(table: String, createSql: String) = mysqlService.generatorTable(table, createSql)
