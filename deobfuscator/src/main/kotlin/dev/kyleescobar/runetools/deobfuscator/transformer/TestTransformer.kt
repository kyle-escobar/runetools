package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.deobfuscator.Transformer
import org.tinylog.kotlin.Logger

class TestTransformer : Transformer {

    override fun run(group: ClassGroup) {
        Logger.info("Boom - test transfromer!")
    }
}