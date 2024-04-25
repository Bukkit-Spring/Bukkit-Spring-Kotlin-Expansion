## Bukkit-Spring-Kotlin-Expansion

在 Spring-Api 基础上, 结合 Taboolib 实现了一些比较好用的糖, 此项目无法直接打包插件直接使用, 需根据实际情况将代码引入自己项目。

### 特性
- 与 Taboolib 兼容, 在 Object 中注入 IOC 对象
- Redisson 顶级函数
- Mysql 顶级函数
- 更符合 Kotlin 体质的 MybatisPlus 且快速分页
- MybatisPlus 多数据源、动态切换
- 基于 RabbitMQ 的生产订阅消息推送

### 快速查询 并完成分页
![img.png](img.png)

---
#### 注意
由于 Kotlin 的 class 默认为 final, 无法被 AOP 正常代理, 所以请在您的项目中引入 gradle 插件 `org.jetbrains.kotlin.plugin.spring` 可参考本项目构建文件, 此插件会自动将使用`@Service`等注解的类变为 `open` 可继承