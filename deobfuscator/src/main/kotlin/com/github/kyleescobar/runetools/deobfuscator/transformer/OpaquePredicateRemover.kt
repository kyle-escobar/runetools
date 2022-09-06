package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import com.github.kyleescobar.runetools.deobfuscator.asm.*
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.common.collect.Multimaps
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.*
import org.tinylog.kotlin.Logger
import java.lang.IllegalStateException
import java.lang.reflect.Modifier

class OpaquePredicateRemover : Transformer {

    private var count = 0

    private val opaqueMethods = hashSetOf<MethodNode>()

    override fun run(pool: ClassPool) {
        removeOpaqueMethodChecks(pool)
        removeOpaqueMethodArgs(pool)

        Logger.info("Removed $count opaque method arguments.")
    }

    private fun removeOpaqueMethodArgs(pool: ClassPool) {
        val classMap = pool.classes.associateBy { it.name }
        val topMethods = hashSetOf<String>()

        pool.classes.forEach { cls ->
            val parents = pool.inheritanceGraph.getAllParents(cls.name).mapNotNull { pool.getClass(it) }.toList()
            cls.methods.forEach { method ->
                if(parents.none { it.methods.any { it.name == method.name && it.desc == method.desc} }) {
                    topMethods.add(method.identifier)
                }
            }
        }

        val parentsMap = MultimapBuilder.hashKeys().arrayListValues().build<String, MethodNode>()
        val parents = parentsMap.asMap()

        pool.classes.forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                val sig = pool.resolveMethod(cls.name, method.name, method.desc, topMethods) ?: return@methodLoop
                parentsMap.put(sig, method)
            }
        }

        val itr = parents.iterator()
        for(entry in itr) {
            if(entry.value.any { !it.hasOpaqueArg() }) {
                itr.remove()
            }
        }

        pool.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions
                for(insn in insns) {
                    if(insn !is MethodInsnNode) continue
                    val sig = pool.resolveMethod(insn.owner, insn.name, insn.desc, parents.keys) ?: continue
                    if(insn.previous.opcode !in listOf(BIPUSH, SIPUSH, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, LDC)) {
                        parents.remove(sig)
                    }
                }
            }
        }

        parentsMap.values().forEach { method ->
            method.desc = method.desc.dropLastArg()
            count++
        }

        pool.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions
                for(insn in insns) {
                    if(insn !is MethodInsnNode) continue
                    if(pool.resolveMethod(insn.owner, insn.name, insn.desc, parents.keys) != null) {
                        insn.desc = insn.desc.dropLastArg()
                        insns.remove(insn.previous)
                    }
                }
            }
        }
    }

    private fun removeOpaqueMethodChecks(pool: ClassPool) {
        pool.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions.iterator()
                while(insns.hasNext()) {
                    val insn = insns.next()
                    if(insn.matchOpaqueCheckException(method)) {
                        val label = (insn.next.next as JumpInsnNode).label.label
                        insns.remove()
                        repeat(6) {
                            insns.next()
                            insns.remove()
                        }
                        insns.add(JumpInsnNode(GOTO, LabelNode(label)))
                    } else if(insn.matchOpaqueCheckReturn(method)) {
                        val label = (insn.next.next as JumpInsnNode).label.label
                        insns.remove()
                        repeat(3) {
                            insns.next()
                            insns.remove()
                        }
                        insns.add(JumpInsnNode(GOTO, LabelNode(label)))
                    } else {
                        continue
                    }
                    if(method !in opaqueMethods) {
                        opaqueMethods.add(method)
                    }
                }
            }
        }
    }

    private fun AbstractInsnNode.matchOpaqueCheckException(method: MethodNode): Boolean {
        val i0 = this
        if(i0.opcode != ILOAD) return false
        i0 as VarInsnNode
        if(i0.`var` != (if(Modifier.isStatic(method.access)) -1 else 0) + (Type.getArgumentsAndReturnSizes(method.desc) shr 2) - 1) return false
        val i1 = i0.next
        if(i1.opcode !in listOf(BIPUSH, SIPUSH, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, LDC)) return false
        if(i1.opcode == LDC && (i1 as LdcInsnNode).cst !is Int) return false
        val i2 = i1.next
        if(i2 !is JumpInsnNode || i2.opcode == GOTO) return false
        val i3 = i2.next
        if(i3.opcode != NEW) return false
        val i4 = i3.next
        if(i4.opcode != DUP) return false
        val i5 = i4.next
        if(i5.opcode != INVOKESPECIAL) return false
        i5 as MethodInsnNode
        if(i5.owner != Type.getInternalName(IllegalStateException::class.java)) return false
        val i6 = i5.next
        if(i6.opcode != ATHROW) return false
        return true
    }

    private fun AbstractInsnNode.matchOpaqueCheckReturn(method: MethodNode): Boolean {
        val i0 = this
        if(i0.opcode != ILOAD) return false
        i0 as VarInsnNode
        if(i0.`var` != (if(Modifier.isStatic(method.access)) -1 else 0) + (Type.getArgumentsAndReturnSizes(method.desc) shr 2) - 1) return false
        val i1 = i0.next
        if(i1.opcode !in listOf(BIPUSH, SIPUSH, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, LDC)) return false
        if(i1.opcode == LDC && (i1 as LdcInsnNode).cst !is Int) return false
        val i2 = i1.next
        if(i2 !is JumpInsnNode || i2.opcode == GOTO) return false
        val i3 = i2.next
        if(i3.opcode !in IRETURN..RETURN) return false
        return true
    }

    private fun String.dropLastArg(): String {
        val type = Type.getMethodType(this)
        return Type.getMethodDescriptor(type.returnType, *type.argumentTypes.copyOf(type.argumentTypes.size - 1))
    }

    private fun MethodNode.hasOpaqueArg(): Boolean {
        val args = Type.getArgumentTypes(this.desc)
        if(args.isEmpty()) return false
        if(args.last() !in listOf(INT_TYPE, SHORT_TYPE, BYTE_TYPE)) return false
        if(Modifier.isAbstract(this.access)) return true
        for(insn in this.instructions) {
            if(insn !is VarInsnNode) continue
            if(insn.`var` == (if(Modifier.isStatic(this.access)) -1 else 0) + (Type.getArgumentsAndReturnSizes(this.desc) shr 2) - 1) return false
        }
        return true
    }

    private fun ClassPool.resolveMethod(owner: String, name: String, desc: String, methods: Set<String>): String? {
        val sig = "$owner.$name$desc"
        if(sig in methods) return sig
        if("$name$desc".startsWith("<init>")) return null
        val cls = this.getClass(owner) ?: return null
        for(parent in inheritanceGraph.getAllParents(cls.name)) {
            return this.resolveMethod(parent, name, desc, methods) ?: continue
        }
        return null
    }
}