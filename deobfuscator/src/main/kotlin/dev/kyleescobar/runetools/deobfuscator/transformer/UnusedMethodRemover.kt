package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassFile
import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.Method
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.InvokeInstruction
import dev.kyleescobar.runetools.deob.Deob
import dev.kyleescobar.runetools.deobfuscator.Deobfuscator
import dev.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.runetools.deobfuscator.ext.filterClientClasses
import org.tinylog.kotlin.Logger

class UnusedMethodRemover : Transformer {

    private var count = 0

    private val methods = mutableListOf<Method>()

    private fun ClassFile.extendsApplet(): Boolean {
        if(this.parent != null) {
            return this.parent.extendsApplet()
        }
        return this.superName == "java/applet/Applet"
    }

    override fun run(group: ClassGroup) {
        group.classes.filterClientClasses().forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                if(method.code == null) return@methodLoop
                method.code.instructions.instructions.forEach insnLoop@ { insn ->
                    if(insn !is InvokeInstruction) return@insnLoop
                    methods.addAll(insn.methods)
                }
            }
        }

        group.classes.filterClientClasses().forEach { cls ->
            val extendsApplet = cls.extendsApplet()
            val methods = cls.methods.toTypedArray()
            for(method in methods) {
                if(!Deob.isObfuscated(method.name) && method.name != "<init>") continue
                if(extendsApplet && method.name == "<init>") continue
                if(!methods.contains(method)) {
                    cls.removeMethod(method)
                    count++
                }
            }
        }

        Logger.info("Removed $count unused methods.")
    }
}