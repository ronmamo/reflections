package org.reflections.scanners;

import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import org.reflections.Store;
import org.reflections.adapters.MetadataAdapter;

import java.lang.reflect.Modifier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** scans methods/constructors and indexes parameter names */
@SuppressWarnings("unchecked")
public class MethodParameterNamesScanner extends AbstractScanner {

    @Override
    public void scan(Object cls, Store store) {
        final MetadataAdapter md = getMetadataAdapter();

        for (Object method : md.getMethods(cls)) {
            String key = md.getMethodFullKey(cls, method);
            if (acceptResult(key)) {
                CodeAttribute codeAttribute = ((MethodInfo) method).getCodeAttribute();
                LocalVariableAttribute table = codeAttribute != null ? (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag) : null;
                int length = md.getParameterNames(method).size();
                if (length > 0) {
                    int shift = Modifier.isStatic(((MethodInfo) method).getAccessFlags()) ? 0 : 1; //skip this
                    String join = IntStream.range(shift, length + shift)
                            .mapToObj(i -> ((MethodInfo) method).getConstPool().getUtf8Info(table.nameIndex(i)))
                            .filter(name -> !name.startsWith("this$"))
                            .collect(Collectors.joining(", "));
                    if (!join.isEmpty()) {
                        put(store, key, join);
                    }
                }
            }
        }
    }
}
