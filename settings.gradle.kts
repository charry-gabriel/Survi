rootProject.name = "Survi"

val miubyLibDir = file("../MiubyLib")
if (miubyLibDir.exists()) {
    includeBuild("../MiubyLib") {
        dependencySubstitution {
            substitute(module("com.github.charry-gabriel:MiubyLib")).using(project(":"))
        }
    }
}