package dev.kyleescobar.runetools.asm

data class ParameterNode(val owner: MethodNode, val name: String, val access: Int) {

    var variable: Any? = null

}