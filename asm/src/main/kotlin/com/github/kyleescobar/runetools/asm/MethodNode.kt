package com.github.kyleescobar.runetools.asm

import com.github.kyleescobar.runetools.asm.util.field
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

var MethodNode.owner: ClassNode by field()

internal fun MethodNode.init(owner: ClassNode) {
    this.owner = owner
}