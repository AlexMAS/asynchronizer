rootProject.name = "asynchronizer"

include(":asynchronizer")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {

            // Plugins
            plugin("jreleaser", "org.jreleaser").version("1.13.1")

            // Common Utils

            val lombok = "1.18.34"
            val slf4j = "2.0.16"

            library("lombok", "org.projectlombok:lombok:${lombok}")
            library("slf4j-api", "org.slf4j:slf4j-api:${slf4j}")
        }

        create("testLibs") {

            // Functional Tests

            val assertj = "3.26.3"
            val junit = "5.11.0"
            val junitSuite = "1.11.0"
            val mockito = "5.13.0"

            library("assertj", "org.assertj:assertj-core:${assertj}")
            library("junit-jupiter-api", "org.junit.jupiter:junit-jupiter-api:${junit}")
            library("junit-jupiter-engine", "org.junit.jupiter:junit-jupiter-engine:${junit}")
            library("junit-jupiter-params", "org.junit.jupiter:junit-jupiter-params:${junit}")
            library("junit-platform-suite-engine", "org.junit.platform:junit-platform-suite-engine:${junitSuite}")
            library("mockito-core", "org.mockito:mockito-core:${mockito}")
            library("mockito-junit-jupiter", "org.mockito:mockito-junit-jupiter:${mockito}")

            bundle(
                "junitAndMockito", listOf(
                    "assertj",
                    "junit-jupiter-api",
                    "junit-jupiter-engine",
                    "junit-jupiter-params",
                    "junit-platform-suite-engine",
                    "mockito-core",
                    "mockito-junit-jupiter"
                )
            )

            // Benchmark Tests

            val jmhPlugin = "0.6.8"
            val jmh = "1.36"

            plugin("jmh", "me.champeau.jmh").version(jmhPlugin)
            library("jmh-core", "org.openjdk.jmh:jmh-core:${jmh}")
            library("jmh-generator-annprocess", "org.openjdk.jmh:jmh-generator-annprocess:${jmh}")

            bundle(
                "jmh", listOf(
                    "jmh-core",
                    "jmh-generator-annprocess"
                )
            )
        }
    }
}
