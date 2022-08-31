package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.asm.ClassPool
import com.github.kyleescobar.runetools.deobfuscator.Transformer
import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.tinylog.kotlin.Logger

class RedundantGotoRemover : Transformer {

    private var count = 0

    override fun run(pool: ClassPool) {
        pool.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions.iterator()
                while(insns.hasNext()) {
                    val insn0 = insns.next()
                    if(insn0.opcode != GOTO) continue
                    insn0 as JumpInsnNode
                    val insn1 = insn0.next
                    if(insn1 == null || insn1 !is LabelNode) continue
                    if(insn0.label == insn1) {
                        insns.remove()
                        count++
                    }
                }
            }
        }
        Logger.info("Removed $count redundant GOTO instructions.")
    }
}