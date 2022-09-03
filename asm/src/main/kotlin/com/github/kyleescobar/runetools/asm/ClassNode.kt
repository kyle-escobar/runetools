package com.github.kyleescobar.runetools.asm

import com.github.kyleescobar.runetools.asm.util.field
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

internal fun ClassNode.init(pool: ClassPool) {
    this.pool = pool
    this.methods.forEach { it.init(this) }
    this.fields.forEach { it.init(this) }
}

var ClassNode.pool: ClassPool by field()
var ClassNode.isSourceClass: Boolean by field { true }

var ClassNode.parent: ClassNode? by field { null }
val ClassNode.children: HashSet<ClassNode> by field { hashSetOf() }
val ClassNode.interfaceClasses: HashSet<ClassNode> by field { hashSetOf() }
val ClassNode.implementers: HashSet<ClassNode> by field { hashSetOf() }

val ClassNode.methodTypeRefs: HashSet<MethodNode> by field { hashSetOf() }
val ClassNode.fieldTypeRefs: HashSet<FieldNode> by field { hashSetOf() }

val ClassNode.strings: HashSet<String> by field { hashSetOf() }

val ClassNode.relatives: MutableList<ClassNode> get() {
    return this.interfaceClasses
        .let { if(this.parent != null) it.plus(this.parent) else it }
        .mapNotNull { it?.let { this.pool.getOrCreateClass(it.name) } }
        .flatMap { it.relatives.plus(it) }
        .toMutableList()
}

fun ClassNode.resolveMethods(name: String, desc: String): List<MethodNode> {
    return this.relatives.flatMap { it.methods }.filter { it.name == name && it.desc == desc }.toList()
}

fun ClassNode.resolveFields(name: String, desc: String): List<FieldNode> {
    return this.relatives.flatMap { it.fields }.filter { it.name == name && it.desc == desc }.toList()
}