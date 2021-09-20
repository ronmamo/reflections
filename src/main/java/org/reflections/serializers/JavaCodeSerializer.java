package org.reflections.serializers;

import org.reflections.Reflections;
import org.reflections.scanners.TypeElementsScanner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** source code serialization for {@link org.reflections.Reflections} <pre>{@code reflections.save(file, new JavaCodeSerializer())}</pre>
 * <p></p>an example of produced java source:
 * <pre>{@code
 * public interface MyTestModelStore {
 *   interface org {
 *     interface reflections {
 *       interface TestModel$C4 {
 *         interface fields {
 *           interface f1 {}
 *           interface f2 {}
 *         }
 *         interface methods {
 *           interface m1 {}
 *           interface add {}
 *         }
 *         interface annotations {
 *           ...
 *         }
 *       }
 *     }
 *   }
 * }
 * }</pre>
 * <p>this allows strongly typed access by fqn to type elements - packages, classes, annotations, fields and methods:
 * <pre>{@code MyTestModelStore.org.reflections.TestModel$C1.methods.m1.class}</pre>
 * <p>depends on {@link org.reflections.scanners.TypeElementsScanner} configured
 */
public class JavaCodeSerializer implements Serializer {

    private static final String pathSeparator = "_";
    private static final String doubleSeparator = "__";
    private static final String dotSeparator = ".";
    private static final String arrayDescriptor = "$$";
    private static final String tokenSeparator = "_";

    private StringBuilder sb;
    private List<String> prevPaths;
    private int indent;

    public Reflections read(InputStream inputStream) {
        throw new UnsupportedOperationException("read is not implemented on JavaCodeSerializer");
    }

    /**
     * serialize and save to java source code
     * @param name should be in the pattern {@code path/path/path/package.package.classname},
     */
    public File save(Reflections reflections, String name) {
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1); //trim / at the end
        }

        //prepare file
        String filename = name.replace('.', '/').concat(".java");
        File file = Serializer.prepareFile(filename);

        //get package and class names
        String packageName;
        String className;
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            packageName = "";
            className = name.substring(name.lastIndexOf('/') + 1);
        } else {
            packageName = name.substring(name.lastIndexOf('/') + 1, lastDot);
            className = name.substring(lastDot + 1);
        }

        //generate
        try {
            sb = new StringBuilder();
            sb.append("//generated using Reflections JavaCodeSerializer").append(" [").append(new Date()).append("]").append("\n");
            if (packageName.length() != 0) {
                sb.append("package ").append(packageName).append(";\n");
                sb.append("\n");
            }
            sb.append("public interface ").append(className).append(" {\n\n");
            toString(reflections);
            sb.append("}\n");

            Files.write(new File(filename).toPath(), sb.toString().getBytes(Charset.defaultCharset()));

        } catch (IOException e) {
            throw new RuntimeException();
        }

        return file;
    }

    private void toString(Reflections reflections) {
        Map<String, Set<String>> map = reflections.getStore().get(TypeElementsScanner.class.getSimpleName());
        prevPaths = new ArrayList<>();
        indent = 1;

        map.keySet().stream().sorted().forEach(fqn -> {
            List<String> typePaths = Arrays.asList(fqn.split("\\."));
            String className = fqn.substring(fqn.lastIndexOf('.') + 1);
            List<String> fields = new ArrayList<>();
            List<String> methods = new ArrayList<>();
            List<String> annotations = new ArrayList<>();
            map.get(fqn).stream().sorted().forEach(element -> {
                if (element.startsWith("@")) {
                    annotations.add(element.substring(1));
                } else if (element.contains("(")) {
                    if (!element.startsWith("<")) {
                        int i = element.indexOf('(');
                        String name = element.substring(0, i);
                        String params = element.substring(i + 1, element.indexOf(")"));
                        String paramsDescriptor = params.length() != 0 ? tokenSeparator + params.replace(dotSeparator, tokenSeparator).replace(", ", doubleSeparator).replace("[]", arrayDescriptor) : "";
                        methods.add(!methods.contains(name) ? name : name + paramsDescriptor);
                    }
                } else if (!element.isEmpty()) {
                    fields.add(element);
                }
            });

            int i = indentOpen(typePaths, prevPaths);
            addPackages(typePaths, i);
            addClass(typePaths, className);
            addFields(typePaths, fields);
            addMethods(typePaths, fields, methods);
            addAnnotations(typePaths, annotations);

            prevPaths = typePaths;
        });

        indentClose(prevPaths);
    }

    protected int indentOpen(List<String> typePaths, List<String> prevPaths) {
        int i = 0;
        while (i < Math.min(typePaths.size(), prevPaths.size()) && typePaths.get(i).equals(prevPaths.get(i))) {
            i++;
        }
        for (int j = prevPaths.size(); j > i; j--) {
            sb.append(indent(--indent)).append("}\n");
        }
        return i;
    }

    protected void indentClose(List<String> prevPaths) {
        for (int j = prevPaths.size(); j >= 1; j--) {
            sb.append(indent(j)).append("}\n");
        }
    }

    protected void addPackages(List<String> typePaths, int i) {
        for (int j = i; j < typePaths.size() - 1; j++) {
            sb.append(indent(indent++)).append("interface ").append(uniqueName(typePaths.get(j), typePaths, j)).append(" {\n");
        }
    }

    protected void addClass(List<String> typePaths, String className) {
        sb.append(indent(indent++)).append("interface ").append(uniqueName(className, typePaths, typePaths.size() - 1)).append(" {\n");
    }

    protected void addFields(List<String> typePaths, List<String> fields) {
        if (!fields.isEmpty()) {
            sb.append(indent(indent++)).append("interface fields {\n");
            for (String field : fields) {
                sb.append(indent(indent)).append("interface ").append(uniqueName(field, typePaths)).append(" {}\n");
            }
            sb.append(indent(--indent)).append("}\n");
        }
    }

    protected void addMethods(List<String> typePaths, List<String> fields, List<String> methods) {
        if (!methods.isEmpty()) {
            sb.append(indent(indent++)).append("interface methods {\n");
            for (String method : methods) {
                String methodName = uniqueName(method, fields);
                sb.append(indent(indent)).append("interface ").append(uniqueName(methodName, typePaths)).append(" {}\n");
            }
            sb.append(indent(--indent)).append("}\n");
        }
    }

    protected void addAnnotations(List<String> typePaths, List<String> annotations) {
        if (!annotations.isEmpty()) {
            sb.append(indent(indent++)).append("interface annotations {\n");
            for (String annotation : annotations) {
                sb.append(indent(indent)).append("interface ").append(uniqueName(annotation, typePaths)).append(" {}\n");
            }
            sb.append(indent(--indent)).append("}\n");
        }
    }

    private String uniqueName(String candidate, List<String> prev, int offset) {
        String normalized = normalize(candidate);
        for (int i = 0; i < offset; i++) {
            if (normalized.equals(prev.get(i))) {
                return uniqueName(normalized + tokenSeparator, prev, offset);
            }
        }
        return normalized;
    }

    private String normalize(String candidate) {
        return candidate.replace(dotSeparator, pathSeparator);
    }

    private String uniqueName(String candidate, List<String> prev) {
        return uniqueName(candidate, prev, prev.size());
    }

    private String indent(int times) {
        return IntStream.range(0, times).mapToObj(i -> "  ").collect(Collectors.joining());
    }
}
