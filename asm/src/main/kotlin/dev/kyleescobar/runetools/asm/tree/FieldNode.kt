package dev.kyleescobar.runetools.asm.tree

import dev.kyleescobar.runetools.asm.editor.FieldEditor
import dev.kyleescobar.runetools.asm.reflect.FieldInfo
import dev.kyleescobar.runetools.asm.visitor.NodeVisitor

class FieldNode(val owner: ClassNode, val info: FieldInfo) {

    private var editor: FieldEditor? = null

    fun editor(): FieldEditor {
        if(editor == null) {
            editor = owner.editor.context().editField(info)
        }
        return editor!!
    }

    fun commit() {
        if(editor != null) {
            editor!!.commit()
            editor = null
        }
    }

    fun accept(visitor: NodeVisitor) {
        visitor.visitField(this)
    }
}