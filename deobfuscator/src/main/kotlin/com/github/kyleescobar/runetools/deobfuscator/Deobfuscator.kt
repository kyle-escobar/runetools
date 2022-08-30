package com.github.kyleescobar.runetools.deobfuscator

import com.github.kyleescobar.runetools.asm.ClassPool
import com.github.kyleescobar.runetools.deobfuscator.transformer.ControlFlowFixer
import com.github.kyleescobar.runetools.deobfuscator.transformer.DeadCodeRemover
import com.github.kyleescobar.runetools.deobfuscator.transformer.Renamer
import com.github.kyleescobar.runetools.deobfuscator.transformer.RuntimeExceptionRemover
import com.github.kyleescobar.runetools.deobfuscator.util.DeobUtils.isIgnored
import org.tinylog.kotlin.Logger
import java.io.File
import java.io.FileNotFoundException
import kotlin.reflect.full.createInstance

object Deobfuscator {

    private val pool = ClassPool()

    /**
     * ====== BYTECODE TRANSFORMERS =====
     */
    private val transformers = mutableListOf(
        RuntimeExceptionRemover::class,
        //DeadCodeRemover::class,
        ControlFlowFixer::class,
        Renamer::class
    )

    @JvmStatic
    fun main(args: Array<String>) {
        if(args.size != 2) {
            throw IllegalArgumentException("Missing required arguments. Usage: deobfuscator <input jar> <output jar>")
        }

        val input = File(args[0])
        val output = File(args[1])

        if(!input.exists()) {
            throw FileNotFoundException("Could not find input jar file: ${input.absolutePath}.")
        }

        if(output.exists()) {
            Logger.info("Overwriting output jar file: ${output.path}.")
            output.deleteRecursively()
        }

        Logger.info("Loading classes from jar file: ${input.path}.")
        pool.loadJar(input)
        pool.classes.forEach { cls ->
            if(cls.isIgnored()) {
                pool.ignoreClass(cls.name)
            }
        }
        Logger.info("Successfully loaded ${pool.classes.size} classes.")

        /*
         * Run bytecode transformers
         */
        transformers.map { it.createInstance() }.forEach { transformer ->
            Logger.info("Running bytecode transformer: ${transformer::class.simpleName}.")
            val start = System.currentTimeMillis()
            transformer.run(pool)
            Logger.info("Finished transformer in ${System.currentTimeMillis() - start}ms.")
        }

        Logger.info("Saving classes to jar file: ${output.path}.")
        pool.saveJar(output)

        Logger.info("Deobfuscator completed successfully.")

        /*
         * During development, run the test client.
         */
        TestClient.run(output)
    }
}