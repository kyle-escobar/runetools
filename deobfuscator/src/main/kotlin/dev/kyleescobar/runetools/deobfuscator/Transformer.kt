package dev.kyleescobar.runetools.deobfuscator

import dev.kyleescobar.runetools.asm.ClassGroup

interface Transformer {

    fun run(group: ClassGroup)

}