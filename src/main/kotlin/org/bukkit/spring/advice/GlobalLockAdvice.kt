package org.bukkit.spring.advice

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.bukkit.spring.annotations.Rlock
import org.bukkit.spring.function.rlockMulti
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.annotation.Order
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component


/**
 * @Author 二木
 * @Description 全局加锁切面
 * 用于解决 锁和声明式事务 @Transactional 一块使用时的失效问题
 * @Date 2023/11/4 20:14
 */
class GlobalLockAdvice {

    @Aspect
    @Order(1)
    @Component
    class SyncAdvice{
        @Around("@annotation(aosuo.ning.core.annotations.Sync)")
        fun around(joinPoint: ProceedingJoinPoint): Any?{
            synchronized(joinPoint.target){
                return joinPoint.proceed()
            }
        }
    }

    @Aspect
    @Order(2)
    @Component
    class RlockAdvice{
        // spel表达式解析器
        private val spelExpressionParser = SpelExpressionParser()

        // 参数名发现器
        private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

        @Around("@annotation(rlock)")
        fun around(joinPoint: ProceedingJoinPoint, rlock: Rlock): Any?{
            val methodSignature = joinPoint.signature as MethodSignature
            //参数数组
            val lockArray = parameterNameDiscoverer.getParameterNames(methodSignature.method)?.let {paramsName ->
                val context = StandardEvaluationContext()
                joinPoint.args.forEachIndexed { index, value ->
                    context.setVariable(paramsName[index], value)
                }
                rlock.value.map { kotlin.runCatching { spelExpressionParser.parseExpression(it).getValue(context)?.toString() ?: it }.getOrDefault(it)  }.toTypedArray()
            } ?: rlock.value
            rlockMulti(*lockArray, fair = rlock.fair){
                return joinPoint.proceed()
            }
        }
    }
}