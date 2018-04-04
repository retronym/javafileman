package example;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.lang.module.*;
import java.util.*;
import java.util.stream.Collectors;


public class JavaFileManagerRelease {

    // Create a dummy module name to accumulate `--add-exports=some.module/some.module.internal.package=ALL-UNNAMED
    private static final String UNNAMED_MODULE_NAME = "_UNNAMED_";

    public static void main(String... args) throws IOException {
        resolveModules(null);
        resolveModules("9");
    }

    private static void resolveModules(String release1) throws IOException {
        System.out.println("--release: " + release1);
        ModuleFinderAndFileManager finderAndFileManager = ModuleFinderAndFileManager.get(Optional.ofNullable(release1), (x -> x.handleOption("--classpath", List.of("/Users/jz/scala/2.12/lib/scala-library.jar").iterator())));

        // This would be driven by the --add-reads / --add-exports command line options.
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

            private Iterable<ModuleDescriptor.Exports> mkExport(String source, String target) {
                return ModuleDescriptor.newModule("dummy").exports(Set.of(), source, Collections.singleton(target)).build().exports();
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

        // One more more module descriptors could be created programatically based on parsing/typechecking module-info.java
        // Will we be able to do this safely without triggering cycles?
        Set<ModuleDescriptor.Requires.Modifier> transitive = Set.of(ModuleDescriptor.Requires.Modifier.TRANSITIVE);
        String rootModule = "acme.mod2";
        ModuleDescriptor mod0 = adder.patch(ModuleDescriptor.newModule("acme.mod0").exports("acme.mod0").requires(transitive, "java.xml").build());
        ModuleDescriptor mod1 = adder.patch(ModuleDescriptor.newModule("acme.mod1").exports("acme.mod1").requires(transitive, "acme.mod0").build());
        ModuleDescriptor mod2 = adder.patch(ModuleDescriptor.newModule(rootModule).exports(rootModule).requires("acme.mod1").requires("jdk.compiler").build());

        ModuleDescriptor unnamed = adder.patch(ModuleDescriptor.newModule(UNNAMED_MODULE_NAME).requires("java.base").build());
        ModuleFinder fromSourceFinder = FixedModuleFinder.newModuleFinder(List.of(mod0, mod1, mod2, unnamed));


        // Resolve the module graph.
        // `fromSourceFinder` is passed as the `before` finder to take precendence over, rather than clash with, a module-info.class in the
        // output directory.
        Configuration configuration = Configuration.empty().resolve(fromSourceFinder, new ExportRequireAddingModuleFinder(finderAndFileManager.moduleFinder(), adder), List.of(rootModule));

        String resultString = configuration.modules().stream().map(Objects::toString).collect(Collectors.joining(", ", "modules: ", ""));
        System.out.println(resultString);
        ResolvedModule root = configuration.findModule(rootModule).get();

        // Interrogate the resolved configuration to find out what packages are read by && exported to some module.
        List<ModuleDescriptor.Exports> exportedPackagesOfReads = root.reads().stream().flatMap(x -> x.reference().descriptor().exports().stream().filter(y -> !y.isQualified() || y.targets().contains(rootModule))).collect(Collectors.toList());
        System.out.println(exportedPackagesOfReads.stream().map(Objects::toString).collect(Collectors.joining(", ", "exported to root module: ", "")));

        StandardJavaFileManager fileManager = finderAndFileManager.fileManager();
        fileManager.getLocationAsPaths(StandardLocation.PLATFORM_CLASS_PATH);
        fileManager.getLocationAsPaths(StandardLocation.CLASS_PATH);
        Iterable<Set<JavaFileManager.Location>> x = fileManager.listLocationsForModules(StandardLocation.SYSTEM_MODULES);
        System.out.println(x);
        // fileManager.inferModuleName(fileManager.listLocationsForModules(StandardLocation.SYSTEM_MODULES).iterator().next().iterator().next())
        fileManager.list(fileManager.listLocationsForModules(StandardLocation.SYSTEM_MODULES).iterator().next().iterator().next(), "", Set.of(JavaFileObject.Kind.CLASS), true);
    }
}
