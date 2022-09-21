package dev.kyleescobar.runetools.asm

import org.objectweb.asm.MethodVisitor

class MethodNode(val owner: ClassNode, val name: String, val signature: Signature) {

    var access: Int = 0
    val exceptions = mutableListOf<String>()
    val parameters = mutableListOf<ParameterNode>()

    fun accept(visitor: MethodVisitor) {
        parameters.forEach {
            visitor.visitParameter(it.name, it.access)
        }

        visitor.visitEnd()
    }

    override fun toString(): String {
        return "$owner.$name$signature"
    }
}