package org.reflections.scanners;

import com.google.common.base.Joiner;
import javassist.*;
import javassist.bytecode.MethodInfo;
import javassist.expr.*;
import org.reflections.ReflectionsException;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/** scans methods/constructors/fields usage
 * <p><i> depends on {@link org.reflections.adapters.JavassistAdapter} configured </i>*/
@SuppressWarnings("unchecked")
public class MemberUsageScanner extends AbstractScanner {
    private static final ClassPool CLASS_POOL;
    /** unless null, will be used for clearing the {@link javassist.ClassPool} cache */
    @Nullable public static Method removeCached;

    static {
        try {
            CLASS_POOL = ClassPool.getDefault(); //fail if javassist library absent from class path
            (removeCached = ClassPool.class.getDeclaredMethod("removeCached", String.class)).setAccessible(true);
        } catch (Throwable e) {
            throw new ReflectionsException("Can't use MemberUsageScanner", e);
        }
    }


    @Override
    public void scan(Object cls) {
        try {
            CtClass ctClass = CLASS_POOL.get(getMetadataAdapter().getClassName(cls));
            for (CtBehavior member : ctClass.getDeclaredConstructors()) {
                scanMember(member);
            }
            for (CtBehavior member : ctClass.getDeclaredMethods()) {
                scanMember(member);
            }
            if (removeCached != null) removeCached.invoke(ClassPool.getDefault(), ctClass.getName());
        } catch (Exception e) {
            throw new ReflectionsException("Could not scan method usage for " + getMetadataAdapter().getClassName(cls), e);
        }
    }

    void scanMember(CtBehavior member) throws CannotCompileException {
        //key contains this$/val$ means local field/parameter closure
        final String key = member.getDeclaringClass().getName() + "." + member.getMethodInfo().getName() +
                "(" + parameterNames(member.getMethodInfo()) + ")"; //+ " #" + member.getMethodInfo().getLineNumber(0)
        member.instrument(new ExprEditor() {
            @Override
            public void edit(NewExpr e) throws CannotCompileException {
                try {
                    put(e.getConstructor().getDeclaringClass().getName() + "." + "<init>" +
                            "(" + parameterNames(e.getConstructor().getMethodInfo()) + ")", e.getLineNumber(), key);
                } catch (NotFoundException e1) {
                    throw new ReflectionsException("Could not find new instance usage in " + key, e1);
                }
            }

            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                try {
                    put(m.getMethod().getDeclaringClass().getName() + "." + m.getMethodName() +
                            "(" + parameterNames(m.getMethod().getMethodInfo()) + ")", m.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + m.getClassName() + " in " + key, e);
                }
            }

            @Override
            public void edit(ConstructorCall c) throws CannotCompileException {
                try {
                    put(c.getConstructor().getDeclaringClass().getName() + "." + "<init>" +
                            "(" + parameterNames(c.getConstructor().getMethodInfo()) + ")", c.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + c.getClassName() + " in " + key, e);
                }
            }

            @Override
            public void edit(FieldAccess f) throws CannotCompileException {
                try {
                    put(f.getField().getDeclaringClass().getName() + "." + f.getFieldName(), f.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + f.getFieldName() + " in " + key, e);
                }
            }
        });
    }

    private void put(String key, int lineNumber, String value) {
        if (acceptResult(key)) {
            getStore().put(key, value + " #" + lineNumber);
        }
    }

    String parameterNames(MethodInfo info) {
        return Joiner.on(", ").join(getMetadataAdapter().getParameterNames(info));
    }
}
