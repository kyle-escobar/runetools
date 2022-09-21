package dev.kyleescobar.runetools.asm

import dev.kyleescobar.runetools.asm.util.JarUtil
import java.io.File

class ClassPool private constructor(private var isEmptyPool: Boolean) {

    constructor() : this(false)

    companion object {

        val EMPTY_POOL = ClassPool(true)

        fun fromJar(file: File): ClassPool {
            return JarUtil.load(file)
        }
    }

    private val classMap = hashMapOf<String, ClassNode>()
    private val ignoredClassMap = hashMapOf<String, ClassNode>()

    val classes get() = classMap.values.toList()
    val ignoredClasses get() = ignoredClassMap.values.toList()

    val size get() = classMap.values.size

    fun addClass(node: ClassNode, isIgnored: Boolean = false) {
        if(isEmptyPool) throw IllegalStateException("Cannot add class to the empty pool.")
        if(!isIgnored) classMap[node.name] = node
        else ignoredClassMap[node.name] = node
    }

    fun removeClass(name: String) {
        classMap.remove(name)
    }

    fun removeClass(node: ClassNode) {
        removeClass(node.name)
    }

    fun getClass(name: String): ClassNode? = classMap[name]

    fun getIgnoredClass(name: String): ClassNode? = ignoredClassMap[name]

    fun findClass(name: String): ClassNode? = getClass(name) ?: getIgnoredClass(name)

    fun iterator(): Iterator<ClassNode> = classMap.values.iterator()

    fun containsClass(name: String) = classMap.containsKey(name)

    fun containsIgnoredClass(name: String) = ignoredClassMap.containsKey(name)

    fun forEach(block: (ClassNode) -> Unit) {
        classMap.values.forEach { cls ->
            block(cls)
        }
    }

    fun iterateEach(block: (cls: ClassNode, it: Iterator<ClassNode>) -> Unit) {
        val iter = classMap.values.iterator()
        while(iter.hasNext()) {
            val node = iter.next()
            block(node, iter)
        }
    }
}