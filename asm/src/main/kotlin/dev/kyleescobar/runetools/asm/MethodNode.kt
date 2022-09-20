package dev.kyleescobar.runetools.asm

import org.objectweb.asm.MethodVisitor

class MethodNode(val owner: ClassNode, val name: String, val desc: String) {

    var access: Int = 0
    val exceptions = mutableListOf<String>()

    fun accept(visitor: MethodVisitor) {
        visitor.visitEnd()
    }
}