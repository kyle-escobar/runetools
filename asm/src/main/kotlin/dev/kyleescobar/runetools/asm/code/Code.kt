package dev.kyleescobar.runetools.asm.code

import dev.kyleescobar.runetools.asm.MethodNode

class Code(val method: MethodNode) {

    var maxStack = 0
    var maxLocals = 0
    val exceptions = mutableListOf<String>()


}