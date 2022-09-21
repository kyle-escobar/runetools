package dev.kyleescobar.runetools.deobfuscator.transformer

import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.asm.attributes.Code
import dev.kyleescobar.runetools.asm.attributes.code.Instruction
import dev.kyleescobar.runetools.asm.attributes.code.Label
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.JumpingInstruction
import dev.kyleescobar.runetools.asm.attributes.code.instructions.Goto
import dev.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.runetools.deobfuscator.ext.filterClientClasses
import org.tinylog.kotlin.Logger

class ControlFlowOptimizer : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        group.classes.filterClientClasses().forEach classLoop@ { cls ->
            cls.methods.forEach methodLoop@ { method ->
                if(method.code == null || method.code.exceptions.exceptions.isNotEmpty()) return@methodLoop
                rebuildCode(method.code)
                rebuildLabels(method.code)
            }
        }

        Logger.info("Reordered $count method control-flow blocks.")
    }

    private fun rebuildCode(code: Code) {
        val newInsns = code.instructions
        val cfg = ControlFlowGraph(code)

        newInsns.clear()
        val sortedBlocks = cfg.sortedBlocks

        sortedBlocks.forEach { block ->
            ++count
            block.insns.forEach { insn ->
                newInsns.addInstruction(insn)
                insn.instructions = newInsns
            }

            if(block.next != null && block.insns.size > 0) {
                val insn = block.insns.last()
                if(!insn.isTerminal) {
                    val next = block.next!!
                    var labelInsn = next.insns.first()
                    if(labelInsn !is Label) {
                        labelInsn = Label(newInsns)
                        next.insns.add(0, labelInsn)
                    }
                    newInsns.addInstruction(Goto(newInsns, labelInsn))
                }
            }
        }
    }

    private fun rebuildLabels(code: Code) {
        val insns = code.instructions
        val insnList = insns.instructions

        for(i in 0 until insnList.size - 1) {
            val insn1 = if(insnList.size <= i) continue else insnList[i]
            val insn2 = if(insnList.size <= i + 1) continue else insnList[i + 1]
            if(insn1 !is Goto) continue
            if(insn1.jumps.first() != insn2) continue
            insns.remove(insn1)
        }
    }

    /**
     * ===== CONTROL FLOW CLASSES IMPL =====
     */

    private class Block {

        var id = -1
        var isJumpTarget = false

        val prevBranches = mutableListOf<Block>()
        val nextBranches = mutableListOf<Block>()

        var prev: Block? = null
        var next: Block? = null

        val insns = mutableListOf<Instruction>()

        val lineNumber: Int get() {
            insns.forEach { insn ->
                if(insn is Label) {
                    if(insn.lineNumber != null) {
                        return insn.lineNumber
                    }
                }
            }
            return -1
        }

        companion object {

            fun compare(block1: Block, block2: Block): Int {
                val line1 = block1.lineNumber
                val line2 = block2.lineNumber
                if(line1 == line2 || line1 == -1 || line2 == -1) {
                    return 0
                }
                return line1.compareTo(line2)
            }
        }
    }

    private class ControlFlowGraph(private val code: Code) {

        val blocks = hashMapOf<Label, Block>()
        val allBlocks = mutableListOf<Block>()
        val head = Block()

        init {
            var id = 0
            code.instructions.forEach { insn ->
                if(insn is Label) {
                    blocks.computeIfAbsent(insn) {
                        val b = Block()
                        allBlocks.add(b)
                        b
                    }
                }
            }
            allBlocks.add(0, head)
            var cur = head
            code.instructions.forEach { insn ->
                if(insn is Label) {
                    val next = blocks[insn]!!
                    if(next.id == -1) {
                        next.id = id++
                    }
                    if(next != cur) {
                        val last = if(cur.insns.isEmpty()) null else cur.insns.last()
                        if(last == null || !last.isTerminal) {
                            next.prev = cur
                            cur.next = next
                        }
                        cur = next
                    }
                }

                cur.insns.add(insn)
                if(insn is JumpingInstruction) {
                    insn.jumps.forEach { label ->
                        val next = blocks[label]!!
                        if(next.id == -1) {
                            next.id = id++
                        }
                        cur.nextBranches.add(next)
                        next.prevBranches.add(cur)
                    }
                }
            }
        }

        val sortedBlocks: List<Block> get() {
            val lst = mutableListOf<Block>()
            walk(head, lst, mutableSetOf())
            return lst.reversed()
        }

        private fun walk(cur: Block, order: MutableList<Block>, visited: MutableSet<Block>) {
            val directNext = cur.next
            if(directNext != null && visited.add(directNext)) {
                walk(cur.next!!, order, visited)
            }

            val nextBlocks = cur.nextBranches
            nextBlocks.sortWith(Block::compare)
            nextBlocks.forEach { b ->
                if(visited.add(b)) {
                    walk(b, order, visited)
                }
            }
            order.add(cur)
        }
    }
}