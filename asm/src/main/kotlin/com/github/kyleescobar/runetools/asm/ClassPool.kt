package com.github.kyleescobar.runetools.asm

import com.github.kyleescobar.runetools.asm.util.AsmUtils
import com.github.kyleescobar.runetools.asm.util.JarUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import java.io.File
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
            val url = ClassLoader.getSystemResource("$name.class")
            val uri = url.toURI()
            var file = Paths.get(uri)
            if(uri.scheme == "jrt" && !Files.exists(file)) {
                file = Paths.get(URI(uri.scheme, uri.userInfo, uri.host, uri.port, "/modules".plus(uri.path), uri.query, uri.fragment))
            }

            if(file != null) {
                val reader = ClassReader(Files.readAllBytes(file))
                val node = ClassNode()
                reader.accept(node, ClassReader.EXPAND_FRAMES)
                addClass(node, isSourceClass = false)
                return node
            }
        }
        val node = ClassNode()
        node.visit(50, 33, name, null, "java/lang/Object", emptyArray())
        addClass(node, isSourceClass = false)
        return node
    }

    fun removeClass(name: String) {
        if(classMap.containsKey(name)) {
            classMap.remove(name)
        }
    }

    fun addJarClasses(file: File) {
        JarUtils.readJarFile(file).forEach { addClass(it) }
        loadInfo()
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

        // ===== STEP A =====
        classes.forEach { cls ->
            val strings = hashSetOf<String>()
            cls.methods.forEach { method ->
                AsmUtils.extractStrings(method.instructions, strings)
            }
            cls.fields.forEach { field ->
                if(field.value is String) {
                    strings.add(field.value as String)
                }
            }
            cls.strings.addAll(strings)

            if(cls.superName != null && cls.parent == null) {
                cls.parent = getOrCreateClass(cls.superName)
                cls.parent?.children?.add(cls)
            }

            cls.interfaces.forEach {
                val interf = getOrCreateClass(it)
                if(cls.interfaceClasses.add(interf)) interf.implementers.add(cls)
            }
        }

        // ===== STEP B =====
        classes.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions.iterator()
                while(insns.hasNext()) {
                    val insn = insns.next()
                    when(insn) {
                        is MethodInsnNode -> {
                            val dsts = getOrCreateClass(insn.owner).resolveMethods(insn.name, insn.desc)
                            dsts.forEach { dst ->
                                dst.refsIn.add(method)
                                method.refsOut.add(dst)
                                dst.owner.methodTypeRefs.add(method)
                                method.classRefs.add(dst.owner)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        val EMPTY = ClassPool()
    }
}