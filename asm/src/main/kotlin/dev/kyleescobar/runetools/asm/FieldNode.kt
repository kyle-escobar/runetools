package dev.kyleescobar.runetools.asm

import org.objectweb.asm.FieldVisitor

class FieldNode(val owner: ClassNode, val name: String, val type: Type) {

    var access: Int = 0
    var value: Any? = null

    fun accept(visitor: FieldVisitor) {
        visitor.visitEnd()
    }

    override fun toString(): String {
        return "$owner.$name"
    }
}