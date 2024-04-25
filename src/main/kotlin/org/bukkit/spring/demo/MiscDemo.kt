package org.bukkit.spring.demo

import org.bukkit.spring.function.rlock
import org.bukkit.spring.function.rlockMulti
import org.bukkit.spring.mapper.CoreMapper
import org.bukkit.spring.message.registerServerMessageListener
import org.bukkit.spring.message.sendServerMessage
import org.bukkit.spring.proxy.SpringProxy
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * @Author 二木
 * @Description 请确保Mybatis-Plus和Spring扫包路径已正确配置
 * @Date 2024/4/25 19:54
 */
object Demo{
    /**
     * Taboolib很多东西只能在Object中使用 在Object中可使用此种方式完成注入
     */
    private val coreMapper: CoreMapper by SpringProxy

    /**
     * 查询当前数据库版本
     */
    fun queryDatabaseVersion(){
        println(coreMapper.selectVersion())
        println(coreMapper.selectDataBase())
    }

    /**
     * Redisson使用
     */
    fun redissonDemo(){
        rlock("lock_name"){

        }
        rlockMulti("lock1", "lock2"){

        }
    }

    /**
     * 搭配 RabbitMQ 完成消息推送
     */
    fun messagePushDemo(){
        sendServerMessage(channelName = "test", "Hello, world!")
    }

    @Awake(LifeCycle.ENABLE)
    fun onEnable(){
        //开启服务器注册channel监听通道
        registerServerMessageListener("test"){
            println(it)
        }
    }
}