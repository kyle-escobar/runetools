plugins {
    application
}

dependencies {
    implementation("org.tinylog:tinylog-api-kotlin:_")
    implementation("org.tinylog:tinylog-impl:_")
    implementation("com.google.guava:guava:_")
    implementation("com.github.ajalt.clikt:clikt:_")
    implementation("com.github.kyle-escobar:byteflow:1.1.1")
    //implementation("dev.kyleescobar.byteflow:byteflow:1.1.1")
}

application {
    mainClass.set("com.github.kyleescobar.runetools.deobfuscator.Deobfuscator")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    mainClass.set("com.github.kyleescobar.runetools.deobfuscator.Deobfuscator")
}