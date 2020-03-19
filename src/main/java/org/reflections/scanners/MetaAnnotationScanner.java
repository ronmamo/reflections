package org.reflections.scanners;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;
import org.reflections.Store;

import java.util.List;

@SuppressWarnings({"unchecked"})
public class MetaAnnotationScanner extends AbstractScanner {
    public void scan(final Object cls, Store store) {
        if(cls instanceof ClassFile) {
            ClassFile c = (ClassFile) cls;
            boolean isAnnotation = (c.getAccessFlags() & AccessFlag.ANNOTATION) > 0;

            if(!isAnnotation)
                return;

            List<String> metaAnnotations = getMetadataAdapter().getClassAnnotationNames(cls);

            String clsName = getMetadataAdapter().getClassName(cls);

            metaAnnotations.add(clsName);

            for (String metaAnnotation : metaAnnotations) {
                if (acceptResult(metaAnnotation)) {
                    put(store, metaAnnotation, clsName);
                }
            }
        }
    }
}
