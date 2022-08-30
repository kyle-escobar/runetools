package com.github.kyleescobar.runetools.deobfuscator.util

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

object DeobUtils {

    fun String.isObfuscatedName(): Boolean {
        return this.length <= 2 || (this.length == 3 && arrayOf("aa", "ab", "ac", "ad", "ae", "af").any { this.startsWith(it) }) || this == "client"
    }

    fun String.isDeobfuscatedName(): Boolean {
        return arrayOf("class", "method", "field", "var", "arg").any { this.startsWith(it) } || this == "client"
    }

    fun ClassNode.isIgnored(): Boolean = !this.name.isObfuscatedName() && !this.name.isDeobfuscatedName()
    fun MethodNode.isIgnored(): Boolean = !this.name.isObfuscatedName() && !this.name.isDeobfuscatedName()
    fun FieldNode.isIgnored(): Boolean = !this.name.isObfuscatedName() && !this.name.isDeobfuscatedName()
}