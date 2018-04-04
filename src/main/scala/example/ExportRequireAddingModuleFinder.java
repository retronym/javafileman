package example;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ExportRequireAddingModuleFinder implements ModuleFinder {
    private ModuleFinder delegate;
    private ExportRequireAdder patcher;

    public ExportRequireAddingModuleFinder(ModuleFinder delegate, ExportRequireAdder adder) {
        this.delegate = delegate;
        this.patcher = adder;
    }

    @Override
    public Optional<ModuleReference> find(String name) {
        return delegate.find(name).map(this::patch);
    }

    @Override
    public Set<ModuleReference> findAll() {
        return delegate.findAll().stream().map(this::patch).collect(Collectors.toSet());
    }

    private ModuleReference patch(ModuleReference delegate) {
        return new ModuleReference(patcher.patch(delegate.descriptor()), delegate.location().orElse(null)) {
            @Override
            public ModuleReader open() throws IOException {
                return delegate.open();
            }
        };
    }
}
