description = "Kotlin Serialization Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:ir.backend.common"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.translator"))

    runtime(kotlinStdlibWithoutAnnotations())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar = runtimeJar {}

dist(targetName = the<BasePluginConvention>().archivesBaseName + ".jar")

ideaPlugin {
    from(jar)
}
