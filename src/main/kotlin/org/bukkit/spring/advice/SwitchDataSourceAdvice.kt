package org.bukkit.spring.advice

import com.ning.spring.configuration.datasource.DataSourceType
import com.ning.spring.configuration.datasource.DynamicDataSourceContext
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.bukkit.spring.annotations.SwitchDataSource
import org.bukkit.spring.function.currentDataSource
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport
import javax.annotation.Resource
import kotlin.concurrent.getOrSet


/**
 * @Author 二木
 * @Description 用于切换数据源
 * @Date 2023/11/4 20:14
 */
@Aspect
@Component
@Order(3)
class SwitchDataSourceAdvice {
    companion object{
        private val threadLocal = ThreadLocal<SwitchDataSourceContext>()
    }

    class SwitchDataSourceContext{
        /**
         * 当前嵌套层数
         */
        var order: Int = 0

        /**
         * 每层嵌套对应的数据源
         */
        val orderDataSource = mutableMapOf<Int, DataSourceType>()
    }

    @Resource
    lateinit var transactionUtils: TransactionUtil


    @Around("@annotation(aosuo.ning.core.annotations.SwitchDataSource) || @within(aosuo.ning.core.annotations.SwitchDataSource)")
    fun around(joinPoint: ProceedingJoinPoint): Any?{
        //获取当前数据源
        val context = threadLocal.getOrSet {
            SwitchDataSourceContext()
        }
        //获取上一层的旧数据源
        val oldDataSourceType = context.orderDataSource.getOrDefault(context.order, currentDataSource())
        context.order += 1
        //获取需要切换到的数据源
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        val currentClass = joinPoint.target.javaClass
        val switchDataSource = if (method.getAnnotation(SwitchDataSource::class.java) != null) {
            method.getAnnotation(SwitchDataSource::class.java)
        } else {
            currentClass.getAnnotation(SwitchDataSource::class.java)
        }
        val targetDataSource = switchDataSource.type
        context.orderDataSource[context.order] = targetDataSource

        if(targetDataSource == oldDataSourceType){
            //不用切换
            try{
                return joinPoint.proceed()
            }finally{
                orderReduce(context)
            }
        }
        //需要切换
        DynamicDataSourceContext.set(switchDataSource.type.dataSourceName)

        //取当前事务
        val transaction = kotlin.runCatching {
            TransactionAspectSupport.currentTransactionStatus()
        }.getOrNull()
        val transactionAnnotation = method.getAnnotation(Transactional::class.java)

        //当前有事务 但是要切换数据源 只要新执行的方法 不是 REQUIRES_NEW 均新开一个事务 不然会有缓存
        if(transaction != null && (transactionAnnotation == null || transactionAnnotation.propagation != Propagation.REQUIRES_NEW)){
            //按照本地事务 跨库的时候 直接开启新事务 不跟当前事务有关系
            try{
                return transactionUtils.openNew(joinPoint)
            }finally{
                orderReduce(context)
            }
        }

        //当前没有事务或新执行方法为 REQUIRES_NEW 直接执行即可
        try{
            return joinPoint.proceed()
        }finally{
            orderReduce(context)
        }
    }

    /**
     * 减少层数
     */
    private fun orderReduce(context: SwitchDataSourceContext){
        context.order -= 1
        if(context.order <= 0){
            //最后一层
            threadLocal.remove()
            DynamicDataSourceContext.remove()
        }else{
            val dataSource = context.orderDataSource[context.order]!!
            if(dataSource == currentDataSource()){
                //不用切
                return
            }
            DynamicDataSourceContext.set(dataSource.dataSourceName)
        }
    }
}

@Component
class TransactionUtil {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun openNew(joinPoint: ProceedingJoinPoint): Any? {
        println("新开事务")
        return joinPoint.proceed()
    }
}