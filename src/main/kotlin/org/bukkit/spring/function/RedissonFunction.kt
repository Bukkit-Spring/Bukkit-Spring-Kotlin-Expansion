package org.bukkit.spring.function

import com.ning.spring.api.SpringApi
import org.redisson.api.RedissonClient
import java.util.concurrent.TimeUnit

/**
 * @Author 二木
 * @Description 提供Redisson支持
 * @Date 2023/11/3 17:32
 */

/**
 * 加redisson公平锁
 */
inline fun <T> rlockFair(
    lockName: String,
    lockTimeout: Long = 1000L * 15,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    func: () -> T
): T {
    val lock = getRedissonClient().getFairLock(lockName)
    var result = false
    try {
        result = lock.tryLock(1000L * 30, lockTimeout, unit)
        if(!result){
            error("获取锁 $lockName 失败 ${Thread.currentThread().name}")
        }
        return func()
    } finally {
        if(result){
            lock?.unlock()
        }
    }
}

/**
 * 加redisson锁
 */
inline fun <T> rlock(
    lockName: String,
    lockTimeout: Long = 1000L * 15,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    func: () -> T
): T {
    val lock = getRedissonClient().getLock(lockName)
    try {
        lock.lock(lockTimeout, unit)
        return func()
    } finally {
        lock?.unlock()
    }
}

/**
 * 加多把redisson的锁
 * 不加 synchronized
 */
inline fun <T> rlockMulti(
    vararg lockArray: String,
    lockTimeout: Long = 1000L * 15,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    fair: Boolean = false,
    func: () -> T
): T {
    val client = getRedissonClient()
    val rLockArray = lockArray.map { if(fair) it to client.getFairLock(it) else it to client.getLock(it) }
    if(fair){
        //记录加锁结果
        val result = mutableMapOf<String, Boolean>()
        try{
            //挨个加锁
            rLockArray.forEach{ pair ->
                val name = pair.first
                val lock = pair.second
                val lockResult = lock.tryLock(1000L * 30, lockTimeout, unit).also {
                    result[name] = it
                }
                if(!lockResult){
                    error("获取锁 $name 失败 ${Thread.currentThread().name}")
                }
            }
            return func()
        }finally {
            rLockArray.forEach {
                runCatching{
                    if(result[it.first] == true){
                        //加锁成功
                        it.second?.unlock()
                    }
                }
            }
        }
    }else{
        try {
            rLockArray.forEach {
                it.second.lock(lockTimeout, unit)
            }
            return func()
        } finally {
            rLockArray.forEach {
                runCatching {
                    it.second?.unlock()
                }
            }
        }
    }

}

/**
 * 获取redisson客户端
 */
fun getRedissonClient(): RedissonClient {
    return SpringApi.getApplicationContext().getBean(RedissonClient::class.java)
}