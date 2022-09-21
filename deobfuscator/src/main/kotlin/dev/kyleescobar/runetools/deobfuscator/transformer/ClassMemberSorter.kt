package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassFile
import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.Field
import dev.kyleescobar.runetools.asm.Method
import dev.kyleescobar.runetools.asm.attributes.Annotated
import dev.kyleescobar.runetools.asm.execution.Execution
import dev.kyleescobar.runetools.deob.DeobAnnotations
import dev.kyleescobar.runetools.deobfuscator.Transformer
import org.tinylog.kotlin.Logger

class ClassMemberSorter : Transformer {

    private lateinit var execution: Execution
    private val nameIndices = hashMapOf<String, Int>()

    override fun run(group: ClassGroup) {
        execution = Execution(group)
        execution.staticStep = true
        execution.populateInitialMethods()
        execution.run()

        for (i in 0 until group.classes.size) {
            val cf: ClassFile = group.classes[i]
            val className = DeobAnnotations.getObfuscatedName(cf) ?: cf.className
            nameIndices[className] = i
        }
        var sortedMethods = 0
        var sortedFields = 0
        for (cf in group.classes) {
            val m = cf.methods
            m.sortWith(this::compare)
            sortedMethods += m.size

            // field order of enums is mostly handled in EnumDeobfuscator
            if (!cf.isEnum) {
                val f = cf.fields
                f.sortWith(this::compare)
                sortedFields += f.size
            }
        }

        Logger.info("Sorted $sortedMethods methods.")
        Logger.info("Sorted $sortedFields fields.")
    }

    // static fields, member fields, clinit, init, methods, static methods
    private fun compare(a: Annotated, b: Annotated): Int {
        val i1: Int = getType(a)
        val i2: Int = getType(b)
        if (i1 != i2) {
            return i1.compareTo(i2)
        }
        val nameIdx1 = getNameIdx(a)
        val nameIdx2 = getNameIdx(b)
        return if (nameIdx1 != nameIdx2) {
            nameIdx1.compareTo(nameIdx2)
        } else compareOrder(a, b)
    }

    private fun getNameIdx(annotations: Annotated): Int {
        val name: String = DeobAnnotations.getObfuscatedName(annotations) ?: ""
        val nameIdx = nameIndices[name]
        return nameIdx ?: -1
    }

    private fun getType(a: Annotated): Int {
        if (a is Method) return getType(a) else if (a is Field) return getType(a)
        throw RuntimeException("kys")
    }

    private fun getType(m: Method): Int {
        if (m.name.equals("<clinit>")) {
            return 1
        }
        if (m.name.equals("<init>")) {
            return 2
        }
        return if (!m.isStatic) {
            3
        } else 4
    }

    private fun getType(f: Field): Int {
        return if (f.isStatic) {
            1
        } else 2
    }

    private fun compareOrder(o1: Any, o2: Any): Int {
        var i1 = execution.getOrder(o1)
        var i2 = execution.getOrder(o2)

        if (i1 == null) {
            i1 = Int.MAX_VALUE
        }
        if (i2 == null) {
            i2 = Int.MAX_VALUE
        }
        if (i1 != i2) {
            return Integer.compare(i1, i2)
        }

        // Fall back to number of accesses
        i1 = execution.getAccesses(o1)
        i2 = execution.getAccesses(o2)

        if (i1 == null) {
            i1 = Int.MAX_VALUE
        }
        if (i2 == null) {
            i2 = Int.MAX_VALUE
        }

        return Integer.compare(i1, i2)
    }
}