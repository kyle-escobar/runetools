package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.Annotation
import dev.kyleescobar.runetools.asm.ClassFile
import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.Field
import dev.kyleescobar.runetools.asm.Method
import dev.kyleescobar.runetools.deob.DeobAnnotations
import dev.kyleescobar.runetools.deobfuscator.Transformer
import org.tinylog.kotlin.Logger

class AnnotationRemover : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        group.classes.forEach { cls ->
            cls.getDeobAnnotations().forEach {
                cls.annotations.remove(it.type)
                count++
            }

            cls.methods.forEach { method ->
                method.getDeobAnnotations().forEach {
                    method.annotations.remove(it.type)
                    count++
                }
            }

            cls.fields.forEach { field ->
                field.getDeobAnnotations().forEach {
                    field.annotations.remove(it.type)
                    count++
                }
            }
        }

        Logger.info("Removed $count added annotations.")
    }

    private fun ClassFile.getDeobAnnotations(): List<Annotation> {
        val ret = mutableListOf<Annotation>()
        this.findAnnotation(DeobAnnotations.OBFUSCATED_NAME)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.OBFUSCATED_SIGNATURE)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.OBFUSCATED_GETTER)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.EXPORT)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.IMPLEMENTS)?.also { ret.add(it) }
        return ret
    }

    private fun Method.getDeobAnnotations(): List<Annotation> {
        val ret = mutableListOf<Annotation>()
        this.findAnnotation(DeobAnnotations.OBFUSCATED_NAME)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.OBFUSCATED_SIGNATURE)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.OBFUSCATED_GETTER)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.EXPORT)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.IMPLEMENTS)?.also { ret.add(it) }
        return ret
    }

    private fun Field.getDeobAnnotations(): List<Annotation> {
        val ret = mutableListOf<Annotation>()
        this.findAnnotation(DeobAnnotations.OBFUSCATED_NAME)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.OBFUSCATED_SIGNATURE)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.OBFUSCATED_GETTER)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.EXPORT)?.also { ret.add(it) }
        this.findAnnotation(DeobAnnotations.IMPLEMENTS)?.also { ret.add(it) }
        return ret
    }
}