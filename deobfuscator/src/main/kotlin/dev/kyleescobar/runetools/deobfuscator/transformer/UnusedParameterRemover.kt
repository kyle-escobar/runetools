package dev.kyleescobar.runetools.deobfuscator.transformer

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.Method
import dev.kyleescobar.runetools.asm.attributes.Code
import dev.kyleescobar.runetools.asm.attributes.code.Instruction
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.InvokeInstruction
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.LVTInstruction
import dev.kyleescobar.runetools.asm.execution.Execution
import dev.kyleescobar.runetools.asm.execution.InstructionContext
import dev.kyleescobar.runetools.asm.execution.StackContext
import dev.kyleescobar.runetools.asm.signature.Signature
import dev.kyleescobar.runetools.asm.signature.util.VirtualMethods
import dev.kyleescobar.runetools.deob.Deob
import dev.kyleescobar.runetools.deob.DeobAnnotations
import dev.kyleescobar.runetools.deobfuscator.Transformer
import org.tinylog.kotlin.Logger
import java.util.*

class UnusedParameterRemover : Transformer {


    private val unused: MutableMap<List<Method>, Collection<Int>> = HashMap<List<Method>, Collection<Int>>()
    private val invokes: Multimap<Instruction, InstructionContext> = HashMultimap.create<Instruction, InstructionContext>()

    private fun visit(ictx: InstructionContext) {
        val i: Instruction = ictx.instruction ?: return
        invokes.put(i, ictx)
    }

    private fun buildUnused(group: ClassGroup) {
        unused.clear()
        for (cf in group.classes) {
            for (m in cf.methods) {
                if (!Deob.isObfuscated(m.name)) {
                    continue
                }
                val ms: List<Method> = VirtualMethods.getVirtualMethods(m)
                val u = this.findUnusedParameters(ms)
                if (!u.isEmpty()) {
                    unused[ms] = u
                }
            }
        }
    }

    private fun shouldRemove(m: Method, parameter: Int): Boolean {
        val a = m.findAnnotation(DeobAnnotations.OBFUSCATED_SIGNATURE) ?: return false
        val str = a["descriptor"]
        return parameter + 1 == Signature(str as String).size()
    }

    private fun processUnused(execution: Execution, group: ClassGroup): Int {
        var count = 0
        for (entry in unused) {
            val m = entry.key
            val u = entry.value
            val offset = if (m.size == 1 && m[0].isStatic) 0 else 1
            for (unusedParameter in u) {
                if (!shouldRemove(m[0], unusedParameter)) {
                    continue
                }
                val signature: Signature = m[0].descriptor
                val lvtIndex = getLvtIndex(signature, offset, unusedParameter)
                removeParameter(group, m, signature, execution, unusedParameter, lvtIndex)
                break
            }
            ++count
        }
        return count
    }

    private fun findUnusedParameters(method: Method): Set<Int> {
        val offset = if (method.isStatic) 0 else 1
        val signature: Signature = method.descriptor
        val unusedParams: MutableList<Int> = ArrayList()
        var variableIndex = 0
        var lvtIndex = offset
        while (variableIndex < signature.size()) {
            val lv: List<Instruction?>? = method.findLVTInstructionsForVariable(lvtIndex)
            if (lv == null || lv.isEmpty()) {
                unusedParams.add(variableIndex)
            }
            lvtIndex += signature.getTypeOfArg(variableIndex++).size
        }
        return ImmutableSet.copyOf(unusedParams)
    }

    private fun getLvtIndex(signature: Signature, offset: Int, parameter: Int): Int {
        // get lvt index for parameter
        var lvtIndex = offset
        var variableIndex = 0
        while (variableIndex < parameter) {
            lvtIndex += signature.getTypeOfArg(variableIndex++).size
        }
        return lvtIndex
    }

    fun findUnusedParameters(methods: Collection<Method>): Collection<Int> {
        var list: Set<Int>? = null
        for (m in methods) {
            val p: Set<Int> = findUnusedParameters(m)
            list = if (list == null) {
                p
            } else {
                Sets.intersection(list, p)
            }
        }
        return list!!.sorted().asReversed()
    }

    fun removeParameter(
        group: ClassGroup,
        methods: List<Method>,
        signature: Signature,
        execution: Execution?,
        paramIndex: Int,
        lvtIndex: Int
    ) {
        val slots: Int = signature.getTypeOfArg(paramIndex).size
        for (cf in group.classes) {
            for (m in cf.methods) {
                val c: Code = m.code ?: continue
                for (i in c.instructions.instructions.toTypedArray()) {
                    if (i !is InvokeInstruction) {
                        continue
                    }
                    val ii: InvokeInstruction = i as InvokeInstruction
                    if (ii.methods.stream().noneMatch(methods::contains)) {
                        continue
                    }
                    ii.removeParameter(paramIndex) // remove parameter from instruction
                    val ics: Collection<InstructionContext> = invokes[i]
                    for (ins in ics) {
                        val pops: Int =
                            signature.size() - paramIndex - 1 // index from top of stack of parameter. 0 is the last parameter
                        val sctx: StackContext = ins.pops.get(pops)
                        if (sctx.getPushed().instruction.instructions == null) {
                            continue
                        }
                        ins.removeStack(pops) // remove parameter from stack
                    }
                }
            }
        }
        for (method in methods) {
            if (method.code != null) // adjust lvt indexes to get rid of idx in the method
            {
                for (ins in method.code.instructions.instructions) {
                    if (ins is LVTInstruction) {
                        val lins: LVTInstruction = ins as LVTInstruction
                        val i: Int = lins.variableIndex
                        assert(
                            i != lvtIndex // current unused variable detection just looks for no accesses
                        )

                        // reassign
                        if (i > lvtIndex) {
                            assert(i > 0)
                            assert(i >= lvtIndex + slots)
                            val newIns: Instruction = lins.setVariableIndex(i - slots)
                            assert(ins === newIns)
                        }
                    }
                }
            }
        }
        for (method in methods) {
            method.descriptor.remove(paramIndex)
        }
    }

    private var count = 0

    override fun run(group: ClassGroup) {
        val i: Int
        group.buildClassGraph()

        invokes.clear()
        buildUnused(group)

        val execution = Execution(group)
        execution.addExecutionVisitor { ictx: InstructionContext -> visit(ictx) }
        execution.populateInitialMethods()
        execution.run()

        i = processUnused(execution, group)
        count += i

        Logger.info("Removed $count unused method parameters.")
    }
}