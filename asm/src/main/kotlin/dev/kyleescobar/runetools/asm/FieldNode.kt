package dev.kyleescobar.runetools.asm

import org.objectweb.asm.FieldVisitor

class FieldNode(val owner: ClassNode, val name: String, val desc: String) {

    var access: Int = 0
    var value: Any? = null

    fun accept(visitor: FieldVisitor) {
        visitor.visitEnd()
    }
}