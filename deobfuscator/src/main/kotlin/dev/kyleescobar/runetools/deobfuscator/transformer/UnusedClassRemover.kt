package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassFile
import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.deobfuscator.Transformer
import org.tinylog.kotlin.Logger

class UnusedClassRemover : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        val classes = group.classes.toTypedArray()
        for(cls in classes) {
            if(cls.fields.isNotEmpty()) continue
            if(cls.methods.isNotEmpty()) continue
            if(cls.isImplemented()) continue

            group.removeClass(cls)
            count++
        }

        Logger.info("Removed $count unused classes.")
    }

    private fun ClassFile.isImplemented(): Boolean {
        val group = this.group
        group.classes.forEach { cls ->
            if(cls.interfaces.myInterfaces.contains(this)) {
                return true
            }
        }
        return false
    }
}