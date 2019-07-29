plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation("org.quartz-scheduler:quartz:2.3.1")
    implementation(project(":pleo-antaeus-data"))
    compile(project(":pleo-antaeus-models"))
}