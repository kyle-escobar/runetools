package dev.kyleescobar.runetools.asm

data class Type(private val type: String) {

    companion object {
        val VOID = Type("V")
        val BOOLEAN = Type("Z")
        val CHAR = Type("C")
        val BYTE = Type("B")
        val SHORT = Type("S")
        val INT = Type("I")
        val FLOAT = Type("F")
        val LONG = Type("L")
        val DOUBLE = Type("D")
        val OBJECT = Type("Ljava/lang/Object;")
        val STRING = Type("Ljava/lang/String;")
        val THROWABLE = Type("Ljava/lang/Throwable;")
        val EXCEPTION = Type("Ljava/lang/Exception;")
    }

    fun isPrimitive(): Boolean = this in listOf(
        BOOLEAN,
        BYTE,
        SHORT,
        CHAR,
        INT,
        VOID,
        LONG,
        FLOAT,
        DOUBLE
    )

    fun isObject(): Boolean = this == OBJECT

    fun isArray(): Boolean = this.dims > 0

    fun isStackInt(): Boolean = this in listOf(
        BOOLEAN,
        BYTE,
        SHORT,
        CHAR,
        INT
    )

    val stackSize: Int get() = when(this) {
        DOUBLE, LONG -> 2
        VOID -> 0
        else -> 1
    }

    val dims: Int get() {
        if(!type.startsWith("[")) {
            return 0
        }
        return type.chars().filter { it.toChar() == '[' }.count().toInt()
    }

    val subType: Type get() {
        if(!type.startsWith("[")) {
            throw IllegalStateException("$type is not an array-type.")
        }
        return Type(type.substring(1))
    }

    val internalName: String get() {
        var s = type
        while(s.startsWith("[")) {
            s = s.substring(1)
        }
        return if(s.startsWith("L") && s.endsWith(";")) {
            s.substring(1, s.length - 1)
        } else {
            s
        }
    }

    override fun toString(): String {
        return type
    }
}