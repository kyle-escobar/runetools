package dev.kyleescobar.runetools.asm

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes.ASM9
import java.util.TreeMap

class AnnotationNode(val type: Type, private val visible: Boolean) : AnnotationVisitor(ASM9) {

    private val data = TreeMap<String, Any>()

    constructor(type: Type) : this(type, true)

    constructor(type: Type, value: Any) : this(type) {

    }

    fun addData(name: String, value: Any) {
        if(data.containsKey(name)) throw IllegalArgumentException("Annotation already contains value name: $name.")
        data[name] = value
    }

    fun setData(name: String, value: Any) {
        data[name] = value
    }

    fun setData(value: Any) {
        setData("value", value)
    }

    fun removeData(name: String): Any? = data.remove(name)

    operator fun get(name: String): Any? = data[name]

    fun getValue(): Any? = data["value"]

    override fun visit(name: String, value: Any) {
        data[name] = value
    }

    override fun visitAnnotation(name: String, descriptor: String): AnnotationVisitor {
        val annotation = AnnotationNode(Type(descriptor))
        data[name] = annotation
        return annotation
    }

    override fun visitArray(name: String): AnnotationVisitor {
        val values = mutableListOf<Any>()
        data[name] = values
        return object : AnnotationVisitor(ASM9) {
            override fun visit(name: String, value: Any) {
                values.add(value)
            }

            override fun visitAnnotation(name: String, descriptor: String): AnnotationVisitor {
                val annotation = AnnotationNode(Type(descriptor))
                values.add(annotation)
                return annotation
            }
        }
    }

    fun accept(visitor: AnnotationVisitor) {
        data.entries.forEach { entry ->
            accept(visitor, entry.key, entry.value)
        }
        visitor.visitEnd()
    }

    fun accept(visitor: AnnotationVisitor, name: String?, value: Any?) {
        if(value is AnnotationNode) {
            value.accept(visitor.visitAnnotation(name, value.type.toString()))
        } else if(value is List<*>) {
            val arrayVisitor = visitor.visitArray(name)
            value.forEach {
                accept(arrayVisitor, null, it)
            }
            arrayVisitor.visitEnd()
        } else {
            visitor.visit(name, value)
        }
    }
}