package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassFile
import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.Method
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.InvokeInstruction
import dev.kyleescobar.runetools.deob.Deob
import dev.kyleescobar.runetools.deobfuscator.Transformer
import org.tinylog.kotlin.Logger

class UnusedMethodRemover : Transformer {

    private var count = 0
    private val methods: MutableSet<Method?> = HashSet()

    override fun run(group: ClassGroup) {
        for (cf in group.classes) {
            for (method in cf.methods) {
                findMethodInvokes(method)
            }
        }

        for (cf in group.classes) {
            val extendsApplet = extendsApplet(cf)
            for (method in cf.methods.toList()) {
                // constructors can't be renamed, but are obfuscated
                if (!Deob.isObfuscated(method.name) && !method.name.equals("<init>")) {
                    continue
                }
                if (extendsApplet && method.name.equals("<init>")) {
                    continue
                }
                if (!methods.contains(method)) {
                    cf.removeMethod(method)
                    ++count
                }
            }
        }

        Logger.info("Removed $count unused methods.")
    }

    private fun findMethodInvokes(method: Method) {
        val code = method.code ?: return
        for (i in code.instructions.instructions) {
            if (i !is InvokeInstruction) {
                continue
            }
            val ii = i as InvokeInstruction
            methods.addAll(ii.methods)
        }
    }

    private fun extendsApplet(cf: ClassFile): Boolean {
        return if (cf.parent != null) {
            extendsApplet(cf.parent)
        } else cf.superName.equals("java/applet/Applet")
    }
}