plugins {
    `java-library`
    `maven-publish`
    id("io.izzel.taboolib") version "1.60"
    id("org.jetbrains.kotlin.plugin.spring") version "1.5.31"
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
}

taboolib {
    install("common")
    install("common-5")
    install("module-nms")
    install("module-nms-util")
    install("module-effect")
    install("module-navigation")
    install("module-configuration")
    install("platform-bukkit")
    install("expansion-command-helper")
    classifier = null
    version = "6.0.10-96"

    description{
        name("Bukkit-Spring-Kotlin-Expansion")
        contributors {
            name("二木")
        }
        dependencies {
            //你的插件只需要依赖Spring-Api
            name("Spring-Api")
        }
    }
}


dependencies {
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("ink.ptms.core:v11902:11902-minimize:mapped")
    compileOnly("ink.ptms.core:v11902:11902-minimize:universal")
    implementation("com.google.guava:guava:20.0")
    implementation("com.rabbitmq:amqp-client:5.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

tasks.withType<Jar>{
    destinationDir = file("F:/我的世界/服务器/麦格瑞/麦格瑞文档")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

