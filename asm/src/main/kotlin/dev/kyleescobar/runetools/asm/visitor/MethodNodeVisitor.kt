package dev.kyleescobar.runetools.asm.visitor

import dev.kyleescobar.runetools.asm.ClassNode
import dev.kyleescobar.runetools.asm.MethodNode
import dev.kyleescobar.runetools.asm.Signature
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9

class MethodNodeVisitor(owner: ClassNode, access: Int, name: String, signature: Signature, exceptions: List<String>) : MethodVisitor(ASM9) {

    val node = MethodNode(owner, name, signature)

    init {
        node.access = access
        node.code.exceptions.addAll(exceptions)
        owner.methods.add(node)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        node.code.maxStack = maxStack
        node.code.maxLocals = maxLocals
    }


}