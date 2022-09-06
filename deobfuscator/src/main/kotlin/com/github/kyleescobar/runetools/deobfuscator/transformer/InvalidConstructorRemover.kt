package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import com.github.kyleescobar.runetools.deobfuscator.asm.ClassPool
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode
import org.tinylog.kotlin.Logger

class InvalidConstructorRemover : Transformer {

    private var count = 0

    override fun run(pool: ClassPool) {
        pool.classes.forEach { cls ->
            val methods = cls.methods.iterator()
            while(methods.hasNext()) {
                val method = methods.next()
                if(method.isInvalidConstructor()) {
                    methods.remove()
                    count++
                }
            }
        }

        Logger.info("Removed $count invalid constructor methods.")
    }

    private fun MethodNode.isInvalidConstructor(): Boolean {
        if(this.name != "<init>") return false
        if(Type.getArgumentTypes(this.desc).isNotEmpty()) return false
        if(this.exceptions != listOf(Type.getType(Throwable::class.java).internalName)) return false

        val insns = instructions.toArray().filter { it.opcode > 0 }.iterator()
        if(!insns.hasNext() || insns.next().opcode != ALOAD) return false
        if(!insns.hasNext() || insns.next().opcode != INVOKESPECIAL) return false
        if(!insns.hasNext() || insns.next().opcode != NEW) return false
        if(!insns.hasNext() || insns.next().opcode != DUP) return false
        if(!insns.hasNext() || insns.next().opcode != INVOKESPECIAL) return false
        if(!insns.hasNext() || insns.next().opcode != ATHROW) return false
        return !insns.hasNext()
    }
}