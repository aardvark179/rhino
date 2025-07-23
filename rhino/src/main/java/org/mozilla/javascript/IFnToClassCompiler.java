package org.mozilla.javascript;

import org.mozilla.javascript.optimizer.ClassCompiler;

public class IFnToClassCompiler implements Context.FunctionCompiler {

    @Override
    public Callable compile(
            InterpretedFunction ifun,
            Context cx,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args) {
        try {
            InterpreterData idata = ifun.idata;
            if (ifun.getRawSource() == null) {
                return null; // No source available
            }

            CompilerEnvirons env = new CompilerEnvirons();
            env.initFromContext(cx);
            env.setInterpretedMode(false);
//            env.setOptimizationLevel(9); // TODO

            ClassCompiler compiler = new ClassCompiler(env);
            String className = "CompiledFunction" + Math.abs(ifun.hashCode());
            String fullClassName = "org.mozilla.javascript.compiled." + className;

            Object[] results =
                    compiler.compileToClassFiles(
                            ifun.getRawSource(),
                            idata.itsSourceFile,
                            0, // TODO
                            fullClassName,
                            true);

            if (results == null || results.length < 2) {
                return null; // TODO: Compilation failed
            }

            ClassLoader parentLoader = cx.getApplicationClassLoader();
            ClassLoader loader =
                    new ClassLoader(parentLoader) {
                        @Override
                        protected Class<?> findClass(String name) throws ClassNotFoundException {
                            for (int i = 0; i < results.length; i += 2) {
                                String className = (String) results[i];
                                if (name.equals(className)) {
                                    byte[] classBytes = (byte[]) results[i + 1];
                                    return defineClass(name, classBytes, 0, classBytes.length);
                                }
                            }
                            return super.findClass(name);
                        }
                    };

            Class<?> clazz = loader.loadClass(fullClassName);

            // TODO: is it always the first constructor?
            // TODO: _id is 1?
            // Create a new instance of the compiled function
            return (Callable) clazz.getDeclaredConstructors()[0].newInstance(scope, cx, 1);

        } catch (Exception e) {
            // Log the error and fall back to interpretation
            Context.reportError("Error compiling function: " + e);
            return null;
        } catch (VerifyError err) {
            Context.reportError("Error compiling function: " + err);
            return null;
        }
    }
}
