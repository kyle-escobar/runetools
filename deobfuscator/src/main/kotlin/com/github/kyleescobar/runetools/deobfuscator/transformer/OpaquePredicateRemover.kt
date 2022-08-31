package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.asm.ClassPool
import com.github.kyleescobar.runetools.asm.util.InstructionModifier
import com.github.kyleescobar.runetools.asm.util.LabelMap
import com.github.kyleescobar.runetools.asm.util.next
import com.github.kyleescobar.runetools.deobfuscator.Transformer
import com.javadeobfuscator.deobfuscator.utils.Utils
import me.coley.analysis.util.InheritanceGraph
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.AbstractInsnNode
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
        //removeArgs(pool)
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
        val inheritanceGraph = InheritanceGraph()
        inheritanceGraph.addClasspath()
        inheritanceGraph.addModulePath()
        pool.classMap.values.forEach {
            val parents = mutableListOf<String>()
            parents.add(it.superName)
            parents.addAll(it.interfaces)
            inheritanceGraph.add(it.name, parents)
        }

        val mappings = mutableMapOf<String, String>()

        pool.classes.forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                if(method.name.startsWith("<")) return@methodLoop
                if(mappings.containsKey("${cls.name}.${method.name}${method.desc}")) return@methodLoop
                if(method.hasOpaqueArg()) {
                    val oldDesc = method.desc
                    val newDesc = method.desc.dropOpaqueArgDesc()

                    /*
                     * Update any possible child overrides of this method in
                     * the class hierarchy.
                     */
                    inheritanceGraph.getAllChildren(cls.name).forEach { child ->
                        val childMethod = pool.getClass(child)?.methods?.firstOrNull { it.name == method.name && it.desc == method.desc }
                        if(childMethod != null) {
                            if(!childMethod.name.startsWith("<")) {
                                mappings["$child.${method.name}$oldDesc"] = newDesc
                                argCount++
                            }
                        }
                    }

                    /*
                     * Now update the current method's descriptor
                     */
                    mappings["${cls.name}.${method.name}$oldDesc"] = newDesc
                    argCount++
                }
            }
        }

        /*
         * Now we loop through all method instructions in order to update them as well.
         */
        pool.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions
                for(insn in insns) {
                    if(insn !is MethodInsnNode) continue
                    val key = "${insn.owner}.${insn.name}${insn.desc}"
                    if(!mappings.containsKey(key)) continue
                    insn.desc = mappings[key]!!

                    val prev = insn.previous
                    if(Utils.willPushToStack(prev.opcode)) {
                        insns.remove(prev)
                    }
                }
            }
        }

        pool.classes.forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                val key = "${cls.name}.${method.name}${method.desc}"
                val newDesc = mappings[key] ?: return@methodLoop
                println("$key -> $newDesc")
                method.desc = newDesc
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
}