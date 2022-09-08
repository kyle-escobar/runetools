package dev.kyleescobar.runetools.asm.tree

import dev.kyleescobar.runetools.asm.editor.ClassEditor
import dev.kyleescobar.runetools.asm.reflect.ClassInfo
import dev.kyleescobar.runetools.asm.visitor.NodeVisitor

class ClassNode(val pool: ClassPool, val info: ClassInfo, val editor: ClassEditor) {

    val methods = mutableListOf<MethodNode>()
    val fields = mutableListOf<FieldNode>()

    init {
        info.methods().forEach { method ->
            methods.add(MethodNode(this, method))
        }
        info.fields().forEach { field ->
            fields.add(FieldNode(this, field))
        }
    }

    fun getMethod(name: String, desc: String) = methods.firstOrNull { it.editor().name() == name && it.editor().type().descriptor() == desc }

    fun getField(name: String, desc: String) = fields.firstOrNull { it.editor().name() == name && it.editor().type().descriptor() == desc }

    fun accept(visitor: NodeVisitor) {
        visitor.visitClass(this)
        methods.forEach { method ->
            method.accept(visitor)
        }
        fields.forEach { field ->
            field.accept(visitor)
        }
    }
}