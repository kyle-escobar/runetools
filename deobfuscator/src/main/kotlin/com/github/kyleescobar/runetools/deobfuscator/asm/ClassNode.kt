package com.github.kyleescobar.runetools.deobfuscator.asm

import com.github.kyleescobar.runetools.deobfuscator.util.field
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

internal fun ClassNode.init(pool: ClassPool) {
    this.pool = pool
    methods.forEach { it.init(this) }
    fields.forEach { it.init(this) }
}

var ClassNode.pool: ClassPool by field()

val ClassNode.identifier get() = this.name

fun ClassNode.getMethod(name: String, desc: String): MethodNode? {
    return this.methods.firstOrNull { it.name == name && it.desc == desc }
}

fun ClassNode.getField(name: String, desc: String): FieldNode? {
    return this.fields.firstOrNull { it.name == name && it.desc == desc }
}