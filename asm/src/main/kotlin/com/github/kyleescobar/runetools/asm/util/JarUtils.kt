package com.github.kyleescobar.runetools.asm.util

import com.github.kyleescobar.runetools.asm.isSourceClass
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

object JarUtils {

    fun readJarFile(file: File): List<ClassNode> {
        val list = mutableListOf<ClassNode>()
        JarFile(file).use { jar ->
            jar.entries().asSequence().forEach { entry ->
                if(entry.name.endsWith(".class")) {
                    val reader = ClassReader(jar.getInputStream(entry))
                    val node = ClassNode()
                    reader.accept(node, ClassReader.EXPAND_FRAMES)
                    list.add(node)
                }
            }
        }
        return list
    }

    fun writeJarFile(file: File, classes: List<ClassNode>) {
        if(file.exists()) file.deleteRecursively()
        JarOutputStream(FileOutputStream(file)).use { jos ->
            classes.filter { it.isSourceClass }.forEach { cls ->
                val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
                cls.accept(writer)
                jos.putNextEntry(JarEntry(cls.name + ".class"))
                jos.write(writer.toByteArray())
                jos.closeEntry()
            }
        }
    }
}