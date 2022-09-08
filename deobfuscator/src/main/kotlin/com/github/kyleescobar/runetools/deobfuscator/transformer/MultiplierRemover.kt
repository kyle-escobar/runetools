package com.github.kyleescobar.runetools.deobfuscator.transformer

import com.github.kyleescobar.runetools.deobfuscator.Transformer
import dev.kyleescobar.byteflow.BFField
import dev.kyleescobar.byteflow.ClassGroup
import dev.kyleescobar.byteflow.editor.MemberRef
import dev.kyleescobar.byteflow.tree.*
import dev.kyleescobar.byteflow.visitor.ExprTreeVisitor
import org.tinylog.kotlin.Logger
import java.math.BigInteger
import kotlin.math.abs

class MultiplierRemover : Transformer {

    private var count = 0

    private val multipliers = hashMapOf<String, MultiplierNumber>()

    override fun run(group: ClassGroup) {
        findMultipliers(group)
        removeMultipliers(group)
    }

    private fun findMultipliers(group: ClassGroup) {
        Logger.info("Finding multipliers and associated fields.")

        group.classes.filter { !it.name.contains("/") }.forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                method.cfg().visit(object : ExprTreeVisitor() {
                    override fun visitArithExpr(expr: ArithExpr) {
                        if(expr.operation() == ArithExpr.MUL.code) {
                            var ldc: ConstantExpr? = null
                            var other: Expr? = null

                            if(expr.left() is ConstantExpr && expr.right() !is ConstantExpr) {
                                ldc = expr.left() as ConstantExpr
                                other = expr.right()
                            } else if(expr.right() is ConstantExpr && expr.left() !is ConstantExpr) {
                                ldc = expr.right() as ConstantExpr
                                other = expr.left()
                            }

                            if(ldc == null || other == null) return

                            var safe = true
                            var storeExpr: StoreExpr? = null

                            expr.stmt().visitChildren(object : TreeVisitor() {
                                override fun visitExpr(expr: Expr) {
                                    if(expr is StoreExpr) {
                                        if(storeExpr != null) {
                                            safe = false
                                        }
                                        storeExpr = expr
                                    }
                                }
                            })

                            if(!safe) return

                            var field: BFField? = null
                            if(storeExpr != null) {
                                val target = storeExpr!!.target()
                                if(target is FieldExpr) {
                                    val fieldRef = target.field()
                                    field = group.getClass(fieldRef.declaringClass().className())?.getField(fieldRef.name(), fieldRef.type().descriptor())
                                } else if(target is StaticFieldExpr) {
                                    val fieldRef = target.field()
                                    field = group.getClass(fieldRef.declaringClass().className())?.getField(fieldRef.name(), fieldRef.type().descriptor())
                                }
                            }

                            var otherField: BFField? = null
                            if(other is FieldExpr) {
                                val fieldRef = other.field()
                                otherField = group.getClass(fieldRef.declaringClass().className())?.getField(fieldRef.name(), fieldRef.type().descriptor())
                            } else if(other is StaticFieldExpr) {
                                val fieldRef = other.field()
                                otherField = group.getClass(fieldRef.declaringClass().className())?.getField(fieldRef.name(), fieldRef.type().descriptor())
                            }

                            if(storeExpr != null && otherField != null && field != otherField) return

                            val resolvedField = field ?: otherField ?: return

                            val prevMultiplier = multipliers[resolvedField.toString()]
                            if(prevMultiplier != null) return

                            expr.stmt().visitChildren(object : TreeVisitor() {
                                override fun visitExpr(expr: Expr) {
                                    var relatedField: BFField? = null
                                    if(expr is FieldExpr || expr is StaticFieldExpr) {
                                        val fieldRef = when(expr) {
                                            is FieldExpr -> expr.field()
                                            is StaticFieldExpr -> expr.field()
                                            else -> throw IllegalStateException()
                                        }
                                        relatedField = group.getClass(fieldRef.declaringClass().className())?.getField(fieldRef.name(), fieldRef.type().descriptor())
                                    }

                                    if(relatedField != null && relatedField != resolvedField) {
                                        safe = false
                                    }
                                }
                            })

                            if(!safe) return

                            if(ldc.value() is Int || ldc.value() is Long) {
                                val value = when(ldc.value()) {
                                    is Long -> ldc.value() as Long
                                    is Int -> ldc.value() as Int
                                    else -> throw IllegalStateException()
                                }
                                val quotient = BigInteger.valueOf(value.toLong())
                                if((value.toInt() and 1)  == 0) return

                                val multiplier = MultiplierNumber.decipher(quotient, if(ldc.value() is Long) 64 else 32, storeExpr != null)
                                multipliers[resolvedField.toString()] = multiplier
                            }
                        }
                    }
                })
            }
        }
    }

    private fun removeMultipliers(group: ClassGroup) {
        Logger.info("Removing found field multipliers.")

        group.classes.filter { !it.name.contains("/") }.forEach { cls ->
            cls.methods.forEach methodLoop@ { method ->
                method.cfg().visit(object : ExprTreeVisitor() {
                    override fun visitArithExpr(expr: ArithExpr) {
                        if(expr.operation() != ArithExpr.MUL.code && expr.operation() != ArithExpr.ADD.code && expr.operation() != ArithExpr.SUB.code) {
                            return
                        }

                        var ldc: ConstantExpr? = null
                        var other: Expr? = null

                        if(expr.left() is ConstantExpr) {
                            ldc = expr.left() as ConstantExpr
                            other = expr.right()
                        } else if(expr.right() is ConstantExpr) {
                            ldc = expr.right() as ConstantExpr
                            other = expr.left()
                        }

                        if(ldc != null) {
                            var fieldRef: MemberRef? = null

                            expr.visitChildren(object : TreeVisitor() {
                                override fun visitExpr(childExpr: Expr) {
                                    if(childExpr is ConstantExpr) return
                                    when (childExpr) {
                                        is FieldExpr -> { fieldRef = childExpr.field() }
                                        is StaticFieldExpr -> { fieldRef = childExpr.field() }
                                        is StoreExpr -> {
                                            if(childExpr.parent() is ArithExpr && (childExpr.parent() as ArithExpr).operation() != ArithExpr.MUL.code) return
                                            childExpr.visitChildren(this)
                                        }
                                    }
                                }
                            })

                            var loadCodec = if(fieldRef != null) multipliers["${fieldRef!!.declaringClass().className()}.${fieldRef!!.name()}"] else null

                            var store: StoreExpr? = null
                            var n: Node? = expr
                            while (n!!.parent().also { n = it } != null) {
                                if (n is StoreExpr) {
                                    store = n as StoreExpr
                                    break
                                }
                            }

                            var storeCodec: MultiplierNumber? = null
                            if(store != null) {
                                val memExpr = store.target()
                                if(memExpr is FieldExpr) {
                                    storeCodec = multipliers["${memExpr.field().declaringClass().className()}.${memExpr.field().name()}"]
                                } else if(memExpr is StaticFieldExpr) {
                                    storeCodec = multipliers["${memExpr.field().declaringClass().className()}.${memExpr.field().name()}"]
                                }
                            }

                            if(expr.operation() != ArithExpr.MUL.code && storeCodec == loadCodec) {
                                loadCodec = null
                            }
                            decrypt(loadCodec, storeCodec, expr, ldc, other)
                        }
                    }

                    override fun visitStoreExpr(expr: StoreExpr) {
                        val fieldRef: MemberRef = if(expr.target() is FieldExpr) {
                            (expr.target() as FieldExpr).field()
                        } else if(expr.target() is StaticFieldExpr) {
                            (expr.target() as StaticFieldExpr).field()
                        } else {
                            return
                        }

                        if(expr.expr() is ConstantExpr) {
                            val codec = multipliers["${fieldRef.declaringClass().className()}.${fieldRef.name()}"]
                            if(codec != null) {
                                val ldc = expr.expr() as ConstantExpr
                                if(ldc.value() !is Int && ldc.value() !is Long) return
                                decrypt(null, codec, expr, ldc, null)
                            }
                        }
                    }
                })
            }
        }

        Logger.info("Removed $count field multipliers.")
    }

    private fun decrypt(loadCodec: MultiplierNumber?, storeCodec: MultiplierNumber?, expr: Expr, ldc: ConstantExpr, other: Expr?) {
        if(loadCodec == null && storeCodec == null) return

        var unsafe = false
        val encodedValue = ldc.value() as Number
        val decodedValue: Number
        if(encodedValue is Int) {
            var value = encodedValue.toInt()
            if(loadCodec != null) value *= loadCodec.product.toInt()
            if(storeCodec != null) value *= storeCodec.quotient.toInt()
            decodedValue = value
            if((abs(value) and 0xfffff) != abs(value)) {
                unsafe = true
            }
        } else if(encodedValue is Long) {
            var value = encodedValue.toLong()
            if(loadCodec != null) value *= loadCodec.product.toLong()
            if(storeCodec != null) value *= storeCodec.quotient.toLong()
            decodedValue = value
            if((abs(value) and 0xffffffff) != abs(value)) {
                unsafe = true
            }
        } else {
            return
        }

        if(expr is ArithExpr && expr.operation() == ArithExpr.MUL.code && decodedValue.toLong() == 1L && other != null) {
            expr.replaceWith(other.clone() as Node)
            count++
        } else {
            ldc.replaceWith(ConstantExpr(decodedValue, ldc.type()))
        }
    }

    /**
     * ==== Multiplicative Inverse Math Utils
     */
    private object ModMath {

        private val INT_MODULUS = BigInteger.ONE.shiftLeft(32)
        private val LONG_MODULUS = BigInteger.ONE.shiftLeft(64)

        fun inverse(value: BigInteger, bits: Int): BigInteger {
            val shift = when(bits) {
                32 -> INT_MODULUS
                64 -> LONG_MODULUS
                else -> throw IllegalStateException()
            }
            return value.modInverse(shift)
        }

        fun gcd(vararg values: BigInteger): BigInteger {
            var num = values[0].gcd(values[1])
            for(i in 2 until values.size) {
                num = num.gcd(values[i])
            }
            return num
        }
    }

    private data class MultiplierNumber(
        val product: BigInteger,
        val quotient: BigInteger,
        val gcd: BigInteger,
        val bits: Int,
        val unsafe: Boolean
    ) {

        val trueValue: BigInteger = gcd.multiply(product).let { quotient.multiply(it) }

        companion object {

            fun decipher(quotient: BigInteger, bits: Int, store: Boolean): MultiplierNumber {
                var unsafe = false

                val product = ModMath.inverse(quotient, bits)
                val gcd = ModMath.gcd(product, quotient)

                if(gcd.toLong() != 1L) {
                    val value1 = product.divide(gcd).multiply(quotient).toLong()
                    val value2 = quotient.divide(gcd).multiply(product).toLong()
                    if(value1 != value2) {
                        unsafe = true
                    }
                }

                return if(!store) {
                    MultiplierNumber(product, quotient, gcd, bits, unsafe)
                } else {
                    MultiplierNumber(quotient, product, gcd, bits, unsafe)
                }
            }
        }
    }
}