package dev.kyleescobar.runetools.asm.tree

import dev.kyleescobar.runetools.asm.cfg.FlowGraph
import dev.kyleescobar.runetools.asm.codegen.CodeGenerator
import dev.kyleescobar.runetools.asm.editor.MethodEditor
import dev.kyleescobar.runetools.asm.reflect.MethodInfo
import dev.kyleescobar.runetools.asm.visitor.ExprVisitor
import dev.kyleescobar.runetools.asm.visitor.NodeVisitor

class MethodNode(val owner: ClassNode, val info: MethodInfo) {

    private var editor: MethodEditor? = null
    private var cfg: FlowGraph? = null

    fun editor(): MethodEditor {
        if(editor == null) {
            editor = owner.editor.context().editMethod(info)
        }
        return editor!!
    }

    fun cfg(): FlowGraph {
        if(cfg == null) {
            cfg = FlowGraph(editor())
        }
        return cfg!!
    }

    fun commit() {
        if(cfg != null) {
            val codegen = CodeGenerator(editor())
            codegen.replacePhis(cfg)
            codegen.simplifyControlFlow(cfg)
            editor().clearCode()
            cfg!!.visit(codegen)
            cfg = null
        }
        if(editor != null) {
            editor!!.commit()
            editor = null
        }
    }

    fun accept(visitor: NodeVisitor) {
        visitor.visitMethod(this)
        if(visitor is ExprVisitor) {
            cfg().visit(visitor)
        }
    }
}