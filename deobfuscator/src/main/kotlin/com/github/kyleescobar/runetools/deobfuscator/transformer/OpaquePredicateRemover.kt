package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.asm.ClassPool
import com.github.kyleescobar.runetools.asm.util.InstructionModifier
import com.github.kyleescobar.runetools.asm.util.LabelMap
import com.github.kyleescobar.runetools.asm.util.next
import com.github.kyleescobar.runetools.deobfuscator.Transformer
import com.google.common.collect.MultimapBuilder
import com.javadeobfuscator.deobfuscator.utils.Utils
import me.coley.analysis.util.InheritanceGraph
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.tinylog.kotlin.Logger
import java.lang.reflect.Modifier

class OpaquePredicateRemover : Transformer {

    private var checkCount = 0
    private var argCount = 0

    override fun run(pool: ClassPool) {
        removeChecks(pool)
        removeArgs(pool)
    }

    private fun removeChecks(pool: ClassPool) {
        pool.classes.forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                val lastArgIndex = method.lastArgIndex()
                val insns = method.instructions.iterator()
                while(insns.hasNext()) {
                    val insn = insns.next()

                    /**
                     * === RETURN PREDICATE PATTERN ===
                     * ILOAD
                     * <int>
                     * <if>
                     * <return>
                     */
                    val patternOpcodes = if(insn.matchesReturnPattern(lastArgIndex)) 4

                    /**
                     * === EXCEPTION PREDICATE PATTERN ===
                     * ILOAD
                     * <int>
                     * <if>
                     * NEW
                     * DUP
                     * INVOKESPECIAL (IllegalStateException.java)
                     * ATHROW
                     */
                    else if(insn.matchesExceptionPattern(lastArgIndex)) 7
                    else continue

                    val label = (insn.next.next as JumpInsnNode).label.label
                    insns.remove()
                    repeat(patternOpcodes - 1) {
                        insns.next()
                        insns.remove()
                    }
                    insns.add(JumpInsnNode(GOTO, LabelNode(label)))
                    checkCount++
                }
            }
        }
        Logger.info("Removed $checkCount opaque predicate checks.")
    }

    private fun removeArgs(pool: ClassPool) {
        val classes = pool.classes.associateBy { it.name }
        val topMethods = hashSetOf<String>()
        val methodOverridesMap = MultimapBuilder.hashKeys().arrayListValues().build<String, Pair<ClassNode, MethodNode>>()
        val methodOverrides = methodOverridesMap.asMap()

        pool.classes.forEach { cls ->
            val parents = getParentClasses(cls, classes)
            cls.methods.forEach { method ->
                if(parents.none { it.methods.any { it.name == method.name && it.desc == method.desc } }) {
                    topMethods.add("${cls.name}.${method.name}${method.desc}")
                }
            }
        }

        pool.classes.forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                val override = getMethodOverrides(cls.name, method.name, method.desc, topMethods.toList(), classes) ?: return@methodLoop
                methodOverridesMap.put(override, cls to method)
            }
        }

        val it = methodOverrides.iterator()
        it.forEach { entry ->
            if(entry.value.any { !it.second.hasOpaqueArg() }) {
                it.remove()
            }
        }

        pool.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions
                for(insn in insns) {
                    if(insn !is MethodInsnNode) continue
                    val override = getMethodOverrides(insn.owner, insn.name, insn.desc, methodOverrides.keys.toList(), classes) ?: continue
                    if(!Utils.isInteger(insn.previous)) {
                        methodOverrides.remove(override)
                    }
                }
            }
        }

        methodOverridesMap.values().forEach {
            val newDesc = it.second.desc.dropOpaqueArgDesc()
            it.second.desc = newDesc
            argCount++
        }

        pool.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions
                for(insn in insns) {
                    if(insn !is MethodInsnNode) continue
                    if(getMethodOverrides(insn.owner, insn.name, insn.desc, methodOverrides.keys.toList(), classes) != null) {
                        insn.desc = insn.desc.dropOpaqueArgDesc()
                        val prev = insn.previous
                        insns.remove(prev)
                    }
                }
            }
        }

        Logger.info("Removed $argCount opaque predicate arguments.")
    }

    private fun MethodNode.lastArgIndex(): Int {
        val offset = if(Modifier.isStatic(access)) 1 else 0
        return (Type.getArgumentsAndReturnSizes(desc) shr 2) - offset - 1
    }

    private fun AbstractInsnNode.matchesExceptionPattern(lastArgIndex: Int): Boolean {
        val i0 = this
        if(i0.opcode != ILOAD) return false
        i0 as VarInsnNode
        if(i0.`var` != lastArgIndex) return false
        val i1 = i0.next
        if(!Utils.isInteger(i1)) return false
        val i2 = i1.next
        if(!(i2 is JumpInsnNode && i2.opcode != GOTO)) return false
        val i3 = i2.next
        if(i3.opcode != NEW) return false
        val i4 = i3.next
        if(i4.opcode != DUP) return false
        val i5 = i4.next
        if(i5.opcode != INVOKESPECIAL) return false
        i5 as MethodInsnNode
        if(i5.owner != Type.getInternalName(java.lang.IllegalStateException::class.java)) return false
        val i6 = i5.next
        if(i6.opcode != ATHROW) return false
        return true
    }

    private fun AbstractInsnNode.matchesReturnPattern(lastArgIndex: Int): Boolean {
        val i0 = this
        if(i0.opcode != ILOAD) return false
        i0 as VarInsnNode
        if(i0.`var` != lastArgIndex) return false
        val i1 = i0.next
        if(!Utils.isInteger(i1)) return false
        val i2 = i1.next
        if(!(i2 is JumpInsnNode && i2.opcode != GOTO)) return false
        val i3 = i2.next
        if(i3.opcode !in IRETURN..RETURN) return false
        return true
    }

    private fun MethodNode.hasOpaqueArg(): Boolean {
        val args = Type.getArgumentTypes(this.desc)
        if(args.isEmpty()) return false
        val lastArg = args.last()
        if(lastArg !in arrayOf(BYTE_TYPE, SHORT_TYPE, INT_TYPE)) return false
        if(Modifier.isAbstract(this.access)) return true
        val lastArgIndex = (if(Modifier.isStatic(this.access)) -1 else 0) + (Type.getArgumentsAndReturnSizes(this.desc) shr 2) - 1
        for(insn in this.instructions) {
            if(insn !is VarInsnNode) continue
            if(insn.`var` == lastArgIndex) return false
        }
        return true
    }

    private fun String.dropOpaqueArgDesc(): String {
        val type = Type.getMethodType(this)
        return Type.getMethodDescriptor(type.returnType, *type.argumentTypes.copyOf(type.argumentTypes.size - 1))
    }

    private fun getParentClasses(cls: ClassNode, classes: Map<String, ClassNode>): List<ClassNode> {
        val results = mutableListOf<ClassNode>()
        results.addAll(cls.interfaces.plus(cls.superName).mapNotNull { classes[it] }.flatMap { getParentClasses(it, classes).plus(it) })
        return results
    }

    private fun getMethodOverrides(owner: String, name: String, desc: String, methods: List<String>, classes: Map<String, ClassNode>): String? {
        val key = "$owner.$name$desc"
        if(key in methods) return key
        if(name == "<init>") return null
        val cls = classes[owner] ?: return null
        for(parent in getParentClasses(cls, classes)) {
            return getMethodOverrides(parent.name, name, desc, methods, classes) ?: continue
        }
        return null
    }
}