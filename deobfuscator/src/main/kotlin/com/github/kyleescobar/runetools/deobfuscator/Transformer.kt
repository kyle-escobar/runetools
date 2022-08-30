package com.github.kyleescobar.runetools.deobfuscator

import com.github.kyleescobar.runetools.asm.ClassPool

interface Transformer {

    fun run(pool: ClassPool)

}