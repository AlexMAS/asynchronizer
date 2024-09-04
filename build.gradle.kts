@file:Suppress("UnstableApiUsage")

import org.jreleaser.model.Active

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.jreleaser)
}

allprojects {

    group = properties["PROJECT_GROUP"].toString()
    version = properties[project.name + "-version"].toString()
    description = properties[project.name + "-description"].toString()

    repositories {
        mavenCentral()
    }

}

subprojects {

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = rootProject.libs.plugins.jreleaser.get().pluginId)

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Jar> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    dependencies {
        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)

        testCompileOnly(rootProject.libs.lombok)
        testAnnotationProcessor(rootProject.libs.lombok)
    }

    testing {
        suites {
            val test by getting(JvmTestSuite::class) {
                testType.set(TestSuiteType.UNIT_TEST)

                useJUnitJupiter()

                dependencies {
                    implementation.bundle(rootProject.testLibs.bundles.junitAndMockito)
                }
            }
        }
    }

    publishing {

        publications {
            create<MavenPublication>("release") {
                from(components["java"])

                artifactId = project.name

                pom {
                    inceptionYear = "2024"
                    name.set(project.name)
                    description.set(project.description)
                    url.set(project.properties["PROJECT_URL"].toString())

                    issueManagement {
                        url.set(project.properties["PROJECT_ISSUES_URL"].toString())
                    }

                    scm {
                        url.set(project.properties["PROJECT_REPO_URL"].toString())
                        connection.set(project.properties["PROJECT_GIT_URL"].toString())
                        developerConnection.set(project.properties["PROJECT_GIT_URL"].toString())
                    }

                    licenses {
                        license {
                            name.set(project.properties["PROJECT_LICENSE_NAME"].toString())
                            url.set(project.properties["PROJECT_LICENSE_URL"].toString())
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set(project.properties["AUTHOR_ID"].toString())
                            name.set(project.properties["AUTHOR_NAME"].toString())
                            email.set(project.properties["AUTHOR_EMAIL"].toString())
                            url.set(project.properties["AUTHOR_URL"].toString())
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                setUrl(layout.buildDirectory.dir("staging-deploy"))
            }
        }

    }

    jreleaser {
        beforeEvaluate {
            mkdir(layout.buildDirectory.dir("jreleaser"))
        }
        gitRootSearch = true
        project {
            inceptionYear = "2024"
            author(properties["AUTHOR_NAME"].toString())
        }
        signing {
            active = Active.ALWAYS
            armored = true
            verify = true
        }
        release {
            github {
                overwrite = true
                sign = true
                branch = properties["RELEASE_BRANCH"].toString()
                branchPush = properties["RELEASE_BRANCH"].toString()
                changelog {
                    formatted = Active.ALWAYS
                    format = "- {{commitShortHash}} {{commitTitle}}"
                    contributors {
                        enabled = false
                    }
                    labeler {
                        label = "feature"
                        title = "feat:"
                    }
                    labeler {
                        label = "bug"
                        title = "fix:"
                    }
                    labeler {
                        label = "task"
                        title = "chore:"
                    }
                    labeler {
                        label = "task"
                        title = "ci:"
                    }
                    labeler {
                        label = "doc"
                        title = "docs:"
                    }
                    category {
                        title = "🚀 New Features"
                        key = "feature"
                        labels = setOf("feature")
                        order = 1
                    }
                    category {
                        title = "🐞 Bug Fixes"
                        key = "bug"
                        labels = setOf("bug")
                        order = 2
                    }
                    category {
                        title = "🔨 Tasks"
                        key = "task"
                        labels = setOf("task")
                        order = 3
                    }
                    category {
                        title = "📔 Docs"
                        key = "doc"
                        labels = setOf("doc")
                        order = 4
                    }
                    replacer {
                        search = "feat: "
                        replace = ""
                    }
                    replacer {
                        search = "fix: "
                        replace = ""
                    }
                    replacer {
                        search = "chore: "
                        replace = ""
                    }
                    replacer {
                        search = "ci: "
                        replace = ""
                    }
                    replacer {
                        search = "docs: "
                        replace = ""
                    }
                }
            }
        }
        deploy {
            maven {
                mavenCentral.create(properties["RELEASE_DEPLOYER_NAME"].toString()) {
                    active = Active.ALWAYS
                    url = properties["RELEASE_DEPLOYER_URL"].toString()
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().toString())
                    setAuthorization("Basic")
                    retryDelay = 60
                }
            }
        }
    }

}
