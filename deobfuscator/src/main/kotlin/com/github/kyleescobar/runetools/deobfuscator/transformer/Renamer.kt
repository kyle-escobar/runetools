package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.asm.ClassPool
import com.github.kyleescobar.runetools.deobfuscator.Transformer
import me.coley.analysis.util.InheritanceGraph
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.tinylog.kotlin.Logger

class Renamer : Transformer {

    private var classCount = 0
    private var methodCount = 0
    private var fieldCount = 0

    private val mappings = mutableMapOf<String, String>()

    override fun run(pool: ClassPool) {
        generateMappings(pool)
        applyMappings(pool)

        Logger.info("Renamed [classes: $classCount, methods: $methodCount, fields: $fieldCount].")
    }

    private fun generateMappings(pool: ClassPool) {
        Logger.info("Generating name mappings.")

        val inheritanceGraph = InheritanceGraph()
        inheritanceGraph.addClasspath()
        inheritanceGraph.addModulePath()

        pool.classes.forEach {
            val parents = mutableListOf<String>()
            parents.add(it.superName)
            parents.addAll(it.interfaces)
            inheritanceGraph.add(it.name, parents)
        }

        /*
         * Generate class mappings
         */
        pool.classes.filter { it.name.length <= 2 }.forEach { cls ->
            val newName = "class${++classCount}"
            mappings[cls.name] = newName
        }

        /*
         * Generate method mappings
         */
        pool.classes.forEach { cls ->
            cls.methods.filter { it.name.length <= 2 }.forEach methodLoop@ { method ->
                if(mappings.containsKey("${cls.name}.${method.name}${method.desc}")) return@methodLoop

                val newName = "method${++methodCount}"
                mappings["${cls.name}.${method.name}${method.desc}"] = newName

                inheritanceGraph.getAllChildren(cls.name).forEach { child ->
                    mappings["$child.${method.name}${method.desc}"] = newName
                }
            }
        }

        /*
         * Generate field mappings
         */
        pool.classes.forEach { cls ->
            cls.fields.filter { it.name.length <= 2 }.forEach fieldLoop@ { field ->
                if(mappings.containsKey("${cls.name}.${field.name}")) return@fieldLoop

                val newName = "field${++fieldCount}"
                mappings["${cls.name}.${field.name}"] = newName

                inheritanceGraph.getAllChildren(cls.name).forEach { child ->
                    mappings["$child.${field.name}"] = newName
                }
            }
        }
    }

    private fun applyMappings(pool: ClassPool) {
        Logger.info("Applying name mappings.")

        val newNodes = mutableListOf<ClassNode>()
        val remapper = SimpleRemapper(mappings)

        pool.classes.forEach { cls ->
            val newNode = ClassNode()
            cls.accept(ClassRemapper(newNode, remapper))
            newNodes.add(newNode)
        }

        pool.clear()
        newNodes.forEach { pool.addClass(it) }
    }
}