package dev.kyleescobar.runetools.deobfuscator.transformer

import com.google.common.base.Objects
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.Method
import dev.kyleescobar.runetools.asm.attributes.code.Instruction
import dev.kyleescobar.runetools.asm.attributes.code.InstructionType
import dev.kyleescobar.runetools.asm.attributes.code.InstructionType.*
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.*
import dev.kyleescobar.runetools.asm.attributes.code.instructions.Goto
import dev.kyleescobar.runetools.asm.attributes.code.instructions.If
import dev.kyleescobar.runetools.asm.attributes.code.instructions.If0
import dev.kyleescobar.runetools.asm.execution.Execution
import dev.kyleescobar.runetools.asm.execution.InstructionContext
import dev.kyleescobar.runetools.asm.execution.MethodContext
import dev.kyleescobar.runetools.asm.execution.StackContext
import dev.kyleescobar.runetools.deobfuscator.Transformer
import org.tinylog.kotlin.Logger
import java.util.*

class DeadParameterRemover : Transformer {

    private val parameters: MutableMap<ConstantMethodParameter, ConstantMethodParameter> = HashMap()
    private val mparams: Multimap<Method, ConstantMethodParameter?> = HashMultimap.create()

    private fun checkMethodsAreConsistent(methods: List<Method>) {
        var prev: Method? = null
        for (m in methods) {
            if (prev != null) {
                assert(prev.descriptor.equals(m.descriptor))
                assert(prev.isStatic == m.isStatic)
            }
            prev = m
        }
    }

    private fun getCMPFor(methods: List<Method>, paramIndex: Int, lvtIndex: Int): ConstantMethodParameter {
        val cmp = ConstantMethodParameter()
        cmp.methods = methods
        cmp.paramIndex = paramIndex
        cmp.lvtIndex = lvtIndex
        val exists = parameters[cmp]
        if (exists != null) {
            // existing mparams for each method of methods can have different 'methods'
            // due to invokespecial on virtual methods.
            return exists
        }
        parameters[cmp] = cmp
        for (m in methods) {
            mparams.put(m, cmp)
        }
        return cmp
    }

    // find constant values passed as parameters
    private fun findConstantParameter(methods: List<Method>, invokeCtx: InstructionContext) {
        checkMethodsAreConsistent(methods)
        val method = methods[0] // all methods must have the same signature etc
        val offset = if (method.isStatic) 0 else 1
        val pops = invokeCtx.pops

        // object is popped first, then param 1, 2, 3, etc. double and long take two slots.
        var lvtOffset = offset
        var parameterIndex = 0
        while (parameterIndex < method.descriptor.size()) {

            // get(0) == first thing popped which is the last parameter,
            // get(descriptor.size() - 1) == first parameter
            val ctx = pops[method.descriptor.size() - 1 - parameterIndex]
            val cmp = getCMPFor(methods, parameterIndex, lvtOffset)
            if (cmp.invalid) {
                lvtOffset += method.descriptor.getTypeOfArg(parameterIndex++).size
                continue
            }
            if (ctx.getPushed().instruction is PushConstantInstruction) {
                val pc = ctx.getPushed().instruction as PushConstantInstruction
                if (pc.constant !is Number) {
                    cmp.invalid = true
                    lvtOffset += method.descriptor.getTypeOfArg(parameterIndex++).size
                    continue
                }
                val number = pc.constant as Number
                if (!cmp.values.contains(number)) {
                    cmp.values.add((pc.constant as Number))
                }
            } else {
                cmp.invalid = true
            }
            lvtOffset += method.descriptor.getTypeOfArg(parameterIndex++).size
        }
    }

    // find constant valuess passed to parameters
    private fun findParameters(ins: InstructionContext) {
        if (ins.instruction !is InvokeInstruction) {
            return
        }
        val methods = (ins.instruction as InvokeInstruction).methods
        if (methods.isEmpty()) {
            return
        }
        findConstantParameter(methods, ins)
    }

    private fun findParametersForMethod(m: Method): List<ConstantMethodParameter?> {
        val c = mparams[m] ?: return emptyList()
        return c.toList()
    }

    // compare known values against a jump. also invalidates constant param
    // if lvt is reassigned or a comparison is made against a non constant
    private fun findDeadParameters(mctx: MethodContext) {
        mctx.instructionContexts.forEach(this::findDeadParameters)
    }

