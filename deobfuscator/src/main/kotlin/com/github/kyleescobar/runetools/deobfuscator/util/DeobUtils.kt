package com.github.kyleescobar.runetools.deobfuscator.util

import com.github.kyleescobar.runetools.deobfuscator.asm.ClassPool

fun String.isObfuscatedName(): Boolean {
    return this.length <= 2 || (this.length == 3 && listOf("aa", "ab", "ac", "ad", "ae", "af", "ag").any { this.startsWith(it) })
}

val ClassPool.clientClasses get() = this.classes.filter { it.name.isObfuscatedName() || it.name == "client" }