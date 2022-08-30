package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.asm.ClassPool
import com.github.kyleescobar.runetools.deobfuscator.Transformer
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.tinylog.kotlin.Logger

class DeadCodeRemover : Transformer {

    private var count = 0

    override fun run(pool: ClassPool) {
        pool.classes.forEach { cls ->
            cls.methods.forEach { method ->
                try {
                    val frames = Analyzer(BasicInterpreter()).analyze(cls.name, method)
                    val insns = method.instructions.toArray()
                    for(i in frames.indices) {
                        if(frames[i] == null) continue
                        method.instructions.remove(insns[i])
                        count++
                    }
                } catch(e : Exception) { throw Exception() }
            }
        }

        Logger.info("Removed $count dead bytecode instructions.")
    }
}