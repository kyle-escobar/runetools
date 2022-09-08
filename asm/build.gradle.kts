plugins {
    `java`
}

dependencies {
    implementation("org.ow2.asm:asm:_")
    implementation("org.ow2.asm:asm-commons:_")
    implementation("org.ow2.asm:asm-util:_")
    implementation("org.ow2.asm:asm-tree:_")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    compileJava {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    }
}

sourceSets {
    main {
        java.srcDir("src/main/java")
    }
}