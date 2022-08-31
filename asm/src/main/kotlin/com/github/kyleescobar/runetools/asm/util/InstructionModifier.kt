package com.github.kyleescobar.runetools.asm.util

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

class InstructionModifier {

    companion object {
        private val EMPTY_LIST = InsnList()
    }

    private val insns = InsnList()
    private val replacements = hashMapOf<AbstractInsnNode, InsnList>()
    private val appends = hashMapOf<AbstractInsnNode, InsnList>()
    private val prepends = hashMapOf<AbstractInsnNode, InsnList>()

    fun append(original: AbstractInsnNode, insns: InsnList) {
        appends[original] = insns
    }

    fun append(original: AbstractInsnNode, insn: AbstractInsnNode) {
        val insns = InsnList()
        insns.add(insn)
        append(original, insns)
    }

    fun prepend(original: AbstractInsnNode, insns: InsnList) {
        prepends[original] = insns
    }

    fun prepend(original: AbstractInsnNode, insn: AbstractInsnNode) {
        val insns = InsnList()
        insns.add(insn)
        prepend(original, insns)
    }

    fun replace(original: AbstractInsnNode, insns: InsnList) {
        replacements[original] = insns
    }

    fun replace(original: AbstractInsnNode, vararg insns: AbstractInsnNode) {
        val list = InsnList()
        insns.forEach { list.add(it) }
        replacements[original] = list
    }

    fun add(insn: AbstractInsnNode) {
        this.insns.add(insn)
    }

    fun addAll(insns: List<AbstractInsnNode>) {
        insns.forEach { this.insns.add(it) }
    }

    fun remove(original: AbstractInsnNode) {
        replacements[original] = EMPTY_LIST
    }

    fun removeAll(insns: List<AbstractInsnNode>) {
        insns.forEach { remove(it) }
    }

    fun apply(method: MethodNode) {
        replacements.forEach { (original, insns) ->
            method.instructions.insert(original, insns)
            method.instructions.remove(original)
        }
        prepends.forEach { (original, insns) ->
            method.instructions.insertBefore(original, insns)
        }
        appends.forEach { (original, insns) ->
            method.instructions.insert(original, insns)
        }
        this.insns.forEach { insn ->
            method.instructions.add(insn)
        }
    }
}