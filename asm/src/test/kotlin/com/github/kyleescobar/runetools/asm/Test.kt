package com.github.kyleescobar.runetools.asm

import com.github.kyleescobar.runetools.asm.util.JarUtils
import java.io.File

object Test {

    @JvmStatic
    fun main(args: Array<String>) {
        val file = File("gamepack.jar")

        val pool = ClassPool()
        JarUtils.readJarFile(file).forEach { pool.addClass(it) }
        pool.loadInfo()

        println("Loaded ${pool.classes.size} classes.")

        val cls = pool.getClass("client")!!
        val method = cls.methods.first { it.name == "mr" }

        println("=== RELATIVES: client.class ===")
        cls.relatives.forEach { println(it.name) }
        println(" ")

        val overrides = cls.resolveMethods("init", "()V")
        overrides.forEach { println("${it.owner.name}.${it.name}${it.desc}") }
    }
}