package com.github.kyleescobar.runetools.asm

import com.github.kyleescobar.runetools.asm.util.field
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode


internal fun MethodNode.init(owner: ClassNode) {
    this.owner = owner
}

var MethodNode.owner: ClassNode by field()

val MethodNode.refsIn: HashSet<MethodNode> by field { hashSetOf() }
val MethodNode.refsOut: HashSet<MethodNode> by field { hashSetOf() }
val MethodNode.fieldReadRefs: HashSet<FieldNode> by field { hashSetOf() }
val MethodNode.fieldWriteRefs: HashSet<FieldNode> by field { hashSetOf() }
val MethodNode.classRefs: HashSet<ClassNode> by field { hashSetOf() }