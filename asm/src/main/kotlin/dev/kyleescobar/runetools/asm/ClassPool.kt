package dev.kyleescobar.runetools.asm

class ClassPool {

    companion object {
        internal val EMPTY_POOL = ClassPool()
    }

    private val classMap = hashMapOf<String, ClassNode>()

    val classes get() = classMap.values.toList()

    fun addClass(node: ClassNode) {
        classMap[node.name] = node
    }

    fun removeClass(name: String) {
        classMap.remove(name)
    }

    fun removeClass(node: ClassNode) {
        removeClass(node.name)
    }

}