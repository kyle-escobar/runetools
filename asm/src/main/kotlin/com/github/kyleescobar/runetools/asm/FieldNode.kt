package com.github.kyleescobar.runetools.asm

import com.github.kyleescobar.runetools.asm.util.field
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

internal fun FieldNode.init(owner: ClassNode) {
    this.owner = owner
}

var FieldNode.owner: ClassNode by field()
val FieldNode.ownerDebug get() = this.owner

val FieldNode.readRefs: HashSet<MethodNode> by field { hashSetOf() }
val FieldNode.writeRefs: HashSet<MethodNode> by field { hashSetOf() }