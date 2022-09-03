package com.github.kyleescobar.runetools.asm

import com.github.kyleescobar.runetools.asm.util.field
import org.objectweb.asm.tree.ClassNode

var ClassNode.pool: ClassPool by field()
var ClassNode.isSourceClass: Boolean by field { true }

internal fun ClassNode.init(pool: ClassPool) {
    this.pool = pool
    this.methods.forEach { it.init(this) }
    this.fields.forEach { it.init(this) }
}
