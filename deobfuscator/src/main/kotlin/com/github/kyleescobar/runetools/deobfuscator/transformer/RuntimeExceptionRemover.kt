package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import com.github.kyleescobar.runetools.deobfuscator.asm.ClassPool
import org.objectweb.asm.Type
import org.tinylog.kotlin.Logger
import java.lang.RuntimeException

class RuntimeExceptionRemover : Transformer {

    private var count = 0

    override fun run(pool: ClassPool) {
        pool.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val startCount = method.tryCatchBlocks.size
                method.tryCatchBlocks.removeIf { it.type == Type.getInternalName(RuntimeException::class.java) }
                count += startCount - method.tryCatchBlocks.size
            }
        }

        Logger.info("Removed $count 'RuntimeException' try-catch blocks.")
    }
}