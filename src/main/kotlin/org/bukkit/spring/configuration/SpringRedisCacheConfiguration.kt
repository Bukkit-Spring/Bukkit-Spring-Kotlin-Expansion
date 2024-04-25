package org.bukkit.spring.configuration

import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import java.time.Duration

/**
 * @Author 二木
 * @Description
 * @Date 2023/12/6 17:11
 */
@Configuration
class SpringRedisCacheConfiguration {
    @Bean
    fun redisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, Any?>{
        return RedisTemplate<String, Any?>().also {
            it.connectionFactory = factory
            it.keySerializer = RedisSerializer.string()
            it.hashKeySerializer = RedisSerializer.string()
        }
    }

    @Bean
    fun cacheManager(factory: RedisConnectionFactory): CacheManager {
        val cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24))
        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(factory)
            .cacheDefaults(cacheConfiguration)
            .build()
    }
}