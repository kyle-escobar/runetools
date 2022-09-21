package dev.kyleescobar.runetools.asm.visitor

import dev.kyleescobar.runetools.asm.ClassNode
import dev.kyleescobar.runetools.asm.FieldNode
import dev.kyleescobar.runetools.asm.Type
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes.ASM9

class FieldNodeVisitor(owner: ClassNode, access: Int, name: String, desc: Type, value: Any?) : FieldVisitor(ASM9) {

    val node = FieldNode(owner, name, desc)

    init {
        node.access = access
        node.value = value
        owner.fields.add(node)
    }

}