    private fun findDeadParameters(ins: InstructionContext) {
        val parameters = findParametersForMethod(ins.frame.method)
        for (parameter in parameters) {
            val lvtIndex = parameter!!.lvtIndex
            if (parameter.invalid) {
                continue
            }
            if (ins.instruction is LVTInstruction) {
                val lvt = ins.instruction as LVTInstruction
                if (lvt.variableIndex != lvtIndex) {
                    continue
                }
                if (lvt.store() || ins.instruction.type === InstructionType.IINC) {
                    parameter.invalid = true
                    continue  // value changes at some point, parameter is used
                }
                assert(ins.pushes.size == 1)
                val sctx = ins.pushes[0]
                if (sctx.popped.size != 1
                    || sctx.popped[0].instruction !is ComparisonInstruction
                ) {
                    parameter.invalid = true
                    continue
                }
            }
            if (ins.instruction !is ComparisonInstruction) {
                continue
            }

            // assume that this will always be variable index #paramIndex comp with a constant.
            val comp = ins.instruction as ComparisonInstruction
            var one: StackContext
            var two: StackContext? = null
            if (comp is If0) {
                one = ins.pops[0]
            } else if (comp is If) {
                one = ins.pops[0]
                two = ins.pops[1]
            } else {
                throw RuntimeException("Unknown comp ins")
            }

            // find if one is a lvt ins
            var lvt: LVTInstruction? = null
            var other: StackContext? = null
            if (one.getPushed().instruction is LVTInstruction) {
                lvt = one.getPushed().instruction as LVTInstruction
                other = two
            } else if (two != null && two.getPushed().instruction is LVTInstruction) {
                lvt = two.getPushed().instruction as LVTInstruction
                other = one
            }
            assert(lvt == null || !lvt.store())
            if (lvt == null || lvt.variableIndex !== lvtIndex) {
                continue
            }
            var otherValue: Number? = null
            if (two != null) // two is null for if0
            {
                if (other!!.getPushed().instruction !is PushConstantInstruction) {
                    parameter.invalid = true
                    continue
                }
                val pc = other.getPushed().instruction as PushConstantInstruction
                otherValue = pc.constant as Number
            }
            for (value in parameter.values) {
                // the result of the comparison doesn't matter, only that it always goes the same direction for every invocation
                val result = doLogicalComparison(value, comp, otherValue)

                // XXX this should check that the particular if always takes the same path,
                // not that all ifs for a specific parameter always take the same path
                if (parameter.result != null && parameter.result !== result) {
                    parameter.invalid = true
                } else {
                    parameter.operations.add(ins.instruction)
                    parameter.result = result
                }
            }
        }
    }

    private fun doLogicalComparison(value: Number, comparison: ComparisonInstruction, otherValue: Number?): Boolean {
        val ins = comparison as Instruction
        assert(comparison is If0 == (otherValue == null))
        return when (ins.type) {
            IFEQ -> value == 0
            IFNE -> value != 0
            IFLT -> value.toInt() < 0
            IFGE -> value.toInt() >= 0
            IFGT -> value.toInt() > 0
            IFLE -> value.toInt() <= 0
            IF_ICMPEQ -> value == otherValue
            IF_ICMPNE -> value != otherValue
            IF_ICMPLT -> value.toInt() < otherValue!!.toInt()
            IF_ICMPGE -> value.toInt() >= otherValue!!.toInt()
            IF_ICMPGT -> value.toInt() > otherValue!!.toInt()
            IF_ICMPLE -> value.toInt() <= otherValue!!.toInt()
            else -> throw RuntimeException("Unknown constant comparison instructuction")
        }
    }

    // remove logically dead comparisons
    private fun removeDeadOperations(mctx: MethodContext): Int {
        var count = 0
        for (cmp in parameters.values) {
            if (cmp.invalid) {
                continue
            }
            if (!cmp.methods.contains(mctx.method)) {
                continue
            }

            for (ins in cmp.operations)  // comparisons
            {
                if (ins.instructions == null || ins.instructions.code.method !== mctx.method) {
                    continue
                }
                val ctx: InstructionContext = mctx.getInstructonContexts(ins).toTypedArray()[0]
                val branch = cmp.result!! // branch that is always taken
                if (ins.instructions == null) {
                    continue  // ins already removed?
                }
                val instructions = ins.instructions

                // remove the if
                if (ctx.instruction is If) {
                    ctx.removeStack(1)
                }
                ctx.removeStack(0)
                val idx = instructions.instructions.indexOf(ins)
                if (idx == -1) {
                    continue  // already removed?
                }
                ++count
                var to: Instruction
                to = if (branch) {
                    val jumpIns = ins as JumpingInstruction
                    assert(jumpIns.jumps.size == 1)
                    jumpIns.jumps[0]
                } else {
                    // just go to next instruction
                    instructions.instructions[idx + 1]
                }
                assert(to.instructions === instructions)
                assert(ins !== to)
                assert(instructions.instructions.contains(to))
                instructions.remove(ins)
                assert(instructions.instructions.contains(to))
                if (branch) {
                    val gotoins = Goto(instructions, instructions.createLabelFor(to))

                    // insert goto
                    instructions.instructions.add(idx, gotoins)
                }
            }
        }
        return count
    }

    private var count = 0

    override fun run(group: ClassGroup) {
        var execution = Execution(group)
        execution.addExecutionVisitor { ins: InstructionContext -> findParameters(ins) }
        execution.populateInitialMethods()
        execution.run()

        execution = Execution(group)
        execution.addMethodContextVisitor(this::findDeadParameters)
        execution.populateInitialMethods()
        execution.run()

        execution = Execution(group)
        execution.addMethodContextVisitor { m -> count += removeDeadOperations(m) }
        execution.populateInitialMethods()
        execution.run()

        Logger.info("Removed $count dead method parameters.")
    }


    private class ConstantMethodParameter {
        lateinit var methods: List<Method>
        var paramIndex = 0
        var lvtIndex = 0

        val values = mutableListOf<Number>()
        val operations = mutableListOf<Instruction>()

        var result: Boolean? = null
        var invalid = false

        override fun hashCode(): Int {
            var h = 3
            h = 47 * h + Objects.hashCode(methods)
            h = 47 * h + lvtIndex
            return h
        }

        override fun equals(other: Any?): Boolean {
            if(other == null) return false
            if(this::class != other::class) return false
            other as ConstantMethodParameter
            if(!Objects.equal(methods, other.methods)) return false
            if(lvtIndex != other.lvtIndex) return false
            return true
        }
    }
}