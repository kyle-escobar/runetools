plugins {
    java
    id("io.freefair.lombok") version "6.5.1"
}

dependencies {
    api("org.ow2.asm:asm:_")
    api("org.ow2.asm:asm-commons:_")
    api("org.ow2.asm:asm-util:_")
    api("org.ow2.asm:asm-tree:_")
    api("com.google.guava:guava:_")
    implementation("org.slf4j:slf4j-simple:_")
    implementation("org.slf4j:slf4j-api:_")
    implementation("com.google.code.gson:gson:_")
}