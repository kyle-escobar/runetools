@file:Suppress("UNCHECKED_CAST")

package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.byteflow.ClassGroup
import dev.kyleescobar.byteflow.cfg.Block
import dev.kyleescobar.byteflow.cfg.Handler
import dev.kyleescobar.byteflow.editor.Type
import dev.kyleescobar.byteflow.tree.GotoStmt
import org.tinylog.kotlin.Logger
import java.lang.RuntimeException

class RuntimeExceptionRemover : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        group.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val cfg = method.cfg()
                val itr = cfg.handlersMap().entries.iterator()
                while(itr.hasNext()) {
                    val (_, handler) = itr.next() as Map.Entry<Block, Handler>
                    if(handler.catchType() == Type.getType(RuntimeException::class.java)) {
                        val tryCatchBlock = (handler.catchBlock().tree().lastStmt() as GotoStmt).target()
                        itr.remove()
                        cfg.removeNode(handler.catchBlock().label())
                        cfg.removeNode(tryCatchBlock.label())
                        count++
                    }
                }
            }
        }

        Logger.info("Removed $count RuntimeException try-catch blocks.")
    }

}