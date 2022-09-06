package com.github.kyleescobar.runetools.deobfuscator.asm

import me.coley.analysis.util.InheritanceGraph
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class ClassPool {

    private val classMap = hashMapOf<String, ClassNode>()

    var inheritanceGraph = InheritanceGraph().also {
        it.addClasspath()
        it.addModulePath()
    }

    val classes get() = classMap.values.toList()

    private val topMethods = hashSetOf<String>()
    private val topFields = hashSetOf<String>()

    fun addClass(node: ClassNode) {
        if(!classMap.containsKey(node.name)) {
            classMap[node.name] = node
            node.init(this)
            val parents = mutableListOf<String>()
            parents.add(node.superName)
            parents.addAll(node.interfaces)
            inheritanceGraph.add(node.name, parents)
        }
    }

    fun removeClass(name: String) {
        if(classMap.containsKey(name)) {
            classMap.remove(name)
        }
    }

    fun getClass(name: String) = classMap[name]

    fun addJar(file: File) {
        JarFile(file).use { jar ->
            jar.entries().asSequence().forEach { entry ->
                if(entry.name.endsWith(".class")) {
                    val reader = ClassReader(jar.getInputStream(entry))
                    val node = ClassNode()
                    reader.accept(node, ClassReader.SKIP_FRAMES)
                    addClass(node)
                }
            }
        }
    }

    fun saveJar(file: File) {
        if(file.exists()) file.deleteRecursively()
        JarOutputStream(FileOutputStream(file)).use { jos ->
            classes.forEach { cls ->
                val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
                cls.accept(writer)
                jos.putNextEntry(JarEntry(cls.name + ".class"))
                jos.write(writer.toByteArray())
                jos.closeEntry()
            }
        }
    }

    fun clear() {
        classMap.clear()
        inheritanceGraph = InheritanceGraph().also {
            it.addClasspath()
            it.addModulePath()
        }
    }

    fun loadHierarchy() {
        classes.forEach { cls ->
            val parents = inheritanceGraph.getAllParents(cls.name).mapNotNull { getClass(it) }.toList()
            cls.methods.forEach { method ->
                if(parents.none { it.methods.any { it.name == method.name && it.desc == method.desc } }) {
                    topMethods.add(method.identifier)
                }
            }
            cls.fields.forEach { field ->
                if(parents.none { it.fields.any { it.name == field.name } }) {
                    topFields.add(field.identifier)
                }
            }
        }
    }

    fun resolveMethod(owner: String, name: String, desc: String): String? {
        val sig = "$owner.$name$desc"
        if(sig in topMethods) return sig
        if("$name$desc".startsWith("<init>")) return null
        val cls = getClass(owner) ?: return null
        for(parent in inheritanceGraph.getAllParents(cls.name)) {
            return resolveMethod(parent, name, desc) ?: continue
        }
        return null
    }

    fun resolveField(owner: String, name: String): String? {
        val sig = "$owner.$name"
        if(sig in topFields) return sig
        val cls = getClass(owner) ?: return null
        for(parent in inheritanceGraph.getAllParents(cls.name)) {
            return resolveField(parent, name) ?: continue
        }
        return null
    }
}