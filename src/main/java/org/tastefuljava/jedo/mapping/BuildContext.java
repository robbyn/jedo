package org.tastefuljava.jedo.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.tastefuljava.jedo.JedoException;

public class BuildContext {
    private final Map<Class<?>, List<Consumer<ClassMapper>>> forwards
            = new LinkedHashMap<>();
    private final List<Runnable> finalizers = new ArrayList<>();

    public void addForwardClassRef(Class<?> clazz, Consumer<ClassMapper> fixer) {
        List<Consumer<ClassMapper>> fixers = forwards.get(clazz);
        if (fixers == null) {
            fixers = new ArrayList<>();
            forwards.put(clazz, fixers);
        }
        fixers.add(fixer);
    }

    public void addFinalizer(Runnable r) {
        finalizers.add(r);
    }

    public void fixall(ClassMapper cm) {
        List<Consumer<ClassMapper>> fixers = forwards.remove(cm.getType());
        if (fixers != null) {
            for (Consumer<ClassMapper> fixer: fixers) {
                fixer.accept(cm);
            }
        }
    }

    public void complete() {
        if (!forwards.isEmpty()) {
            StringBuilder buf = new StringBuilder(
                    "Unresolved forward class references: ");
            boolean first = true;
            for (Class<?> c: forwards.keySet()) {
                if (first) {
                    first = false;
                } else {
                    buf.append(", ");
                }
                buf.append(c.getName());
            }
            throw new JedoException(buf.toString());
        }
        for (Runnable r: finalizers) {
            r.run();
        }
    }
}
