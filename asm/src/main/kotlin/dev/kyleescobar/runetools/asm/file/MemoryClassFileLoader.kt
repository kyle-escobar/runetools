package dev.kyleescobar.runetools.asm.file

import dev.kyleescobar.runetools.asm.reflect.ClassInfo
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class MemoryClassFileLoader : ClassFileLoader() {

    private val outputStreamClassMap = hashMapOf<String, ByteArrayOutputStream>()

    fun reset() {
        outputStreamClassMap.clear()
    }

    override fun outputStreamFor(name: String): OutputStream {
        return outputStreamClassMap.getOrPut(name) { ByteArrayOutputStream() }
    }

    override fun outputStreamFor(info: ClassInfo): OutputStream {
        return outputStreamFor(info.name() + ".class")
    }

    fun getClassBytes(name: String): ByteArray? = outputStreamClassMap[name]?.toByteArray()

}