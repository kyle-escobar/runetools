package dev.kyleescobar.runetools.asm.code

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.AbstractInsnNode

open class Instruction(val code: Code, private val insn: AbstractInsnNode) {

    val opcode = insn.opcode

    fun accept(visitor: MethodVisitor) {
        visitor.visitInsn(opcode)
    }
}