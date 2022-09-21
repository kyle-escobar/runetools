package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.signature.util.VirtualMethods
import dev.kyleescobar.runetools.deob.util.NameMappings
import dev.kyleescobar.runetools.deobfuscator.Deobfuscator
import dev.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.runetools.deobfuscator.ext.filterClientClasses
import org.tinylog.kotlin.Logger

class Renamer : Transformer {

    private var classCount = 0
    private var methodCount = 0
    private var fieldCount = 0

    private val mappings = NameMappings()

    override fun run(group: ClassGroup) {
        generateNameMappings(group)
        applyNameMappings(group)

        Logger.info("Renamed $classCount classes.")
        Logger.info("Renamed $methodCount methods.")
        Logger.info("Renamed $fieldCount fields.")
    }

    private fun generateNameMappings(group: ClassGroup) {
        Logger.info("Generating name mappings.")

        // Build class group hierarchy
        group.buildClassGraph()
        group.lookup()

        // Generate class name mappings
        group.classes.filterClientClasses().forEach { cls ->
            if(Deobfuscator.isObfuscatedName(cls.name)) {
                mappings.map(cls.poolClass, "class${++classCount}")
            }
        }
        mappings.classes = classCount

        // Generate Field name mappings
        group.classes.filterClientClasses().forEach { cls ->
            cls.fields.forEach { field ->
                if(Deobfuscator.isObfuscatedName(field.name)) {
                    mappings.map(field.poolField, "field${++fieldCount}")
                }
            }
        }
        mappings.fields = fieldCount

        // Generate method name mappings
        group.classes.filterClientClasses().forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                if(!Deobfuscator.isObfuscatedName(method.name)) {
                    return@methodLoop
                }

                val name = "method${++methodCount}"
                VirtualMethods.getVirtualMethods(method).forEach { virtualMethod ->
                    mappings.map(virtualMethod.poolMethod, name)
                }
            }
        }
        mappings.methods = methodCount
    }

    private fun applyNameMappings(group: ClassGroup) {
        Logger.info("Applying name mappings.")
        val renamer = dev.kyleescobar.runetools.deob.deobfuscators.Renamer(mappings)
        renamer.run(group)
    }
}