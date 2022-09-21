package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.attributes.code.Exception
import dev.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.runetools.deobfuscator.ext.filterClientClasses
import org.tinylog.kotlin.Logger

class RuntimeExceptionRemover : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        group.classes.filterClientClasses().forEach classLoop@ { cls ->
            cls.methods.forEach methodLoop@ { method ->
                if(method.code == null) return@methodLoop
                if(cls.name == "client" && method.name == "init") return@methodLoop

                val toRemove = mutableListOf<Exception>()
                method.code.exceptions?.exceptions?.forEach { exception ->
                    if(exception.catchType != null && exception.catchType.name == "java/lang/RuntimeException") {
                        toRemove.add(exception)
                        count++
                    }
                }
                toRemove.forEach { method.code.exceptions.remove(it) }
            }
        }

        Logger.info("Removed $count 'RuntimeException' try-catch blocks.")
    }
}