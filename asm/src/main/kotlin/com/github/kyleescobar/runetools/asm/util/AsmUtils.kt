package com.github.kyleescobar.runetools.asm.util

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode

fun AbstractInsnNode.isInstruction(): Boolean = this !is LineNumberNode && this !is FrameNode && this !is LabelNode

fun AbstractInsnNode.next(amount: Int): AbstractInsnNode {
    var cur = this
    repeat(amount) {
        cur = cur.next
    }
    return cur
}

fun AbstractInsnNode.previous(amount: Int): AbstractInsnNode {
    var cur = this
    repeat(amount) {
        cur = cur.previous
    }
    return cur
}

fun AbstractInsnNode.isTerminating(): Boolean = when(this.opcode) {
    RETURN,
    ARETURN,
    IRETURN,
    FRETURN,
    DRETURN,
    LRETURN,
    ATHROW,
    TABLESWITCH,
    LOOKUPSWITCH,
    GOTO -> true
    else -> false
}

fun InsnList.copy(): InsnList {
    val newInsnList = InsnList()
    var insn = this.first
    while(insn != null) {
        newInsnList.add(insn)
        insn = insn.next
    }
    return newInsnList
}

fun InsnList.clone(): InsnList {
    val newInsnList = InsnList()
    val labels = hashMapOf<LabelNode, LabelNode>()
    var insn = this.first
    while(insn != null) {
        if(insn is LabelNode) {
            labels[insn] = LabelNode()
        }
        insn = insn.next
    }

    insn = this.first
    while(insn != null) {
        newInsnList.add(insn.clone(labels))
        insn = insn.next
    }

    return newInsnList
}
