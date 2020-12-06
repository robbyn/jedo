package org.tastefuljava.jedo.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.tastefuljava.jedo.JedoException;

public class BuildContext {
    private final List<Consumer<Mapper>> forwards = new ArrayList<>();
    private final List<Runnable> finalizers = new ArrayList<>();

    public void addForward(Consumer<Mapper> fixer) {
        forwards.add(fixer);
    }

    public void addFinalizer(Runnable r) {
        finalizers.add(r);
    }

    public void fixall(Mapper mapper) {
        for (Consumer<Mapper> fixer: forwards) {
            fixer.accept(mapper);
        }
        forwards.clear();
    }

    public void complete() {
        if (!forwards.isEmpty()) {
            throw new JedoException(forwards.size() + " unresolved forwards");
        }
        for (Runnable r: finalizers) {
            r.run();
        }
    }
}
