package com.github.kyleescobar.runetools.deobfuscator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.types.file
import com.github.kyleescobar.runetools.deobfuscator.transformer.DeadCodeRemover
import com.github.kyleescobar.runetools.deobfuscator.transformer.IllegalStateExceptionRemover
import com.github.kyleescobar.runetools.deobfuscator.transformer.RuntimeExceptionRemover
import com.github.kyleescobar.runetools.deobfuscator.transformer.TryCatchHandlerFixer
import dev.kyleescobar.byteflow.ClassGroup
import org.tinylog.kotlin.Logger
import java.io.File
import kotlin.reflect.full.createInstance

class Deobfuscator(private val inputJar: File, private val outputJar: File) {

    private val group = ClassGroup()
    private val transformers = mutableListOf<Transformer>()

    private fun init() {
        Logger.info("Initializing...")

        group.clear()
        transformers.clear()

        /*
         * Load classes from input jar file.
         */
        Logger.info("Loading classes from input jar: ${inputJar.path}.")
        group.addFromJarFile(inputJar)
        Logger.info("Successfully loaded ${group.classes.size} classes.")

        /*
         * Create / order transformer instances.
         */
        add<TryCatchHandlerFixer>()
        add<RuntimeExceptionRemover>()
        //add<DeadCodeRemover>()
        add<IllegalStateExceptionRemover>()

        Logger.info("Registered ${transformers.size} bytecode transformers.")
    }

    fun run() {
        /*
         * Initialize / setup
         */
        init()

        Logger.info("Preparing to deobfuscate classes.")
        transformers.forEach { transformer ->
            Logger.info("Running bytecode transformer: ${transformer::class.simpleName}.")
            val start = System.currentTimeMillis()
            transformer.run(group)
            val delta = System.currentTimeMillis() - start
            Logger.info("Finished bytecode transformer: ${transformer::class.simpleName} in ${delta}ms.")
        }

        Logger.info("Successfully completed all bytecode transformers.")

        /*
         * Save the deobfuscated classes to output jar file.
         */
        Logger.info("Saving deobfuscated classes to jar: ${outputJar.path}.")
        group.saveToJarFile(outputJar)
        Logger.info("Deobfuscator has completed successfully.")

        TestClient(outputJar).start()
    }

    private inline fun <reified T : Transformer> add() {
        transformers.add(T::class.createInstance())
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val command = object : CliktCommand(
                name = "deobfuscate",
                help = "Deobfuscates the bytecode for the Jagex Old School RuneScape java gamepack/client jar.",
                invokeWithoutSubcommand = true,
                printHelpOnEmptyArgs = true
            ) {
                private val inputJar by argument("input-jar", help = "The obfuscated jar file path.").file(mustExist = true, canBeDir = false).check {
                    it.extension.contains("jar")
                }

                private val outputJar by argument("output-jar", help = "The jar file path to save deobfuscated classes to.").file(canBeDir = false).check {
                    it.extension.contains("jar")
                }

                override fun run() = Deobfuscator(inputJar, outputJar).run()
            }
            command.main(args)
        }
    }
}