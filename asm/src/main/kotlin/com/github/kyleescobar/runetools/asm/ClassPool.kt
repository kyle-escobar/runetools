package com.github.kyleescobar.runetools.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths

class ClassPool {

    private val classMap = hashMapOf<String, ClassNode>()

    val classes get() = classMap.values.toList()

    fun addClass(entry: ClassNode, isSourceClass: Boolean = true) {
        if(!classMap.containsKey(entry.name)) {
            classMap[entry.name] = entry
            entry.isSourceClass = isSourceClass
            entry.init(this)
        }
    }

    fun addClass(name: String): ClassNode {
        if(!classMap.containsKey(name)) {
            val uri = ClassLoader.getSystemResource("$name.class").toURI()
            val file = Paths.get(uri).takeUnless { uri.scheme == "jrt" && !Files.exists(it) }?.let {
                Paths.get(URI(uri.scheme, uri.userInfo, uri.host, uri.port, "/modules${uri.path}", uri.query, uri.fragment))
            }

            if(file != null) {
                val reader = ClassReader(Files.readAllBytes(file))
                val node = ClassNode()
                reader.accept(node, ClassReader.EXPAND_FRAMES)
                addClass(node, isSourceClass = false)
                return node
            }
        }

        throw IllegalStateException("Failed to load JVM class: $name.")
    }

    fun removeClass(name: String) {
        if(classMap.containsKey(name)) {
            classMap.remove(name)
        }
    }

    fun getClass(name: String): ClassNode? = classMap[name]
    fun getOrCreateClass(name: String): ClassNode = getClass(name) ?: addClass(name)

    /**
     * Loads / Reloads all of the classes in this pool member additional info.
     * This needs to be called after all classes have been added to the pool.
     * Additionally if classes are removed / added after this needs to be called again.
     *
     */
    fun loadInfo() {
        // Initialize all classes tree
        classes.forEach { cls ->
            cls.init(this)
        }
    }

    companion object {
        val EMPTY = ClassPool()
    }
}