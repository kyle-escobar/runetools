package dev.kyleescobar.runetools.asm.visitor

import dev.kyleescobar.runetools.asm.tree.*

open class ExprVisitor : TreeVisitor(), NodeVisitor {
    lateinit var pool: ClassPool
    lateinit var cls: ClassNode
    lateinit var method: MethodNode
    lateinit var field: FieldNode

    override fun visitPool(pool: ClassPool) {
        this.pool = pool
    }

    override fun visitClass(cls: ClassNode) {
        this.cls = cls
    }

    override fun visitMethod(method: MethodNode) {
        this.method = method
    }

    override fun visitField(field: FieldNode) {
        this.field = field
    }

    override fun visitStart() {}

    override fun visitEnd() {}
}