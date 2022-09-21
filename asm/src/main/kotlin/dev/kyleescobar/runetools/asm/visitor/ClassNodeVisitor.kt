package dev.kyleescobar.runetools.asm.visitor

import dev.kyleescobar.runetools.asm.ClassNode
import dev.kyleescobar.runetools.asm.Signature
import dev.kyleescobar.runetools.asm.Type
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9

class ClassNodeVisitor : ClassVisitor(ASM9) {

    val node = ClassNode()

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>
    ) {
        node.version = version
        node.access = access
        node.name = name
        node.superName = superName ?: ""
        interfaces.forEach { interf ->
            node.interfaces.add(interf)
        }
    }

    override fun visitSource(source: String, debug: String?) {
        node.source = source
    }

    override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor =
        FieldNodeVisitor(node, access, name, Type(descriptor), value)

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor =
        MethodNodeVisitor(node, access, name, Signature(descriptor), exceptions?.toList() ?: emptyList())

}