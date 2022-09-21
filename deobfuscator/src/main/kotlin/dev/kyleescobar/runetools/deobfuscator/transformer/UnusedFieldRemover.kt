package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.Field
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.FieldInstruction
import dev.kyleescobar.runetools.deobfuscator.Transformer
import org.tinylog.kotlin.Logger

class UnusedFieldRemover : Transformer {

    private var count = 0

    private val usedFields = hashSetOf<Field>()

    override fun run(group: ClassGroup) {
        findUsedFields(group)

        group.classes.forEach { cls ->
            val fields = cls.fields.toTypedArray()
            for(field in fields) {
                if(!usedFields.contains(field)) {
                    cls.removeField(field)
                    count++
                }
            }
        }

        Logger.info("Removed $count unused fields.")
    }

    private fun findUsedFields(group: ClassGroup) {
        group.classes.forEach classLoop@ { cls ->
            cls.methods.forEach methodLoop@ { method ->
                val code = method.code ?: return@methodLoop
                for(insn in code.instructions.instructions) {
                    if(insn is FieldInstruction) {
                        usedFields.add(insn.myField)
                    }
                }
            }
        }
    }
}