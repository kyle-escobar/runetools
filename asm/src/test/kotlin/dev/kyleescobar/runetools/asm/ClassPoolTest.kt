package dev.kyleescobar.runetools.asm

import dev.kyleescobar.runetools.asm.util.JarUtil
import java.io.File

object ClassPoolTest {

    @JvmStatic
    fun main(args: Array<String>) {
        val jarFile = File("gamepack.jar")
        val outJarFile = File("gamepack-out.jar")
        val pool = JarUtil.load(jarFile)
        JarUtil.save(outJarFile, pool)
        println()
    }
}