package dev.kyleescobar.runetools.asm.util

import dev.kyleescobar.runetools.asm.ClassNode
import dev.kyleescobar.runetools.asm.ClassPool
import dev.kyleescobar.runetools.asm.visitor.ClassNodeVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

object JarUtil {

    fun load(file: File, filter: (ClassNode) -> Boolean = { true }): ClassPool {
        val pool = ClassPool()
        JarFile(file).use { jar ->
            jar.entries().asSequence().forEach { entry ->
                if(!entry.name.endsWith(".class")) return@forEach
                val reader = ClassReader(jar.getInputStream(entry))
                val visitor = ClassNodeVisitor()
                reader.accept(visitor, ClassReader.SKIP_FRAMES)
                if(filter(visitor.node)) {
                    pool.addClass(visitor.node, isIgnored = false)
                } else {
                    pool.addClass(visitor.node, isIgnored = true)
                }
            }
        }
        return pool
    }

    fun save(file: File, pool: ClassPool) {
        if(file.exists()) {
            file.deleteRecursively()
        }

        JarOutputStream(FileOutputStream(file)).use { jos ->
            pool.ignoredClasses.forEach { cls ->
                jos.putNextEntry(JarEntry("${cls.name}.class"))
                jos.write(writeClass(cls))
                jos.closeEntry()
            }
            pool.classes.forEach { cls ->
                jos.putNextEntry(JarEntry("${cls.name}.class"))
                jos.write(writeClass(cls))
                jos.closeEntry()
            }
        }
    }

    private fun writeClass(cls: ClassNode): ByteArray {
        val writer = NonLoadingClassWriter(cls.pool, ClassWriter.COMPUTE_MAXS)
        val checker = CheckClassAdapter(writer, false)
        cls.accept(writer)
        val bytes = writer.toByteArray()
        //validate(cls.name, bytes)
        return bytes
    }

    private fun validate(name: String, bytes: ByteArray) = try {
        val reader = ClassReader(bytes)
        val writer = ClassWriter(reader, 0)
        val checker = CheckClassAdapter(writer, true)
        reader.accept(checker, 0)
    } catch (e: Exception) {
        throw IllegalStateException("Class Validation Error.\n${e.stackTraceToString()}")
    }
}