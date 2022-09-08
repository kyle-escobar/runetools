package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.byteflow.ClassGroup
import dev.kyleescobar.byteflow.trans.DeadCodeElimination
import org.tinylog.kotlin.Logger

class DeadCodeRemover : Transformer {

    override fun run(group: ClassGroup) {
        group.classes.forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                if(method.name.startsWith("<")) return@methodLoop

                val cfg = method.cfg()
                val deadCodeRemover = DeadCodeElimination(cfg)
                deadCodeRemover.transform()
            }
        }

        Logger.info("Removed dead code instructions.")
    }
}