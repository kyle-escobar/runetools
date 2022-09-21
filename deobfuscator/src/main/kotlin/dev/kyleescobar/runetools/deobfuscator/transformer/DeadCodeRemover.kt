package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.Method
import dev.kyleescobar.runetools.asm.attributes.code.Instruction
import dev.kyleescobar.runetools.asm.attributes.code.Instructions
import dev.kyleescobar.runetools.asm.attributes.code.Label
import dev.kyleescobar.runetools.asm.execution.Execution
import dev.kyleescobar.runetools.deobfuscator.Transformer
import org.tinylog.kotlin.Logger

class DeadCodeRemover : Transformer {

    private var execution: Execution? = null

    private fun removeUnused(m: Method): Int {
        val ins: Instructions = m.code.instructions
        var count = 0
        val insCopy: List<Instruction> = ins.instructions.toMutableList()
        for (j in insCopy.indices) {
            val i = insCopy[j]
            if (!execution!!.executed.contains(i)) {
                val exceptions = m.code.exceptions.exceptions.toTypedArray()
                for (e in exceptions) {
                    if (e.start.next() === i) {
                        e.start = ins.createLabelFor(insCopy[j + 1])
                        if (e.start.next() === e.end.next()) {
                            m.code.exceptions.remove(e)
                            continue
                        }
                    }
                    if (e.handler.next() === i) {
                        m.code.exceptions.remove(e)
                    }
                }
                if (i is Label) continue
                ins.remove(i)
                ++count
            }
        }
        return count
    }

    override fun run(group: ClassGroup) {
        group.buildClassGraph()

        execution = Execution(group)
        execution!!.populateInitialMethods()
        execution!!.run()

        var count = 0
        for (cf in group.classes) {
            for (m in cf.methods) {
                if (m.code == null) continue
                count += removeUnused(m)
            }
        }

        Logger.info("Removed $count dead code instructions.")
    }
}