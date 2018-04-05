package scala.reflect.internal.jpms.test;

import scala.reflect.internal.jpms.ExportRequireAdder;
import scala.reflect.internal.jpms.ExportRequireAddingModuleFinder;
import scala.reflect.internal.jpms.FixedModuleFinder;
import scala.reflect.internal.jpms.ModuleFinderAndFileManager;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ResolvedModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class JavaFileManagerRelease {

    // Create a dummy module name to accumulate `--add-exports=some.module/some.module.internal.package=ALL-UNNAMED
    private static final String UNNAMED_MODULE_NAME = "_UNNAMED_";
    private static final Path ROOT = Paths.get(".").toAbsolutePath();

    public static void main(String... args) throws IOException {
        for (Optional<String> release : releases) {
            test1(release);
        }
    }

    private static List<Optional<String>> releases = List.of(Optional.empty(), Optional.of("9"));

    public static void test1(Optional<String> release) throws IOException {
        ExportRequireAdder adder = new ExportRequireAdder() {
            @Override
            public Iterable<ModuleDescriptor.Exports> addExports(String moduleName) {
                switch (moduleName) {
                    case "jdk.compiler":
                        // --add-exports com.sun.tools.javac.platform/com.sun.tools.javac.platform=acme.mod2
                        return mkExport("com.sun.tools.javac.platform", "acme.mod2");
                    case "jdk.incubator.httpclient":
                        // --add-exports jdk.incubator.httpclient/jdk.incubator.http.internal.common=acme.mod2
                        return mkExport("jdk.incubator.http.internal.common", "acme.mod2");
                    case "jdk.jfr":
                        // --add-exports jdk.jfr/jdk.jfr.internal.management=ALL-UNNAMED
                        return mkExport("jdk.jfr.internal.management", UNNAMED_MODULE_NAME);
                    default:
                        return List.of();
                }
            }

            @Override
            public Iterable<String> addReads(String moduleName) {
                if (moduleName.equals("acme.mod2")) {
                    // --add-reads acme.mod2=jdk.incubator.httpclient
                    return List.of("jdk.incubator.httpclient");
                } else {
                    return List.of();
                }
            }
        };

        ModuleFinderAndFileManager result = resolveModules(release, adder, (x -> x.handleOption("--classpath", List.of("/Users/jz/scala/2.12/lib/scala-library.jar").iterator())));
    }

    public static void test2(Optional<String> release) throws IOException {
        ExportRequireAdder adder = new ExportRequireAdder();
        Consumer<StandardJavaFileManager> optionAdder = x -> {
//            x.handleOption("--module-path", List.of(ROOT.resolve("a/")).iterator());
        };
        ModuleFinderAndFileManager result = resolveModules(release, adder, optionAdder);
    }

    private static ModuleFinderAndFileManager resolveModules(Optional<String> release1, ExportRequireAdder adder, Consumer<StandardJavaFileManager> optionAdder) throws IOException {
        System.out.println("--release: " + release1);
        ModuleFinderAndFileManager finderAndFileManager = ModuleFinderAndFileManager.get(release1, optionAdder);

        List<ModuleDescriptor> sourceModules = sourceModules(adder);


        ArrayList<ModuleDescriptor> sourceAndUnnamed = new ArrayList<>(sourceModules);
        sourceAndUnnamed.add(ModuleDescriptor.newModule(UNNAMED_MODULE_NAME).requires("java.base").build());
        ModuleFinder fromSourceFinder = FixedModuleFinder.newModuleFinder(sourceAndUnnamed);


        // Resolve the module graph.
        // `fromSourceFinder` is passed as the `before` finder to take precendence over, rather than clash with, a module-info.class in the
        // output directory.
        Configuration configuration = Configuration.empty().resolve(fromSourceFinder, new ExportRequireAddingModuleFinder(finderAndFileManager.moduleFinder(), adder), List.of("acme.mod2"));

        String resultString = configuration.modules().stream().map(Objects::toString).collect(Collectors.joining(", ", "modules: ", ""));
        System.out.println(resultString);
        ResolvedModule root = configuration.findModule("acme.mod2").get();

        // Interrogate the resolved configuration to find out what packages are read by && exported to some module.
        List<ModuleDescriptor.Exports> exportedPackagesOfReads = root.reads().stream().flatMap(x -> x.reference().descriptor().exports().stream().filter(y -> !y.isQualified() || y.targets().contains("acme.mod2"))).collect(Collectors.toList());
        System.out.println(exportedPackagesOfReads.stream().map(Objects::toString).collect(Collectors.joining(", ", "exported to root module: ", "")));

        StandardJavaFileManager fileManager = finderAndFileManager.fileManager();
        fileManager.getLocationAsPaths(StandardLocation.PLATFORM_CLASS_PATH);
        fileManager.getLocationAsPaths(StandardLocation.CLASS_PATH);
        Iterable<Set<JavaFileManager.Location>> x = fileManager.listLocationsForModules(StandardLocation.SYSTEM_MODULES);
        System.out.println(x);
        // fileManager.inferModuleName(fileManager.listLocationsForModules(StandardLocation.SYSTEM_MODULES).iterator().next().iterator().next())
        fileManager.list(fileManager.listLocationsForModules(StandardLocation.SYSTEM_MODULES).iterator().next().iterator().next(), "", Set.of(JavaFileObject.Kind.CLASS), true);
        return finderAndFileManager;
    }

    private static List<ModuleDescriptor> sourceModules(ExportRequireAdder adder) {
        // This would be driven by the --add-reads / --add-exports command line options.

        // One more more module descriptors could be created programatically based on parsing/typechecking module-info.java
        // Will we be able to do this safely without triggering cycles?
        Set<ModuleDescriptor.Requires.Modifier> transitive = Set.of(ModuleDescriptor.Requires.Modifier.TRANSITIVE);
        ModuleDescriptor mod0 = adder.patch(ModuleDescriptor.newModule("acme.mod0").exports("acme.mod0").requires(transitive, "java.xml").build());
        ModuleDescriptor mod1 = adder.patch(ModuleDescriptor.newModule("acme.mod1").exports("acme.mod1").requires(transitive, "acme.mod0").build());
        ModuleDescriptor mod2 = adder.patch(ModuleDescriptor.newModule("acme.mod2").exports("acme.mod2").requires("acme.mod1").requires("jdk.compiler").build());
        return List.of(mod0, mod1, mod2);
    }
}
