package dev.kyleescobar.runetools.asm.visitor

import dev.kyleescobar.runetools.asm.tree.ClassNode
import dev.kyleescobar.runetools.asm.tree.ClassPool
import dev.kyleescobar.runetools.asm.tree.FieldNode
import dev.kyleescobar.runetools.asm.tree.MethodNode

interface NodeVisitor {
    fun visitPool(pool: ClassPool)
    fun visitStart()
    fun visitEnd()
    fun visitClass(cls: ClassNode)
    fun visitMethod(method: MethodNode)
    fun visitField(field: FieldNode)
}