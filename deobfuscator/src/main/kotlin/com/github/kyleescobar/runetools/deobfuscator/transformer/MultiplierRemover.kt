package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import com.github.kyleescobar.runetools.deobfuscator.asm.ClassPool
import com.github.kyleescobar.runetools.deobfuscator.asm.owner
import com.google.common.collect.MultimapBuilder
import me.coley.analysis.SimAnalyzer
import me.coley.analysis.SimFrame
import me.coley.analysis.SimInterpreter
import me.coley.analysis.TypeChecker
import me.coley.analysis.TypeResolver
import me.coley.analysis.util.TypeUtil
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.SourceInterpreter
import org.objectweb.asm.tree.analysis.SourceValue
import org.objectweb.asm.tree.analysis.Value
import org.tinylog.kotlin.Logger
import java.math.BigInteger

class MultiplierRemover : Transformer {

    private val multipliers = Multipliers()
    private val multiplierAnalyzer = Analyzer(MultiplierInterpreter(multipliers))

    override fun run(pool: ClassPool) {
        val analyzer = object : SimAnalyzer(SimInterpreter()) {
            override fun createTypeChecker() = TypeChecker { parent, child ->
                pool.inheritanceGraph.getAllParents(child.internalName).contains(parent.internalName)
            }

            override fun createTypeResolver() = object : TypeResolver {
                override fun common(type1: Type, type2: Type): Type {
                    val common = pool.inheritanceGraph.getCommon(type1.internalName, type2.internalName) ?: return TypeUtil.OBJECT_TYPE
                    return Type.getObjectType(common)
                }

                override fun commonException(type1: Type, type2: Type): Type {
                    val common = pool.inheritanceGraph.getCommon(type1.internalName, type2.internalName) ?: return TypeUtil.EXCEPTION_TYPE
                    return Type.getObjectType(common)
                }
            }
        }
        analyzer.setSkipDeadCodeBlocks(true)
        analyzer.setThrowUnresolvedAnalyzerErrors(false)

        pool.classes.filter { !it.name.contains("/") }.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions.toArray()
                val frames = analyzer.analyze(method.owner.name, method) as Array<SimFrame>
                print("")
            }
        }
    }

    private fun solveMultipliers(pool: ClassPool) {
        Logger.info("Finding and solving multipliers...")

        pool.classes.filter { !it.name.contains("bouncycastle") }.forEach { cls ->
            Logger.info("Analyzing class: ${cls.name} multipliers.")
            cls.methods.forEach methodLoop@ { method ->
                multiplierAnalyzer.analyze(cls.name, method)
            }
        }

        println()
    }

    /**
     * Holds the analyzed and solved multipliers info.
     */
    class Multipliers {
        val decoders = hashMapOf<String, Number>()
        val fieldMultipliers = MultimapBuilder.hashKeys().arrayListValues().build<String, Multiplier>()
        val fieldAssignments = hashSetOf<FieldMultiplierAssignment>()

        fun solve() {
            while(true) {

            }
        }

        private fun simplify() {
            val itr = fieldAssignments.iterator()
            for(entry in itr) {
                if(entry.setter in decoders) {
                    itr.remove()
                    val decoder = decoders.getValue(entry.setter)
                    val simplifiedDecoder = MulMath.multiply(decoder, entry.number)
                    if(MulMath.isMultiplier(simplifiedDecoder)) {
                        fieldMultipliers.put(entry.getter, Multiplier.Decoder(simplifiedDecoder))
                    }
                } else if(entry.getter in decoders) {
                    itr.remove()
                    val encoder = MulMath.invert(decoders.getValue(entry.getter))
                }
            }
        }
    }

    /**
     * Represents a multiplier number which can be associated to either
     * another multiplier or a field.
     */
    sealed class Multiplier(val number: Number) {
        class Encoder(number: Number) : Multiplier(number)
        class Decoder(number: Number) : Multiplier(number)
    }

    object MulMath {
        private val INT_SHIFT = BigInteger.ONE.shiftLeft(Int.SIZE_BITS)
        private val LONG_SHIFT = BigInteger.ONE.shiftLeft(Long.SIZE_BITS)

        fun invert(value: Int) = value.toBigInteger().modInverse(INT_SHIFT).toInt()

        fun invert(value: Long) = value.toBigInteger().modInverse(LONG_SHIFT).toLong()

        fun invert(value: Number): Number = when (value) {
            is Int -> invert(value)
            is Long -> invert(value)
            else -> throw IllegalArgumentException()
        }

        fun isInvertible(value: Int): Boolean = value and 1 == 1

        fun isInvertible(value: Long) = isInvertible(value.toInt())

        fun isInvertible(value: Number) = when (value) {
            is Int, is Long -> isInvertible(value.toInt())
            else -> throw IllegalArgumentException()
        }

        fun multiply(a: Number, b: Number): Number {
            return when (a) {
                is Int -> a.toInt() * b.toInt()
                is Long -> a.toLong() * b.toLong()
                else -> throw IllegalArgumentException()
            }
        }

        fun isMultiplier(value: Number) = isInvertible(value) && invert(value) != value
    }

    data class FieldMultiplier(val field: MulValue, val number: MulValue)
    data class FieldMultiplierAssignment(val setter: String, val getter: String, val number: Number)

    /**
     * ============ MULTIPLIER ANALYSIS =============
     */

    /**
     * The ASM analyzer interpreter for finding and parsing out the code expressions containing multipliers.
     */
    class MultiplierInterpreter(val multipliers: Multipliers) : Interpreter<MulValue>(ASM9) {

        private val interp = SourceInterpreter()

        private val mulValues = hashSetOf<MulValue>()
        private val distributedMulValues = hashSetOf<MulValue>()
        private val mulPuts = hashMapOf<MulValue, MulValue>()

        override fun newValue(type: Type?) = interp.newValue(type)?.let { MulValue(it) }

        override fun newOperation(insn: AbstractInsnNode) = MulValue(interp.newOperation(insn))

        override fun merge(value1: MulValue, value2: MulValue) = MulValue(interp.merge(value1.src, value2.src))

        override fun returnOperation(insn: AbstractInsnNode, value: MulValue, expected: MulValue) {}

        override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out MulValue>) =
            MulValue(interp.naryOperation(insn, values.map { it.src }))

        override fun ternaryOperation(
            insn: AbstractInsnNode,
            value1: MulValue,
            value2: MulValue,
            value3: MulValue
        ) = MulValue(interp.ternaryOperation(insn, value1.src, value2.src, value3.src))

        override fun binaryOperation(insn: AbstractInsnNode, value1: MulValue, value2: MulValue) = MulValue.MulExpr(
            interp.binaryOperation(insn, value1.src, value2.src),
            value1,
            value2
        ).also {
            when(insn.opcode) {
                IMUL, LMUL -> {
                    val fieldMultiplier = it.asFieldMultiplier() ?: return@also
                    if(mulValues.add(fieldMultiplier.number)) {
                        multipliers.fieldMultipliers.put(fieldMultiplier.field.getField(), Multiplier.Decoder(fieldMultiplier.number.getNumber()))
                    }
                }
                PUTFIELD -> setField(it, value2)
            }
        }

        override fun unaryOperation(insn: AbstractInsnNode, value: MulValue) = MulValue(interp.unaryOperation(insn, value.src)).also {
            if(insn.opcode == PUTSTATIC) setField(it, value)
        }

        override fun copyOperation(insn: AbstractInsnNode, value: MulValue) = when(insn.opcode) {
            DUP, DUP2, DUP2_X1, DUP_X1 -> value
            else -> MulValue(interp.copyOperation(insn, value.src))
        }

        private fun setField(field: MulValue, value: MulValue) {
            mulPuts[value] = field
            if(value.isNumber()) { /* Nothing */ }
            else if(value is MulValue.MulExpr) {
                distribute(field.src.insns.singleOrNull() as FieldInsnNode, value)
            }
        }

        private fun MulValue.MulExpr.asFieldMultiplier(): FieldMultiplier? {
            var number: MulValue? = null
            var field: MulValue? = null
            if(first.isNumber() && second.isFieldGetter()) {
                number = first
                field = second
            } else if(second.isNumber() && first.isFieldGetter()) {
                number = second
                field = first
            }
            if(number != null && field != null) {
                if(MulMath.isMultiplier(number.getNumber())) return FieldMultiplier(field, number)
            }
            return null
        }

        private fun distribute(field: FieldInsnNode, value: MulValue.MulExpr) {
            if(value.isMultiply()) {
                val fieldMul = value.asFieldMultiplier()
                if(fieldMul != null && distributedMulValues.add(fieldMul.number)) {
                    multipliers.fieldMultipliers.remove(fieldMul.field.getField(), Multiplier.Decoder(fieldMul.number.getNumber()))
                    multipliers.fieldAssignments.add(FieldMultiplierAssignment("${field.owner}.${field.name}", fieldMul.field.getField(), fieldMul.number.getNumber()))
                    return
                }
            }
            if(!value.isMultiply() && !value.isAddOrSub()) {
                return
            }
            val first = value.first
            val second = value.second

            var number: MulValue? = null
            var other: MulValue? = null
            if(first.isNumber()) {
                number = first
                other = second
            } else if(second.isNumber()) {
                number = second
                other = first
            }
            if(number != null && other != null) {
                val numValue = number.getNumber()
                if(MulMath.isMultiplier(numValue) && mulValues.add(number)) {
                    val getter = mulPuts[other]
                    if(getter == null) {
                        multipliers.fieldMultipliers.put("${field.owner}.${field.name}", Multiplier.Encoder(numValue))
                    } else {
                        multipliers.fieldAssignments.add(FieldMultiplierAssignment("${field.owner}.${field.name}", getter.getField(), numValue))
                    }
                }
                if(value.isMultiply()) return
            }
            if(first is MulValue.MulExpr) distribute(field, first)
            if(second is MulValue.MulExpr) distribute(field, second)
        }
    }

    /**
     * Represents a value on the stack which is related to a multiplier.
     */
    open class MulValue(val src: SourceValue) : Value {

        override fun getSize(): Int = src.size

        override fun hashCode(): Int = src.hashCode()

        override fun equals(other: Any?): Boolean = other is MulValue && src == other.src

        /**
         * Represents two values on the stack one or both being multipliers or another type related to it.
         */
        class MulExpr(src: SourceValue, val first: MulValue, val second: MulValue) : MulValue(src)

        fun isNumber() = src.insns.singleOrNull().let { it != null && it is LdcInsnNode && (it.cst is Int || it.cst is Long) }
        fun isFieldGetter() = src.insns.singleOrNull().let { it != null && (it.opcode == GETSTATIC || it.opcode == GETFIELD) }
        fun getNumber() = src.insns.single().let { it as LdcInsnNode; it.cst as Number }
        fun getField() = src.insns.single().let { it as FieldInsnNode; return@let "${it.owner}.${it.name}" }
        fun isMultiply() = src.insns.singleOrNull().let { it != null && (it.opcode == IMUL || it.opcode == LMUL) }
        fun isAddOrSub() = src.insns.singleOrNull().let { it != null && (it.opcode == IADD || it.opcode == LADD || it.opcode == ISUB || it.opcode == LSUB) }
    }

    /*
     * Below are just utility extension funcs
     */
}