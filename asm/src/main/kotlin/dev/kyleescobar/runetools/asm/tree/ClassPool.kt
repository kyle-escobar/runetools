package dev.kyleescobar.runetools.asm.tree

import dev.kyleescobar.runetools.asm.file.MemoryClassFileLoader
import dev.kyleescobar.runetools.asm.context.CachingBloatContext
import dev.kyleescobar.runetools.asm.file.ClassFile
import dev.kyleescobar.runetools.asm.reflect.ClassInfo
import dev.kyleescobar.runetools.asm.visitor.NodeVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class ClassPool {

    private var loader = MemoryClassFileLoader()
    private var context = CachingBloatContext(loader, mutableListOf<ClassInfo>(), true)

    private val classMap = hashMapOf<String, ClassNode>()
    val classes get() = classMap.values.toList()

    fun addClass(info: ClassInfo) {
        classMap[info.name()] = ClassNode(this, info, context.editClass(info))
    }

    fun addClass(name: String, bytes: ByteArray) {
        val info = ClassFile(File("$name.class"), loader, DataInputStream(bytes.inputStream()))
        addClass(info)
    }

    fun removeClass(name: String) {
        classMap.remove(name)
    }

    fun addJar(file: File) {
        JarFile(file).use { jar ->
            jar.entries().asSequence().filter { it.name.endsWith(".class") }.forEach { entry ->
                val node = org.objectweb.asm.tree.ClassNode()
                val reader = ClassReader(jar.getInputStream(entry))
                reader.accept(node, ClassReader.EXPAND_FRAMES)
                val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
                node.accept(writer)
                val name = node.name
                val bytes = writer.toByteArray()
                addClass(name, bytes)
            }
        }
    }

    fun saveJar(file: File) {
        commit()
        if(file.exists()) file.deleteRecursively()
        val jos = JarOutputStream(FileOutputStream(file))
        classes.forEach { cls ->
            val reader = ClassReader(loader.getClassBytes(cls.info.name() + ".class"))
            val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
            reader.accept(writer, ClassReader.EXPAND_FRAMES)
            val bytes = writer.toByteArray()
            jos.putNextEntry(JarEntry(cls.info.name() + ".class"))
            jos.write(bytes)
            jos.closeEntry()
        }
        jos.close()
    }

    fun getClass(name: String) = classMap[name]

    fun clear() {
        classMap.clear()
        loader = MemoryClassFileLoader()
        context = CachingBloatContext(loader, mutableListOf<ClassInfo>(), true)
    }

    fun commit() {
        loader.reset()
        classes.forEach { cls ->
            cls.methods.forEach { method ->
                method.commit()
            }
            cls.fields.forEach { field ->
                field.commit()
            }
            cls.editor.commit()
            cls.info.commit()
        }
    }

    fun accept(visitor: NodeVisitor) {
        visitor.visitPool(this)
        visitor.visitStart()
        classes.forEach { cls ->
            cls.accept(visitor)
        }
        visitor.visitEnd()
    }
}