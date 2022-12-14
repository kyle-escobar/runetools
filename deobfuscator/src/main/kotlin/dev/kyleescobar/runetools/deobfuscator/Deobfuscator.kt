package dev.kyleescobar.runetools.deobfuscator

import dev.kyleescobar.runetools.asm.ClassFile
import dev.kyleescobar.runetools.asm.ClassGroup
import dev.kyleescobar.runetools.deob.util.JarUtil
import dev.kyleescobar.runetools.deobfuscator.transformer.*
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.tinylog.kotlin.Logger
import java.io.File
import kotlin.reflect.full.createInstance
import kotlin.system.exitProcess

object Deobfuscator {

    private lateinit var group: ClassGroup
    private val ignoredClasses = mutableListOf<ClassFile>()

    private lateinit var inputFile: File
    private lateinit var outputFile: File
    private var postTestEnabled = false

    private val transformers = mutableListOf<Transformer>()

    @JvmStatic
    fun main(args: Array<String>) {
        if(args.size < 2) {
            Logger.error("Invalid program arguments. Arguments: '<input jar> <output jar> [--test]'.")
            exitProcess(0)
        }

        inputFile = File(args[0])
        outputFile = File(args[1])
        postTestEnabled = if(args.size == 3) args[2] == "--test" else false

        /*
         * Initialize deobfuscator.
         */
        init()

        /*
         * Run the Deobfuscator.
         */
        run()

        /*
         * Export deobfuscated classes.
         */
        if(outputFile.exists()) {
            Logger.info("Overwriting existing output jar file: ${outputFile.path}.")
            outputFile.deleteRecursively()
        }

        Logger.info("Exporting deobfuscated classes to jar file: ${outputFile.path}.")
        ignoredClasses.forEach { cls ->
            group.addClass(cls)
        }
        JarUtil.save(group, outputFile)
        Logger.info("Deobfuscator completed successfully.")

        if(postTestEnabled) {
            Logger.info("Testing mode enabled! Starting test-client from jar file: ${outputFile.path}.")

            try {
                TestClient(outputFile).start()
            } catch (e: Exception) {
                e.printStackTrace(System.err)
            }

            Logger.info("Test client process has exited.")
        }
    }

    private fun init() {
        Logger.info("Initializing.")

        if(!inputFile.exists()) {
            Logger.error("Input jar file: ${inputFile.path} could not be found.")
            exitProcess(-1)
        }

        Logger.info("Loading classes from jar file: ${inputFile.path}.")
        group = JarUtil.load(inputFile)
        val classes = group.classes.toTypedArray()
        for(cls in classes) {
            if(cls.name.contains("/")) {
                ignoredClasses.add(cls)
                group.removeClass(cls)
            }
        }
        Logger.info("Found ${group.classes.size} classes. (Ignored ${ignoredClasses.size} classes).")

        transformers.clear()

        /*
         * ==== Register Transformers ====
         */
        register<RuntimeExceptionRemover>()
        register<ControlFlowOptimizer>()
        register<Renamer>()
        register<DeadCodeRemover>()
        register<UnusedMethodRemover>()
        register<IllegalStateExceptionRemover>()
        register<RedundantParameterRemover>()
        register<DeadCodeRemover>()
        register<UnusedMethodRemover>()
        register<UnusedParameterRemover>()
        register<FieldClassOptimizer>()
        register<ClassMemberSorter>()
        register<UnusedClassRemover>()
        register<AnnotationRemover>()

        Logger.info("Registered ${transformers.size} bytecode transformers.")
    }

    private fun run() {
        Logger.info("Starting bytecode transformations.")

        val progressBar = ProgressBarBuilder()
            .setTaskName("Deobfuscator")
            .setInitialMax(transformers.size.toLong())
            .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
            .continuousUpdate()
            .clearDisplayOnFinish()
            .setMaxRenderedLength(100)
            .setUnit(" transforms", 1L)
            .build()

        progressBar.use { p ->
            transformers.forEach { transformer ->
                p.step()
                Logger.info("Running transformer: '${transformer::class.simpleName}'.")
                val start = System.currentTimeMillis()
                transformer.run(group)
                val end = System.currentTimeMillis()
                Logger.info("Completed transformer: '${transformer::class.simpleName}' in ${end - start}ms.")
            }
        }

        Logger.info("Successfully completed all bytecode transformations.")
    }

    private inline fun <reified T : Transformer> register() {
        val inst = T::class.createInstance()
        transformers.add(inst)
    }

    fun isObfuscatedName(name: String): Boolean {
        return name.length <= 2 || (name.length == 3 && listOf("aa", "ab", "ac", "ad", "ae", "af", "ag").any { name.startsWith(it) })
    }

    fun isRenamed(name: String): Boolean {
        return name.startsWith("class") || name.startsWith("method") || name.startsWith("field")
    }
}