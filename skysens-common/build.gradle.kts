// ---------------------------------------------------------------------------
// skysens-common — PURE JAVA. No Minecraft, no Fabric, no Spring (constraint 2).
//
// This module is compiled by both sides of the split, so its classpath must
// contain nothing that only one side can provide.
//
// Everything here is `compileOnly` on purpose:
//  * gson  — the mod uses the Gson that Minecraft already bundles (constraint 5),
//            and the server uses Jackson from the Spring web starter. Putting gson
//            on this module's RUNTIME classpath would let `com/google/gson/**` ride
//            into the shadow jar via `shadowImpl(project(":skysens-common"))`.
//            `@Expose` is RUNTIME-retention, but the annotation type is supplied by
//            Minecraft on the mod side and simply ignored by Jackson server-side.
//  * annotations — JetBrains @Nullable/@NotNull; documentation only, not needed at
//            runtime, and already present on the mod classpath.
// ---------------------------------------------------------------------------

plugins {
    `java-library`
    id("io.freefair.lombok")
}

val gsonVersion: String = (project.findProperty("gson_version") as? String) ?: "2.11.0"
val annotationsVersion: String =
    (project.findProperty("jetbrains_annotations_version") as? String) ?: "26.0.2"

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("com.google.code.gson:gson:$gsonVersion")
    compileOnly("org.jetbrains:annotations:$annotationsVersion")
}
