package org.bukkit.spring.pojo.entity

/**
 * @Author 二木
 * @Description 分页信息对象
 * @Date 2023/6/29 15:37
 */
class AsPageInfo<T>(val page: Int, val pages: Int, val total: Int, val pageSize: Int, val data: List<T>) : List<T> by data {
    fun hasPreviousPage(): Boolean{
        if(total == 0){
            return false
        }
        return page > 1
    }

    fun hasNextPage(): Boolean{
        if(total == 0){
            return false
        }
        return page < pages
    }


    override fun toString(): String {
        return "AsPageInfo(page=$page, pages=$pages, total=$total, pageSize=$pageSize, data=$data)"
    }
}