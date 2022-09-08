plugins {
    application
}

dependencies {
    implementation(project(":asm"))
    implementation("org.tinylog:tinylog-api-kotlin:_")
    implementation("org.tinylog:tinylog-impl:_")
    implementation("com.google.guava:guava:_")
    implementation("com.github.ajalt.clikt:clikt:_")
}

application {
    mainClass.set("dev.kyleescobar.runetools.deobfuscator.Deobfuscator")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    mainClass.set("dev.kyleescobar.runetools.deobfuscator.Deobfuscator")
}