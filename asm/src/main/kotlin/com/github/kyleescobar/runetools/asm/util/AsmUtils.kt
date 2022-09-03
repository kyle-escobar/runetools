package com.github.kyleescobar.runetools.asm.util

import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LdcInsnNode

object AsmUtils {

    fun extractStrings(insns: InsnList, out: HashSet<String>) {
        val it = insns.iterator()
        while(it.hasNext()) {
            val insn = it.next()
            if(insn is LdcInsnNode) {
                if(insn.cst is String) {
                    out.add(insn.cst as String)
                }
            }
        }
    }
}