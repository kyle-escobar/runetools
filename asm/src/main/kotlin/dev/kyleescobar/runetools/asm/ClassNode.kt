package dev.kyleescobar.runetools.asm

import org.objectweb.asm.ClassVisitor

class ClassNode(val pool: ClassPool) {

    constructor() : this(ClassPool.EMPTY_POOL)

    var version: Int = 0
    var access: Int = 0
    var source: String = ""
    lateinit var name: String
    var superName: String? = null
    val interfaces = mutableListOf<String>()
    val methods = mutableListOf<MethodNode>()
    val fields = mutableListOf<FieldNode>()

    fun accept(visitor: ClassVisitor) {
        val interfs = interfaces.toTypedArray()

        visitor.visit(version, access, name, null, superName, interfs)
        visitor.visitSource(source, null)

        fields.forEach { field ->
            val fv = visitor.visitField(field.access, field.name, field.desc, null, field.value)
            field.accept(fv)
        }



        visitor.visitEnd()
    }
}