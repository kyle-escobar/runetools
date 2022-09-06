package com.github.kyleescobar.runetools.deobfuscator.asm

import com.github.kyleescobar.runetools.deobfuscator.util.field
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

internal fun FieldNode.init(owner: ClassNode) {
    this.owner = owner
}

var FieldNode.owner: ClassNode by field()

val FieldNode.identifier get() = "${owner.identifier}.$name"