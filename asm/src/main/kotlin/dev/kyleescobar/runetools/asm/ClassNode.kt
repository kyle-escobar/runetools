package dev.kyleescobar.runetools.asm

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes.ACC_ABSTRACT
import org.objectweb.asm.Opcodes.ACC_INTERFACE

class ClassNode(val pool: ClassPool) {

    constructor() : this(ClassPool.EMPTY_POOL)

    var version: Int = 0
    var access: Int = 0
    var source: String = ""
    lateinit var name: String
    lateinit var superName: String
    val interfaces = mutableListOf<String>()
    val methods = mutableListOf<MethodNode>()
    val fields = mutableListOf<FieldNode>()

    var superClass: ClassNode? = null
    val interfaceClasses = mutableListOf<ClassNode>()

    fun isInterface() = (access and ACC_INTERFACE) != 0
    fun isAbstract() = (access and ACC_ABSTRACT) != 0

    fun accept(visitor: ClassVisitor) {
        val interfs = interfaces.toTypedArray()

        visitor.visit(version, access, name, null, superName, interfs)
        visitor.visitSource(source, null)

        fields.forEach { field ->
            val fv = visitor.visitField(field.access, field.name, field.type.toString(), null, field.value)
            field.accept(fv)
        }

        methods.forEach { method ->
            val mv = visitor.visitMethod(method.access, method.name, method.signature.toString(), null, method.exceptions.toTypedArray())
            method.accept(mv)
        }

        visitor.visitEnd()
    }

    override fun toString(): String {
        return name
    }
}