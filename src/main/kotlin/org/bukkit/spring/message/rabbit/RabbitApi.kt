package org.bukkit.spring.message.rabbit

import com.google.common.io.ByteStreams
import com.rabbitmq.client.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.type.BukkitProxyEvent
import java.util.*

/**
 * @Author 二木
 * @Description
 * @Date 2024/3/8 15:40
 */
object RabbitApi {
    @Config("rabbit.yml")
    private lateinit var rabbitConfig: Configuration

    private lateinit var connectionFactory: ConnectionFactory

    private var connection: Connection? = null

    private var channel: Channel? = null

    private var id: String? = null

    private var sub: Subscription? = null

    private val exchange: String by lazy {
        rabbitConfig.getString("exchange")!!
    }

    private val routingKey by lazy {
        rabbitConfig.getString("routing_key")!!
    }


    @Awake(LifeCycle.ENABLE)
    private fun onEnable(){
        info("开始初始化 RabbitMQ...")
        connectionFactory = ConnectionFactory().also {
            it.host = rabbitConfig.getString("address")
            it.port = rabbitConfig.getInt("port")
            it.username = rabbitConfig.getString("username")
            it.password = rabbitConfig.getString("password")
            it.virtualHost = rabbitConfig.getString("virtual_url")
        }
        Thread(CheckConnectionTask()).start()
        info("RabbitMQ 初始化成功")
    }

    fun publish(message: String){
        kotlin.runCatching {
            ByteStreams.newDataOutput().also { it.writeUTF(message) }.toByteArray().let {
                channel?.basicPublish(exchange, routingKey, AMQP.BasicProperties.Builder().build(), it)
            }
        }
    }

    private class CheckConnectionTask: Runnable{
        override fun run() {
            var firstStartUp = true
            while (!Thread.interrupted()) {
                try {
                    if (!checkAndReopenConnection(firstStartUp)) {
                        // Sleep for 5 seconds to prevent massive spam in console
                        Thread.sleep(5000L)
                        continue
                    }
                    // Check connection life every 30 seconds
                    Thread.sleep(30000L)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                } finally {
                    firstStartUp = false
                }
            }
        }
    }

    private fun checkAndReopenConnection(firstStartup: Boolean): Boolean{
        val connectionAlive = connection != null && connection!!.isOpen
        val channelAlive = channel != null && channel!!.isOpen

        if (connectionAlive && channelAlive) {
            return true
        }
        try{
            channel?.close()
            connection?.close()
        }catch (_: Exception){
        }
        if(!firstStartup){
            warning("RabbitMQ 掉线, 尝试重新连接")
        }
        try{
            connection = connectionFactory.newConnection()
            channel = connection!!.createChannel().also {
                val queue = it.queueDeclare("", false, true, true, null).queue
                it.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, false, true, null)
                it.queueBind(queue, exchange, routingKey)

                id = uuid()
                sub = Subscription(id!!, connection!!, it)
                it.basicConsume(queue, true, sub, CancelCallback {  })
            }

            if(!firstStartup){
                info("RabbitMQ 已重新建立连接")
            }

            return true
        }catch (e: Exception){
            return false
        }
    }


    private class Subscription(
        private val id: String,
        private val connection: Connection,
        private val channel: Channel
    ) : DeliverCallback {
        override fun handle(consumerTag: String, message: Delivery) {
            if (!id.equals(RabbitApi.id, ignoreCase = true)) {
                try {
                    this.channel.close()
                    this.connection.close()
                } catch (ignore: Exception) {
                }
                return
            }
            try {
                val data = message.body
                val input = ByteStreams.newDataInput(data)
                val msg = input.readUTF() ?: return
                RabbitReceiveEvent(msg).call()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    class RabbitReceiveEvent(val message: String): BukkitProxyEvent(){
        override val allowCancelled: Boolean
            get() = false
    }
}

/**
 * 生成32位uuid
 */
fun uuid(): String {
    return UUID.randomUUID().toString().replace("-", "")
}