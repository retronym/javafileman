package example

import java.io.IOException
import java.net.URI
import java.nio.charset.Charset
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.spi.FileSystemProvider
import java.{lang, util}
import java.util.{Collections, EnumSet, Locale}
import javax.tools._


import scala.collection.JavaConverters.{asJavaCollectionConverter, asJavaIterableConverter, asScalaIteratorConverter, iterableAsScalaIterableConverter}
import scala.collection.{JavaConverters, mutable}

object Hello {
  def main(args: Array[String]): Unit = {
    FileSystemProvider.installedProviders()
    val archiveFsProvider = FileSystemProvider.installedProviders.asScala.find(_.getScheme == "jar").get


    def mkFileManager(release: Option[String], modulePath: String) = {
      val compiler = javax.tools.ToolProvider.getSystemJavaCompiler
      val fileManager = compiler.getStandardFileManager(_ => (), Locale.getDefault, Charset.defaultCharset()).asInstanceOf[StandardJavaFileManager]
      release match {
        case Some(x @ ("6" | "7" | "8")) =>
          require(x.length == 1, x)
          val javaHome = System.getProperty("java.home")
          var file = Paths.get(javaHome).resolve("lib").resolve("ct.sym")
          val roots = Files.newDirectoryStream(FileSystems.newFileSystem(file, null).getRootDirectories.iterator().next).iterator().asScala.toList
          val subset = roots.filter(_.getFileName.toString.contains(x))
          fileManager.setLocationFromPaths(StandardLocation.PLATFORM_CLASS_PATH, subset.asJavaCollection)
        case _ =>
      }
      fileManager.handleOption("-classpath", util.Arrays.asList("/tmp/bar:/tmp/a.jar").iterator())
      fileManager.handleOption("--module-path", util.Arrays.asList(modulePath).iterator())
      fileManager
    }

    def packageName(s: String) = {
      if (s.lastIndexOf(".") <= 0) "" else s.substring(0, s.lastIndexOf("."))
    }
    def simpleName(s: String) = {
      if (s.lastIndexOf(".") <= 0) s else s.substring(s.lastIndexOf(".") + 1)
    }
    val locations = List(StandardLocation.PLATFORM_CLASS_PATH, StandardLocation.CLASS_PATH, StandardLocation.SYSTEM_MODULES, StandardLocation.MODULE_PATH)
    val classOnly = util.EnumSet.of(JavaFileObject.Kind.CLASS)

    val release = "8"

    def listPackages(manager: StandardJavaFileManager) = {
      val packages = new java.util.HashSet[String]()
      for (elem <- locations; path <- manager.getLocationAsPaths(elem).iterator().asScala) {
        def visitor(root: Path) = new SimpleFileVisitor[Path] {
          override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = {
            val pack = root.relativize(dir).toString.replace('/', '.')
            packages.add(pack)
            FileVisitResult.CONTINUE
          }
        }
        println(path)
        try {
          val attrs = Files.readAttributes(path, classOf[BasicFileAttributes])
          if (elem == StandardLocation.PLATFORM_CLASS_PATH && path.toString.endsWith("lib/modules")) {
            val fs = FileSystems.getFileSystem(URI.create("jrt:/"))
            val dir: Path = fs.getPath("/packages")
            val ps = Files.newDirectoryStream(dir).iterator().asScala
            def lookup(pack: Path): Seq[Path] = {
              Files.list(pack).iterator().asScala.map(l => if (Files.isSymbolicLink(l)) Files.readSymbolicLink(l) else l).toList
            }

            ps.foreach { p =>
              val module = lookup(p)
              packages.add(p.toString.stripPrefix("/packages/"))
            }
          } else if (attrs.isDirectory) {
            Files.walkFileTree(path, util.EnumSet.noneOf(classOf[FileVisitOption]), Int.MaxValue, visitor(path))
          } else {
            val fs = if (release != null && path.toString.endsWith(".jar")) {
              val env = JavaConverters.mapAsJavaMap(Map("multi-release" -> release))
              archiveFsProvider.newFileSystem(path, env)
            } else {
              FileSystems.newFileSystem(path, null)
            }
            try {
              for (root <- fs.getRootDirectories.iterator().asScala) {
                Files.walkFileTree(root, util.EnumSet.noneOf(classOf[FileVisitOption]), Int.MaxValue, visitor(root))
              }
            } finally {
              fs.close()
            }
          }

        } catch {
          case ex: IOException =>
        }
      }
      packages
    }

    def list(manager: StandardJavaFileManager, pack: String): List[JavaFileObject] = {
      manager.list(StandardLocation.PLATFORM_CLASS_PATH, pack, util.EnumSet.of(JavaFileObject.Kind.CLASS), false).iterator().asScala.toList
    }


    val defaultFileManage = mkFileManager(None, "jdk.compiler")
    val release6FileManager = mkFileManager(Some("6"), "jdk.compiler")
    try {
      for (manager <- List(defaultFileManage, release6FileManager)) {
        println(listPackages(manager))
        println("-" * 60)
        println("com.sun.tools.javac.file")
        println(list(manager, "com.sun.tools.javac.file"))
      }
    } finally {
      defaultFileManage.close()
      release6FileManager.close()
    }
  }
}
