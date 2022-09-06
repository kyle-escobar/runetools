package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import com.github.kyleescobar.runetools.deobfuscator.asm.ClassPool
import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.tinylog.kotlin.Logger

class GotoRemover : Transformer {

    private var count = 0

    override fun run(pool: ClassPool) {
        pool.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions.iterator()
                while(insns.hasNext()) {
                    val insn = insns.next()
                    if(insn.opcode != GOTO) continue
                    insn as JumpInsnNode
                    val nextInsn = insn.next
                    if(nextInsn == null || nextInsn !is LabelNode) continue
                    if(insn.label == nextInsn) {
                        insns.remove()
                        count++
                    }
                }
            }
        }

        Logger.info("Removed $count GOTO instructions.")
    }
}