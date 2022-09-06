package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import com.github.kyleescobar.runetools.deobfuscator.asm.ClassPool
import com.github.kyleescobar.runetools.deobfuscator.asm.LabelMap
import org.objectweb.asm.tree.AbstractInsnNode.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.tinylog.kotlin.Logger
import java.util.*

class ControlFlowOptimizer : Transformer {

    private var count = 0

    override fun run(pool: ClassPool) {
        pool.classes.forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                if(method.tryCatchBlocks.isNotEmpty()) return@methodLoop

                val newInsns = InsnList()
                val controlFlow = ControlFlowGraph(cls, method)
                if(controlFlow.blocks.isNotEmpty()) {
                    /*
                     * Change the method's instructions of the cfg blocks
                     * to normalize the control-flow and labels.
                     */
                    val labelMap = LabelMap()
                    val blockQueue = Stack<Block>()
                    val orderedBlocks = hashSetOf<Block>()

                    blockQueue.add(controlFlow.blocks.first())
                    while (blockQueue.isNotEmpty()) {
                        val block = blockQueue.pop()
                        if (block in orderedBlocks) {
                            continue
                        }

                        // Add the current visited block and it's branches and next block.
                        orderedBlocks.add(block)
                        block.branches.forEach { blockQueue.add(it.head) }
                        if (block.next != null) {
                            blockQueue.add(block.next)
                        }

                        // Add the block's instructions to the newInsns which will be used
                        // to rebuild this method's instruction list.
                        // We clone the blocks insnlist here to force the label map to be regenerated.
                        for(i in block.startIndex until block.endIndex) {
                            newInsns.add(method.instructions[i].clone(labelMap))
                        }
                    }
                }
                method.instructions = newInsns
                count += controlFlow.blocks.size
            }
        }

        Logger.info("Reordered $count control-flow blocks.")
    }

    private class ControlFlowGraph(private val cls: ClassNode, private val method: MethodNode) {

        val blocks = mutableListOf<Block>()

        private val analyzer = object : Analyzer<BasicValue>(BasicInterpreter()) {
            override fun init(owner: String, method: MethodNode) {
                var cur = Block()
                blocks.add(cur)
                for(i in 0 until method.instructions.size()) {
                    val insn = method.instructions[i]
                    cur.endIndex++
                    if(insn.next == null) break
                    if(insn.next.type == LABEL || insn.type == JUMP_INSN || insn.type == LOOKUPSWITCH_INSN || insn.type == TABLESWITCH_INSN) {
                        cur = Block()
                        cur.startIndex = i + 1
                        cur.endIndex = i + 1
                        blocks.add(cur)
                    }
                }
            }

            override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
                val cur = blocks.first { insnIndex in it.startIndex until it.endIndex }
                val next = blocks.first { successorIndex in it.startIndex until it.endIndex }
                if(cur != next) {
                    if(insnIndex + 1 == successorIndex) {
                        cur.next = next
                        next.prev = cur
                    } else {
                        cur.branches.add(next)
                    }
                }
            }
        }

        init {
            try { analyzer.analyze(cls.name, method) }
            catch(e : Exception) { /* Do Nothing */ }
        }
    }

    private class Block {
        var startIndex: Int = 0
        var endIndex: Int = 0
        var prev: Block? = null
        var next: Block? = null
        val branches = mutableListOf<Block>()
        val insns = InsnList()

        val head: Block get() {
            var b = this
            var p = this.prev
            while(p != null) {
                b = p
                p = b.prev
            }
            return b
        }

        val lineNumber: Int get() {
            insns.forEach { insn ->
                if(insn is LineNumberNode) {
                    return insn.line
                }
            }
            return -1
        }
    }
}