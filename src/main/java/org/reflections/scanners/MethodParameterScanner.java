package org.reflections.scanners;

import org.reflections.adapters.MetadataAdapter;

import java.util.List;

/** scans methods/constructors and indexes parameters, return type and parameter annotations */
@SuppressWarnings("unchecked")
public class MethodParameterScanner extends AbstractScanner {

    @Override
    public void scan(Object cls) {
        final MetadataAdapter md = getMetadataAdapter();

        for (Object method : md.getMethods(cls)) {

            String signature = md.getParameterNames(method).toString();
            if (acceptResult(signature)) {
                getStore().put(signature, md.getMethodFullKey(cls, method));
            }

            String returnTypeName = md.getReturnTypeName(method);
            if (acceptResult(returnTypeName)) {
                getStore().put(returnTypeName, md.getMethodFullKey(cls, method));
            }

            List<String> parameterNames = md.getParameterNames(method);
            for (int i = 0; i < parameterNames.size(); i++) {
                for (Object paramAnnotation : md.getParameterAnnotationNames(method, i)) {
                    if (acceptResult((String) paramAnnotation)) {
                        getStore().put((String) paramAnnotation, md.getMethodFullKey(cls, method));
                    }
                }
            }
        }
    }
}
