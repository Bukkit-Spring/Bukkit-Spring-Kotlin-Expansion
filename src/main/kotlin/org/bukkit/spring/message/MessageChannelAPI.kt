package org.bukkit.spring.message

import com.google.gson.Gson
import org.bukkit.spring.message.rabbit.RabbitApi
import taboolib.common.platform.event.SubscribeEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * @Author 二木
 * @Description
 * @Date 2024/3/8 15:10
 */
class MessagePayload(val channelName: String, val content: String) {
    /**
     * 发送服务器
     */
    var sendServer: String = "default"

    /**
     * 消息发送时间
     */

    val sendTime: Long = System.currentTimeMillis()

    override fun toString(): String {
        return "MessagePayload(channelName='$channelName', content=$content, sendServer='$sendServer', sendTime=$sendTime)"
    }
}

private val LISTENER_DATA: MutableMap<String, MutableList<MessagePayload.(String) -> Unit>> = mutableMapOf()

/**
 * 发送服务器通讯消息
 */
fun sendServerMessage(
    channelName: String,
    content: String
) {
    sendServerMessageOriginal(MessagePayload(channelName, content = content))
}

/**
 * 发送消息
 */
fun sendServerMessageOriginal(payload: MessagePayload) {
    RabbitApi.publish(Gson().toJson(payload))
}

/**
 * 注册消息监听器
 */
fun registerServerMessageListener(channelName: String, func: MessagePayload.(String) -> Unit) {
    LISTENER_DATA.getOrPut(channelName) { CopyOnWriteArrayList() }.add(func)
}

object MessageChannelListener{
    @SubscribeEvent
    fun onReceive(e: RabbitApi.RabbitReceiveEvent){
        onReceiveMessage(payload = Gson().fromJson(e.message, MessagePayload::class.java))
    }
}

/**
 * 接收到消息
 */
private fun onReceiveMessage(payload: MessagePayload) {
    LISTENER_DATA[payload.channelName]?.forEach { it(payload, payload.content) }
}
