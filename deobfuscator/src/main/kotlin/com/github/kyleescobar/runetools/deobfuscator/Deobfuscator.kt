package com.github.kyleescobar.runetools.deobfuscator

import com.github.kyleescobar.runetools.deobfuscator.asm.ClassPool
import com.github.kyleescobar.runetools.deobfuscator.transformer.*
import com.github.kyleescobar.runetools.deobfuscator.transformer.MultiplierRemover
import org.tinylog.kotlin.Logger
import java.io.File
import kotlin.reflect.full.createInstance

class Deobfuscator(val inputJar: File, val outputJar: File) {

    private val pool = ClassPool()
    private val transformers = mutableListOf(
        RuntimeExceptionRemover::class,
        DeadCodeRemover::class,
        ControlFlowOptimizer::class,
        Renamer::class,
        OpaquePredicateRemover::class,
        GotoRemover::class,
        InvalidConstructorRemover::class,
        DeadCodeRemover::class,
        StackFrameRebuilder::class,
        MultiplierRemover::class
    )

    fun run() {
        Logger.info("Loading classes from input jar: ${inputJar.path}.")
        pool.addJar(inputJar)
        pool.loadHierarchy()
        Logger.info("Successfully loaded ${pool.classes.size} classes.")

        /*
         * Run bytecode transformers.
         */
        transformers.map { it.createInstance() }.forEach { transformer ->
            Logger.info("Running transformer: ${transformer::class.simpleName}.")
            val start = System.currentTimeMillis()
            transformer.run(pool)
            Logger.info("Finished transformer: ${transformer::class.simpleName} in ${System.currentTimeMillis() - start}ms.")
        }

        Logger.info("Saving classes to output jar: ${outputJar.path}.")
        pool.saveJar(outputJar)
        Logger.info("Deobfuscator has completed successfully.")

        TestClient(outputJar).start()
    }
}