package com.github.kyleescobar.runetools.deobfuscator.util

import kotlin.reflect.KProperty

class ExtensionField<R, T>(private val init: (R) -> T = { throw IllegalStateException("Property not initialized!") }) {
    private val store = mutableWeakIdentityHashMap<R, T>()

    operator fun getValue(self: R, prop: KProperty<*>): T = store[self] ?: setValue(self, prop, init(self))

    operator fun setValue(self: R, prop: KProperty<*>, value: T): T = value.apply {
        store[self] = this
    }
}

fun <R, T> field(init: (R) -> T) = ExtensionField(init)
fun <R, T> field() = ExtensionField<R, T>()