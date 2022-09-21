package dev.kyleescobar.runetools.asm.util

import dev.kyleescobar.runetools.asm.ClassNode
import dev.kyleescobar.runetools.asm.ClassPool
import org.objectweb.asm.ClassWriter

class NonLoadingClassWriter(private val pool: ClassPool, flags: Int) : ClassWriter(flags) {

    override fun getCommonSuperClass(type1: String, type2: String): String {
        if(type1 == "java/lang/Object" || type2 == "java/lang/Object") {
            return "java/lang/Object"
        }

        val class1 = pool.findClass(type1)
        val class2 = pool.findClass(type2)

        if(class1 == null && class2 == null) {
            return try {
                super.getCommonSuperClass(type1, type2)
            } catch (e: Exception) {
                "java/lang/Object"
            }
        }

        if(class1 != null && class2 != null) {
            if(!(class1.isInterface() || class2.isInterface())) {
                var parent1 = pool.findClass(class1.superName)
                while(parent1.also { parent1 = pool.findClass(it?.superName ?: "") } != null) {
                    var parent2 = pool.findClass(class2.superName)
                    while(parent2.also { parent2 = pool.findClass(it?.superName ?: "") } != null) {
                        if(parent1 == parent2) {
                            return parent1!!.name
                        }
                    }
                }
            }
            return "java/lang/Object"
        }


        val found: ClassNode?
        val other: String?

        if(class1 == null) {
            found = class2
            other = type1
        } else {
            found = class1
            other = type2
        }

        var prev: ClassNode? = null
        var c: ClassNode? = pool.findClass(found?.superName ?: "")
        while(c != null) {
            prev = c
            c = pool.findClass(c.superName)
            if(prev.superName == other) return other
        }
        return super.getCommonSuperClass(prev!!.superName, other)
    }
}