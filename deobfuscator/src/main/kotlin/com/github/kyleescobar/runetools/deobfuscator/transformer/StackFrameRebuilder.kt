package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import com.github.kyleescobar.runetools.deobfuscator.asm.ClassPool
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.tinylog.kotlin.Logger

class StackFrameRebuilder : Transformer {

    private var count = 0

    override fun run(pool: ClassPool) {
        val newNodes = mutableListOf<ClassNode>()
        pool.classes.forEach { cls ->
            val node = ClassNode()

            val writer = Writer(pool.classes.associateBy { it.name })
            cls.accept(writer)
            val reader = ClassReader(writer.toByteArray())
            reader.accept(node, ClassReader.SKIP_FRAMES)

            newNodes.add(node)
        }

        pool.clear()
        newNodes.forEach { pool.addClass(it) }
        pool.loadHierarchy()
        count += newNodes.size

        Logger.info("Rebuilt $count classes stack-frame's.")
    }

    private class Writer(private val classNames: Map<String, ClassNode>) : ClassWriter(COMPUTE_FRAMES) {

        companion object {
            val OBJECT_INTERNAL_NAME: String = Type.getInternalName(Any::class.java)
        }

        override fun getCommonSuperClass(type1: String, type2: String): String {
            if (isAssignable(type1, type2)) return type1
            if (isAssignable(type2, type1)) return type2
            var t1 = type1
            do {
                t1 = checkNotNull(superClassName(t1, classNames))
            } while (!isAssignable(t1, type2))
            return t1
        }

        private fun isAssignable(to: String, from: String): Boolean {
            if (to == from) return true
            val sup = superClassName(from, classNames) ?: return false
            if (isAssignable(to, sup)) return true
            return interfaceNames(from).any { isAssignable(to, it) }
        }

        private fun interfaceNames(type: String): List<String> {
            return if (type in classNames) {
                classNames.getValue(type).interfaces
            } else {
                Class.forName(type.replace('/', '.')).interfaces.map { Type.getInternalName(it) }
            }
        }

        private fun superClassName(type: String, classNames: Map<String, ClassNode>): String? {
            return if (type in classNames) {
                classNames.getValue(type).superName
            } else {
                val c = Class.forName(type.replace('/', '.'))
                if (c.isInterface) {
                    OBJECT_INTERNAL_NAME
                } else {
                    c.superclass?.let { Type.getInternalName(it) }
                }
            }
        }
    }
}