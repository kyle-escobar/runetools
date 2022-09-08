package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.byteflow.ClassGroup
import dev.kyleescobar.byteflow.editor.Type
import dev.kyleescobar.byteflow.tree.*
import dev.kyleescobar.byteflow.visitor.ExprTreeVisitor
import org.tinylog.kotlin.Logger
import java.lang.IllegalStateException
import kotlin.math.abs

class IllegalStateExceptionRemover : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        group.classes.filter { !it.name.contains("/") }.forEach { cls ->
            cls.methods.forEach { method ->
                method.accept(object : ExprTreeVisitor() {
                    override fun visitIfCmpStmt(stmt: IfCmpStmt) {
                        var local: LocalExpr? = null
                        var ldc: ConstantExpr? = null

                        if(stmt.left() is LocalExpr && stmt.right() is ConstantExpr) {
                            local = stmt.left() as LocalExpr
                            ldc = stmt.right() as ConstantExpr
                        } else if(stmt.right() is LocalExpr && stmt.left() is ConstantExpr) {
                            local = stmt.right() as LocalExpr
                            ldc = stmt.left() as ConstantExpr
                        }

                        if(local == null || ldc == null) {
                            return
                        }

                        if(ldc.value() !is Int && ldc.value() !is Long) {
                            return
                        }

                        var flag = false
                        stmt.falseTarget().visitChildren(object : TreeVisitor() {
                            override fun visitStmt(s: Stmt) {
                                s.visitChildren(this)
                            }

                            override fun visitReturnStmt(stmt: ReturnStmt) {
                                if(ldc.value() is Int) {
                                    val value = ldc.value() as Int
                                    if((abs(value) and 0xFFFFF) != abs(value)) {
                                        flag = true
                                    }
                                } else {
                                    val value = ldc.value() as Long
                                    if((abs(value) and 0xFFFFFFFF) != abs(value)) {
                                        flag = true
                                    }
                                }
                            }

                            override fun visitGotoStmt(s: GotoStmt) {
                                if(s.target() == s.block() || s.target() == stmt.falseTarget()) {
                                    flag = true
                                }
                            }

                            override fun visitNewExpr(expr: NewExpr) {
                                if(expr.objectType() == Type.getType(IllegalStateException::class.java)) {
                                    flag = true
                                }
                            }
                        })

                        if(flag) {
                            method.cfg().visit(object : ExprTreeVisitor() {
                                override fun visitStmt(s: Stmt) {
                                    s.visitChildren(this)
                                }

                                override fun visitLocalExpr(expr: LocalExpr) {
                                    if(!expr.hasParent()) return
                                    if(local.index() == expr.index()) {
                                        if(expr.parent() is IfCmpStmt) {
                                            val cmp = expr.parent() as IfCmpStmt
                                            val trueTarget = cmp.trueTarget()
                                            val falseTarget = cmp.falseTarget()

                                            cmp.replaceWith(GotoStmt(trueTarget))
                                            if(trueTarget != falseTarget) {
                                                method.cfg().removeNode(falseTarget.label())
                                            }

                                            count++
                                        }
                                    }
                                }
                            })
                        }
                    }
                })
            }
        }

        Logger.info("Removed $count control-flow blocks.")
    }
}