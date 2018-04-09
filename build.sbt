
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

lazy val a = project.in(file("acme.a")).enablePlugins(JpmsPlugin).settings(
  jpmsModuleName := "acme.a"
)

lazy val b = project.in(file("acme.b")).enablePlugins(JpmsPlugin).dependsOn(a).settings(
  jpmsModuleName := "acme.b"
)

lazy val c = project.in(file("acme.c")).enablePlugins(JpmsPlugin).dependsOn(b).settings(
  jpmsModuleName := "acme.c"
)

lazy val tests = project.dependsOn(a, b, c, core).settings(
  libraryDependencies += "junit" % "junit" % "4.12" % Test
)

lazy val root = (project in file(".")).settings(name := "javafileman").aggregate(core, tests, a, b, c)
