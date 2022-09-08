package com.github.kyleescobar.runetools.deobfuscator

import dev.kyleescobar.byteflow.ClassGroup

interface Transformer {

    fun run(group: ClassGroup)

}