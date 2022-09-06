package com.github.kyleescobar.runetools.deobfuscator.asm

import com.github.kyleescobar.runetools.deobfuscator.util.field
import com.google.common.collect.MultimapBuilder
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

internal fun MethodNode.init(owner: ClassNode) {
    this.owner = owner
}

var MethodNode.owner: ClassNode by field()

val MethodNode.identifier get() = "${owner.name}.$name$desc"

val MethodNode.overrides: List<MethodNode> get() {
    val list = hashSetOf<MethodNode>()
    this.owner.pool.classes.forEach { cls ->
        cls.methods.forEach methodLoop@ { method ->
            if(method.name == this.name && method.desc == this.desc) {
                val sig = this.owner.pool.resolveMethod(cls.name, method.name, method.desc) ?: return@methodLoop
                val m = this.owner.pool.classes.flatMap { it.methods }.associateBy { it.identifier }[sig] ?: return@methodLoop
                list.add(m)
            }
        }
    }
    return list.toList()
}