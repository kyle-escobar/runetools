package dev.kyleescobar.runetools.util.ext

fun <T> Collection<T>.copyOf(): Collection<T> {
    val ret = mutableListOf<T>()
    this.forEach { ret.add(it) }
    return ret
}