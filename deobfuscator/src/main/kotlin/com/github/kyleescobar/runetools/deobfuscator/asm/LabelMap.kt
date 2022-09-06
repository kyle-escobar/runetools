package com.github.kyleescobar.runetools.deobfuscator.asm

import org.objectweb.asm.tree.LabelNode

class LabelMap : AbstractMap<LabelNode, LabelNode>() {
    private val map = hashMapOf<LabelNode, LabelNode>()
    override val entries: Set<Map.Entry<LabelNode, LabelNode>> get() = throw IllegalStateException()
    override fun get(key: LabelNode) = map.getOrPut(key) { LabelNode() }
}