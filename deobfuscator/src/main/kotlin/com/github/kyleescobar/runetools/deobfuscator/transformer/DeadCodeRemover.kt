package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import com.github.kyleescobar.runetools.deobfuscator.asm.ClassPool
import com.github.kyleescobar.runetools.deobfuscator.asm.owner
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.tinylog.kotlin.Logger

class DeadCodeRemover : Transformer {

    private var count = 0

    override fun run(pool: ClassPool) {
        pool.classes.forEach { cls ->
            cls.methods.forEach { method ->
                try {
                    val frames = Analyzer(BasicInterpreter()).analyze(method.owner.name, method)
                    val insns = method.instructions.toArray()
                    for(i in frames.indices) {
                        if(frames[i] == null) {
                            method.instructions.remove(insns[i])
                            count++
                        }
                    }
                } catch(e : Exception) { /* Do nothing */ }
            }
        }

        Logger.info("Removed $count dead-code instructions.")
    }
}