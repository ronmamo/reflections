package org.reflections.scanners;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.MethodInfo;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;
import org.reflections.ReflectionsException;
import org.reflections.Store;
import org.reflections.util.ClasspathHelper;

import static org.reflections.util.Utils.join;

/** scans methods/constructors/fields usage
 * <p><i> depends on {@link org.reflections.adapters.JavassistAdapter} configured </i>*/
@SuppressWarnings("unchecked")
public class MemberUsageScanner extends AbstractScanner {
    private ClassPool classPool;

    @Override
    public void scan(Object cls, Store store) {
        try {
            CtClass ctClass = getClassPool().get(getMetadataAdapter().getClassName(cls));
            for (CtBehavior member : ctClass.getDeclaredConstructors()) {
                scanMember(member, store);
            }
            for (CtBehavior member : ctClass.getDeclaredMethods()) {
                scanMember(member, store);
            }
            ctClass.detach();
        } catch (Exception e) {
            throw new ReflectionsException("Could not scan method usage for " + getMetadataAdapter().getClassName(cls), e);
        }
    }

    void scanMember(CtBehavior member, Store store) throws CannotCompileException {
        //key contains this$/val$ means local field/parameter closure
        final String key = member.getDeclaringClass().getName() + "." + member.getMethodInfo().getName() +
                "(" + parameterNames(member.getMethodInfo()) + ")"; //+ " #" + member.getMethodInfo().getLineNumber(0)
        member.instrument(new ExprEditor() {
            @Override
            public void edit(NewExpr e) throws CannotCompileException {
                try {
                    put(store, e.getConstructor().getDeclaringClass().getName() + "." + "<init>" +
                            "(" + parameterNames(e.getConstructor().getMethodInfo()) + ")", e.getLineNumber(), key);
                } catch (NotFoundException e1) {
                    throw new ReflectionsException("Could not find new instance usage in " + key, e1);
                }
            }

            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                try {
                    put(store, m.getMethod().getDeclaringClass().getName() + "." + m.getMethodName() +
                            "(" + parameterNames(m.getMethod().getMethodInfo()) + ")", m.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + m.getClassName() + " in " + key, e);
                }
            }

            @Override
            public void edit(ConstructorCall c) throws CannotCompileException {
                try {
                    put(store, c.getConstructor().getDeclaringClass().getName() + "." + "<init>" +
                            "(" + parameterNames(c.getConstructor().getMethodInfo()) + ")", c.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + c.getClassName() + " in " + key, e);
                }
            }

            @Override
            public void edit(FieldAccess f) throws CannotCompileException {
                try {
                    put(store, f.getField().getDeclaringClass().getName() + "." + f.getFieldName(), f.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + f.getFieldName() + " in " + key, e);
                }
            }
        });
    }

    private void put(Store store, String key, int lineNumber, String value) {
        if (acceptResult(key)) {
            put(store, key, value + " #" + lineNumber);
        }
    }

    String parameterNames(MethodInfo info) {
        return join(getMetadataAdapter().getParameterNames(info), ", ");
    }

    private ClassPool getClassPool() {
        if (classPool == null) {
            synchronized (this) {
                classPool = new ClassPool();
                ClassLoader[] classLoaders = getConfiguration().getClassLoaders();
                if (classLoaders == null) {
                    classLoaders = ClasspathHelper.classLoaders();
                }
                for (ClassLoader classLoader : classLoaders) {
                    classPool.appendClassPath(new LoaderClassPath(classLoader));
                }
            }
        }
        return classPool;
    }
}
