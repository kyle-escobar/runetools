package com.github.kyleescobar.runetools.asm

import com.github.kyleescobar.runetools.asm.util.field
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

var FieldNode.owner: ClassNode by field()
val FieldNode.ownerDebug get() = this.owner

internal fun FieldNode.init(owner: ClassNode) {
    this.owner = owner
}