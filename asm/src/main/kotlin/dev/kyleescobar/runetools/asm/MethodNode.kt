package dev.kyleescobar.runetools.asm

import dev.kyleescobar.runetools.asm.code.Code
import org.objectweb.asm.MethodVisitor

class MethodNode(val owner: ClassNode, val name: String, val signature: Signature) {

    var access: Int = 0

    val code = Code(this)

    fun accept(visitor: MethodVisitor) {
        visitor.visitEnd()
    }

    override fun toString(): String {
        return "$owner.$name$signature"
    }
}