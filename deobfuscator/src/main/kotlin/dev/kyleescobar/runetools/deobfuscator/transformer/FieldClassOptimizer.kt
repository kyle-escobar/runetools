package dev.kyleescobar.runetools.deobfuscator.transformer

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.Field
import dev.kyleescobar.runetools.asm.Method
import dev.kyleescobar.runetools.asm.Type
import dev.kyleescobar.runetools.asm.attributes.Code
import dev.kyleescobar.runetools.asm.attributes.code.Instruction
import dev.kyleescobar.runetools.asm.attributes.code.Instructions
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.FieldInstruction
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.GetFieldInstruction
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.PushConstantInstruction
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.SetFieldInstruction
import dev.kyleescobar.runetools.asm.attributes.code.instructions.LDC
import dev.kyleescobar.runetools.deobfuscator.Transformer
import org.tinylog.kotlin.Logger

class FieldClassOptimizer : Transformer {

    private lateinit var group: ClassGroup

    private val fieldInstructions: Multimap<Field, FieldInstruction> = HashMultimap.create<Field, FieldInstruction>()
    private val fields = mutableListOf<Field>()

    private fun findFieldIns() {
        for (cf in group.classes) {
            for (m in cf.methods) {
                val code: Code = m.code ?: continue
                for (i in code.instructions.instructions) {
                    if (i !is FieldInstruction) continue
                    val sf: FieldInstruction = i
                    if (sf.myField == null) continue
                    fieldInstructions.put(sf.myField, sf)
                }
            }
        }
    }

    private fun makeConstantValues() {
        for (cf in group.classes) {
            for (f in cf.fields) {
                if (!f.isStatic || !f.type.equals(Type.STRING)) continue

                var constantValue = f.value
                if (constantValue != null) continue

                val sfis = fieldInstructions[f].filterIsInstance<SetFieldInstruction>().toMutableList()
                if (sfis.size != 1) continue

                val sfi: SetFieldInstruction = sfis[0]
                val ins: Instruction = sfi as Instruction
                val mOfSet: Method = ins.instructions.code.method
                if (!mOfSet.name.equals("<clinit>")) continue

                // get prev instruction and change to a constant value
                val instructions: Instructions = mOfSet.code.instructions
                val idx: Int = instructions.instructions.indexOf(ins)
                assert(idx != -1)

                val prev: Instruction = instructions.instructions[idx - 1]
                if(prev !is PushConstantInstruction) continue
                val pci: PushConstantInstruction = prev

                constantValue = pci.constant
                f.value = constantValue
                fields.add(f)

                var b: Boolean = instructions.instructions.remove(prev)
                assert(b)

                b = instructions.instructions.remove(ins)
                assert(b)
            }
        }
    }

    fun inlineUse(): Int {
        var count = 0
        for (f in fields) {
            // replace getfield with constant push
            val fins = fieldInstructions[f].filterIsInstance<GetFieldInstruction>().toMutableList()
            val value: Any = f.value
            for (fin in fins) {
                // remove fin, add push constant
                val i: Instruction = fin as Instruction
                val pushIns: Instruction = LDC(i.instructions, value)
                val instructions = i.instructions.instructions
                val idx = instructions.indexOf(i)
                assert(idx != -1)
                i.instructions.remove(i)
                instructions.add(idx, pushIns)
                ++count
            }
            f.classFile.removeField(f)
        }
        return count
    }

    override fun run(group: ClassGroup) {
        this.group = group

        findFieldIns()
        makeConstantValues()

        Logger.info("Moved ${inlineUse()} fields to better optimized classes.")
    }

}