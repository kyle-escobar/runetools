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
    }
}