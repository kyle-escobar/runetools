package dev.kyleescobar.runetools.deobfuscator.ext

import dev.kyleescobar.runetools.asm.ClassFile
import dev.kyleescobar.runetools.asm.Field
import dev.kyleescobar.runetools.asm.Method
import dev.kyleescobar.runetools.deobfuscator.Deobfuscator

fun Collection<ClassFile>.filterClientClasses(): Collection<ClassFile> {
    return this.filter { !it.name.contains("/") }.toList()
}

fun Collection<Method>.filterObfuscatedMethods(): Collection<Method> {
    return this.filter { Deobfuscator.isObfuscatedName(it.name) }.toList()
}

fun Collection<Field>.filterObfuscatedFields(): Collection<Field> {
    return this.filter { Deobfuscator.isObfuscatedName(it.name) }
}
