package org.reflections.scanners;

import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import org.reflections.Store;
import org.reflections.adapters.MetadataAdapter;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.reflections.util.Utils.join;

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
                int length = table != null ? table.tableLength() : 0;
                int i = Modifier.isStatic(((MethodInfo) method).getAccessFlags()) ? 0 : 1; //skip this
                if (i < length) {
                    List<String> names = new ArrayList<>(length - i);
                    while (i < length) names.add(((MethodInfo) method).getConstPool().getUtf8Info(table.nameIndex(i++)));
                    put(store, key, join(names, ", "));
                }
            }
        }
    }
}
