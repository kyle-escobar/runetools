plugins {
    application
}

dependencies {
    implementation(project(":asm"))
    implementation("org.tinylog:tinylog-api-kotlin:_")
    implementation("org.tinylog:tinylog-impl:_")
    implementation("com.google.guava:guava:_")
    implementation("com.github.ajalt.clikt:clikt:_")
    implementation("me.tongfei:progressbar:_")
    implementation("dev.reimer:progressbar-ktx:0.1.0")
}

application {
    mainClass.set("dev.kyleescobar.runetools.deobfuscator.Deobfuscator")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}