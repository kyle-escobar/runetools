package dev.kyleescobar.runetools.deobfuscator

import dev.kyleescobar.runetools.asm.tree.ClassPool

interface Transformer {

    fun run(group: ClassPool)

}