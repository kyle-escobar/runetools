package com.github.kyleescobar.runetools.deobfuscator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file

fun main(args: Array<String>) = DeobfuscatorCommand().main(args)

class DeobfuscatorCommand : CliktCommand(
    name = "deobfuscate",
    help = "Deobfuscates the gamepack JAR for the Jagex Old School RuneScape client.",
    printHelpOnEmptyArgs = true,
    invokeWithoutSubcommand = true
) {

    private val inputJar by argument(name = "input jar", help = "Path to the input file to deobfuscate.").file(mustExist = true, canBeDir = false)
    private val outputJar by argument(name = "output jar", help = "Path to the output file to save classes.").file(canBeDir = false)

    override fun run() = Deobfuscator(inputJar, outputJar).run()

}