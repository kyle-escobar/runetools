package com.github.kyleescobar.runetools.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class ClassPool {

    private val classMap = hashMapOf<String, ClassNode>()
    private val ignoredClasses = hashSetOf<String>()

    val classes get() = classMap.values.filter { it.name !in ignoredClasses }.toList()

    fun addClass(node: ClassNode) {
        classMap[node.name] = node
    }

    fun removeClass(name: String) {
        classMap.remove(name)
    }

    fun getClass(name: String): ClassNode? = classMap[name]

    fun ignoreClass(name: String) {
        ignoredClasses.add(name)
    }

    fun unignoreClass(name: String) {
        ignoredClasses.remove(name)
    }

    fun loadJar(file: File, filter: (String) -> Boolean = { true }) {
        if(!file.exists()) throw FileNotFoundException()
        JarFile(file).use { jar ->
            jar.entries().asSequence()
                .filter { it.name.endsWith(".class") }
                .forEach { entry ->
                    val node = ClassNode()
                    val reader = ClassReader(jar.getInputStream(entry))
                    reader.accept(node, ClassReader.SKIP_DEBUG)
                    if(!filter(node.name)) return@forEach
                    addClass(node)
                }
        }
    }

    fun saveJar(file: File) {
        if(file.exists()) {
            file.deleteRecursively()
        }
        val jos = JarOutputStream(FileOutputStream(file))
        classMap.values.forEach { cls ->
            jos.putNextEntry(JarEntry(cls.name + ".class"))
            val writer = ClassWriter(0)
            cls.accept(writer)
            jos.write(writer.toByteArray())
            jos.closeEntry()
        }
        jos.close()
    }

    fun clear() {
        classMap.keys.removeIf { it !in ignoredClasses }
    }

}