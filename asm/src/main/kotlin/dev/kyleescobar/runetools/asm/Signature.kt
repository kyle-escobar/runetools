package dev.kyleescobar.runetools.asm

import dev.kyleescobar.runetools.util.ext.copyOf

data class Signature(val argumentTypes: List<Type>, val returnType: Type) {

    constructor(desc: String) : this(parseDescArgumentTypes(desc), parseDescReturnType(desc))

    constructor(other: Signature) : this(other.argumentTypes.copyOf().toList(), other.returnType)

    override fun toString(): String {
        val str = StringBuilder()
        str.append('(')
        argumentTypes.forEach {
            str.append(it)
        }
        str.append(')')
        str.append(returnType)
        return str.toString()
    }

    override fun hashCode(): Int {
        return this.toString().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if(other !is Signature) return false
        return this.toString() == other.toString()
    }

    companion object {

        private fun parseDescReturnType(desc: String): Type {
            val idx = desc.indexOf(')')
            if(idx == -1) throw IllegalArgumentException("Descriptor has no return type.")
            return Type(desc.substring(idx + 1))
        }

        private fun parseDescArgumentTypes(desc: String): List<Type> {
            return findArgumentTypes(desc, mutableListOf(), desc.indexOf('(') + 1, desc.indexOf(')'))
        }

        private fun findArgumentTypes(desc: String, types: MutableList<Type>, from: Int, to: Int): List<Type> {
            if(from >= to) return types
            var i = from
            while(desc[i] == '[') {
                ++i
            }
            if(desc[i] == 'L') {
                i = desc.indexOf(';', i)
            }
            types.add(Type(desc.substring(from, ++i)))
            return findArgumentTypes(desc, types, i, to)
        }
    }

    class Builder {

        private val argumentTypes = mutableListOf<Type>()
        private lateinit var returnType: Type

        fun setReturn(type: Type): Builder {
            this.returnType = type
            return this
        }

        fun addArgument(type: Type): Builder {
            argumentTypes.add(type)
            return this
        }

        fun addArgument(index: Int, type: Type): Builder {
            argumentTypes.add(index, type)
            return this
        }

        fun addArguments(types: Collection<Type>): Builder {
            argumentTypes.addAll(types)
            return this
        }

        fun build() = Signature(argumentTypes, returnType)
    }
}