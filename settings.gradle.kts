plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ai-cli-integration"

include(
    ":core",
    ":jetbrains-extension",
    ":vscode-extension",
    ":web-ui", 
    ":desktop-ui"
)
