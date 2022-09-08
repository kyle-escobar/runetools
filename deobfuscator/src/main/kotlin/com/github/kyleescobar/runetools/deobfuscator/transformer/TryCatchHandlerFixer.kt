@file:Suppress("UNCHECKED_CAST")

package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.byteflow.ClassGroup
import dev.kyleescobar.byteflow.editor.Label
import dev.kyleescobar.byteflow.editor.TryCatch
import dev.kyleescobar.byteflow.editor.Type
import org.tinylog.kotlin.Logger

class TryCatchHandlerFixer : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        group.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val tryCatchBlockMap = hashMapOf<Int, MutableList<TryCatch>>()
                method.editor.tryCatches().forEach { tryCatch ->
                    tryCatch as TryCatch
                    val handlerPos = method.editor.code().indexOf(tryCatch.handler())
                    if(!tryCatchBlockMap.containsKey(handlerPos)) {
                        val handlers = mutableListOf<TryCatch>()
                        handlers.add(tryCatch)
                        tryCatchBlockMap[handlerPos] = handlers
                    } else {
                        tryCatchBlockMap[handlerPos]!!.add(tryCatch)
                    }
                }

                tryCatchBlockMap.forEach { (pos, blocks) ->
                    if(blocks.size > 1) {
                        var start = Int.MAX_VALUE
                        var end = 0
                        val handler = blocks.first().handler()
                        val types = hashSetOf<Type>()

                        blocks.forEach { block ->
                            types.add(block.type())
                        }

                        blocks.forEach { block ->
                            val posStart = method.editor.code().indexOf(block.start())
                            if(posStart < start) {
                                start = posStart
                            }

                            val posEnd = method.editor.code().indexOf(block.end())
                            if(posEnd > end) {
                                end = posEnd
                            }

                            method.editor.tryCatches().remove(block)
                        }

                        types.forEach { catchType ->
                            method.editor.addTryCatch(TryCatch(method.editor.codeElementAt(start) as Label, method.editor.codeElementAt(end) as Label, handler, catchType))
                            count++
                        }
                    }
                }
            }
        }

        Logger.info("Fixed $count invalid try-catch block handlers.")
    }
}