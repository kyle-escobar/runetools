package dev.kyleescobar.runetools.asm.visitor

import dev.kyleescobar.runetools.asm.ClassNode
import org.objectweb.asm.ClassVisitor
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

}