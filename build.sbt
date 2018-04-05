import xsbti.compile
inThisBuild(List(
  organization := "com.example",
  version      := "0.1.0-SNAPSHOT",
  autoScalaLibrary := false,
  compileOrder := CompileOrder.JavaThenScala,
  incOptions := incOptions.value.withEnabled(false)
))

crossPaths in ThisBuild := false

incOptions in Global := (incOptions in Global).value.withEnabled(false)

lazy val core = project

lazy val a = project.in(file("acme.a"))

lazy val b = project.in(file("acme.b")).dependsOn(a)

lazy val c = project.in(file("acme.c")).dependsOn(b)

lazy val test = project.dependsOn(c, core)

lazy val root = (project in file(".")).settings(name := "javafileman").aggregate(core, test, a, b, c)
