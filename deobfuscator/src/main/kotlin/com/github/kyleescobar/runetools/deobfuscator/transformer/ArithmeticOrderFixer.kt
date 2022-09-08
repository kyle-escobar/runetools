package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.byteflow.ClassGroup
import dev.kyleescobar.byteflow.tree.ArithExpr
import dev.kyleescobar.byteflow.tree.ConstantExpr
import dev.kyleescobar.byteflow.visitor.ExprTreeVisitor
import org.tinylog.kotlin.Logger
import java.math.BigInteger

class ArithmeticOrderFixer : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        group.classes.filter { !it.name.contains("/") }.forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                if(method.name.startsWith("<")) return@methodLoop
                method.cfg().visit(object : ExprTreeVisitor() {
                    override fun visitArithExpr(expr: ArithExpr) {
                        if (expr.hasParent() && expr.parent().hasParent()) {
                            if (expr.left().type().isIntegral && expr.right().type().isIntegral) {
                                /*
                                 * Fixes the following cases:
                                 * 1234 * x ==> x * 1234
                                 * 1234 + x ==> x + 1234
                                 */
                                if(expr.operation() == ArithExpr.MUL.code) {
                                    if(expr.left() is ConstantExpr) {
                                        expr.replaceWith(ArithExpr(ArithExpr.MUL, expr.right(), expr.left(), expr.type()), false)
                                        count++
                                    }
                                }
                            }
                        }
                    }
                })
            }
        }

        Logger.info("Reordered $count arithmetic expressions.")
    }
}