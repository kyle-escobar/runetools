package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.attributes.code.Instruction
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.ComparisonInstruction
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.JumpingInstruction
import dev.kyleescobar.runetools.asm.attributes.code.instructions.AThrow
import dev.kyleescobar.runetools.asm.attributes.code.instructions.Goto
import dev.kyleescobar.runetools.asm.attributes.code.instructions.If
import dev.kyleescobar.runetools.asm.attributes.code.instructions.New
import dev.kyleescobar.runetools.asm.execution.Execution
import dev.kyleescobar.runetools.asm.execution.InstructionContext
import dev.kyleescobar.runetools.asm.execution.MethodContext
import dev.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.runetools.deobfuscator.ext.filterClientClasses
import org.tinylog.kotlin.Logger

class IllegalStateExceptionRemover : Transformer {

    private var count = 0
    private val interesting = hashSetOf<Instruction>()
    private val toRemove = mutableListOf<InstructionContext>()

    override fun run(group: ClassGroup) {
        findInteresting(group)

        val execution = Execution(group)
        execution.addExecutionVisitor { visitInsn(it) }
        execution.addMethodContextVisitor { visitMethod(it) }
        execution.populateInitialMethods()
        execution.run()

        Logger.info("Removed $count 'IllegalStateException' try-catch blocks.")
    }

    private fun findInteresting(group: ClassGroup) {
        group.classes.filterClientClasses().forEach { cls ->
            for(method in cls.methods) {
                val code = method.code ?: continue
                val insns = code.instructions
                val insnList = insns.instructions
                for(i in insnList.indices) {
                    val insn1 = insnList[i]
                    if(insn1 !is ComparisonInstruction) continue
                    val insn2 = insnList[i + 1]
                    if(insn2 !is New) continue
                    val klass = insn2.newClass
                    if(!klass.name.contains("java/lang/IllegalStateException")) continue
                    interesting.add(insn1)
                }
            }
        }
    }

    private fun visitInsn(ctx: InstructionContext) {
        if(interesting.contains(ctx.instruction)) {
            toRemove.add(ctx)
        }
    }

    private fun visitMethod(ctx: MethodContext) {
        for(insnCtx in toRemove) {
            process(insnCtx)
        }
        toRemove.clear()
    }

    private fun process(ctx: InstructionContext) {
        var insn = ctx.instruction
        val insns = insn.instructions ?: return
        val insnList = insns.instructions

        insn as JumpingInstruction
        val to = insn.jumps.first()

        if(insn is If) {
            ctx.removeStack(1)
        }
        ctx.removeStack(0)

        val idx = insnList.indexOf(insn)
        while(insn !is AThrow) {
            insns.remove(insn)
            insn = insnList[idx]
        }

        insns.remove(insn)
        val goto = Goto(insns, insns.createLabelFor(to))
        insnList.add(idx, goto)

        count++
    }
}