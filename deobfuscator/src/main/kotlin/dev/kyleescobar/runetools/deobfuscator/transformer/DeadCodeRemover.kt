package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.attributes.code.Exception
import dev.kyleescobar.runetools.asm.attributes.code.Instruction
import dev.kyleescobar.runetools.asm.attributes.code.Label
import dev.kyleescobar.runetools.asm.execution.Execution
import dev.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.runetools.deobfuscator.ext.filterClientClasses
import org.tinylog.kotlin.Logger

class DeadCodeRemover : Transformer {

    private var count = 0

    private lateinit var execution: Execution

    override fun run(group: ClassGroup) {
        group.buildClassGraph()

        execution = Execution(group)
        execution.populateInitialMethods()
        execution.run()

        group.classes.filterClientClasses().forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                if(method.code ==  null) return@methodLoop
                val insns = method.code.instructions
                val insnsCopy = mutableListOf<Instruction>().also { it.addAll(insns.instructions) }

                for(i in insnsCopy.indices) {
                    val insn = insnsCopy[i]
                    if(!execution.executed.contains(insn)) {
                        val exceptions = mutableListOf<Exception>().also { it.addAll(method.code.exceptions.exceptions) }
                        for(exception in exceptions) {
                            if(exception.start.next() == insn) {
                                exception.start = insns.createLabelFor(insnsCopy[i + 1])
                                if(exception.start.next() == exception.end.next()) {
                                    method.code.exceptions.remove(exception)
                                    continue
                                }
                            }
                            if(exception.handler.next() == insn) {
                                method.code.exceptions.remove(exception)
                            }
                        }

                        if(insn is Label) {
                            continue
                        }

                        insns.remove(insn)
                        count++
                    }
                }
            }
        }

        Logger.info("Removed $count dead-code instructions.")
    }
}