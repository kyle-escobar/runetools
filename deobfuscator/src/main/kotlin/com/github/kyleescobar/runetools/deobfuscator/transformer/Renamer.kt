package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import com.github.kyleescobar.runetools.deobfuscator.asm.ClassPool
import com.github.kyleescobar.runetools.deobfuscator.asm.identifier
import com.github.kyleescobar.runetools.deobfuscator.asm.owner
import com.github.kyleescobar.runetools.deobfuscator.util.isObfuscatedName
import com.google.common.collect.MultimapBuilder
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.tinylog.kotlin.Logger

class Renamer : Transformer {

    private var classCount = 0
    private var methodCount = 0
    private var fieldCount = 0

    private val mappings = hashMapOf<String, String>()

    override fun run(pool: ClassPool) {
        generateMappings(pool)
        applyMappings(pool)

        Logger.info("Renamed $classCount classes.")
        Logger.info("Renamed $methodCount methods.")
        Logger.info("Renamed $fieldCount fields.")
    }

    private fun generateMappings(pool: ClassPool) {
        Logger.info("Generating name mappings.")

        pool.classes.filter { it.name.isObfuscatedName() }.forEach { cls ->
            val name = "class${++classCount}"
            mappings[cls.identifier] = name
        }

        pool.classes.forEach { cls ->
            cls.methods.filter { it.name.isObfuscatedName() }.forEach methodLoop@ { method ->
                if(mappings.containsKey(method.identifier)) return@methodLoop
                val name = "method${++methodCount}"
                mappings[method.identifier] = name
                pool.inheritanceGraph.getAllChildren(cls.name).mapNotNull { pool.getClass(it) }.forEach { child ->
                    mappings["${child.identifier}.${method.name}${method.desc}"] = name
                }
            }
        }

        pool.classes.forEach { cls ->
            cls.fields.filter { it.name.isObfuscatedName() }.forEach fieldLoop@ { field ->
                if(mappings.containsKey(field.identifier)) return@fieldLoop
                val name = "field${++fieldCount}"
                mappings[field.identifier] = name
                pool.inheritanceGraph.getAllChildren(cls.name).mapNotNull { pool.getClass(it) }.forEach { child ->
                    mappings["${child.identifier}.${field.name}"] = name
                }
            }
        }
    }

    private fun applyMappings(pool: ClassPool) {
        Logger.info("Applying name mappings.")

        val newNodes = mutableListOf<ClassNode>()
        val remapper = SimpleRemapper(mappings)

        pool.classes.forEach { cls ->
            val node = ClassNode()
            cls.accept(ClassRemapper(node, remapper))
            newNodes.add(node)
        }

        pool.clear()
        newNodes.forEach { pool.addClass(it) }
        pool.loadHierarchy()
    }
}