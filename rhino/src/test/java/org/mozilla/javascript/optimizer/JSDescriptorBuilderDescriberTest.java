package org.mozilla.javascript.optimizer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mozilla.classfile.ClassFileWriter.ACC_PUBLIC;
import static org.mozilla.classfile.ClassFileWriter.ACC_STATIC;

import org.junit.jupiter.api.Test;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.DefiningClassLoader;
import org.mozilla.javascript.EagerSourceCodeProvider;
import org.mozilla.javascript.JSCode;
import org.mozilla.javascript.JSDescriptor;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.JSScript;
import org.mozilla.javascript.ast.FunctionNode;

class JSDescriptorBuilderDescriberTest {

    private static final String CLASS_NAME = "TestJSDescriptorConstant";

    private static final String FUNCTION_BODY_SIGNATURE =
            "(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/JSFunction;Ljava/lang/Object;Lorg/mozilla/javascript/VarScope;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";

    private static final String SCRIPT_BODY_SIGNATURE =
            "(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/JSScript;Ljava/lang/Object;Lorg/mozilla/javascript/VarScope;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";

    @Test
    @SuppressWarnings("unchecked")
    void builderResolvesToTheDescriptorItBuilds() throws Exception {
        var env = new MHJSCode.BuilderEnv(CLASS_NAME);

        var script = new JSDescriptor.Builder<JSScript>();
        var scriptCode = new MHJSScriptCode.Builder(env);
        scriptCode.methodName = "_c_script_0";
        scriptCode.methodType = SCRIPT_BODY_SIGNATURE;
        script.code = scriptCode;
        script.constructor = new JSCode.NullBuilder<>();
        script.paramAndVarNames = new String[] {"x"};
        script.paramIsConst = new boolean[] {true};
        script.isScript = true;
        script.isTopLevel = true;
        script.isStrict = true;
        script.sourceFile = "test.js";
        script.sourceCodeProvider = new EagerSourceCodeProvider("function f(a, b) {}");
        script.languageVersion = 200;

        var function = script.createChildBuilder();
        var functionCode = new MHJSFunctionCode.Builder(env);
        functionCode.methodName = "_c_f_1";
        functionCode.methodType = FUNCTION_BODY_SIGNATURE;
        function.code = functionCode;
        function.constructor = functionCode;
        function.paramAndVarNames = new String[] {"a", "b"};
        function.paramIsConst = new boolean[] {false, false};
        function.name = "f";
        function.functionType = FunctionNode.FUNCTION_STATEMENT;
        function.sourceStart = 0;
        function.sourceEnd = 19;
        function.paramAndVarCount = 2;
        function.paramCount = 2;
        function.arity = 2;
        function.hasPrototype = true;
        function.requiresActivationFrame = true;

        ClassFileWriter cfw = new ClassFileWriter(CLASS_NAME, "java/lang/Object", "test.js");
        Codegen.installConstantDescribers(cfw);
        var registry = cfw.getDynamicDescriptorRegistry();
        new MHJSScriptCodeDescriber(registry);
        new MHJSFunctionCodeDescriber(registry);
        new JSDescriptorBuilderDescriber(registry);

        addBody(cfw, scriptCode.methodName, SCRIPT_BODY_SIGNATURE, "script");
        addBody(cfw, functionCode.methodName, FUNCTION_BODY_SIGNATURE, "function");
        cfw.startMethod("get", "()Ljava/lang/Object;", (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.addLoadDynamicConstant(function);
        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod((short) 0);

        var cl = new DefiningClassLoader().defineClass(CLASS_NAME, cfw.toByteArray());
        var desc = (JSDescriptor<?>) cl.getMethod("get").invoke(null);

        assertEquals("f", desc.getName());
        assertEquals(FunctionNode.FUNCTION_STATEMENT, desc.getFunctionType());
        assertEquals("a", desc.getParamOrVarName(0));
        assertEquals("b", desc.getParamOrVarName(1));
        assertFalse(desc.getParamOrVarConst(1));
        assertEquals(2, desc.getParamAndVarCount());
        assertEquals(2, desc.getParamCount());
        assertEquals(2, desc.getArity());
        assertTrue(desc.hasPrototype());
        assertTrue(desc.requiresActivationFrame());
        assertTrue(desc.isStrict());
        assertFalse(desc.isScript());
        assertEquals("function f(a, b) {}", desc.getSource());
        assertEquals(
                "function",
                ((JSCode<JSFunction>) desc.getCode()).execute(null, null, null, null, null, null));
        assertSame(desc.getCode(), desc.getConstructor());

        var parent = (JSDescriptor<?>) desc.getParent();
        assertEquals("", parent.getName());
        assertEquals("test.js", parent.getSourceName());
        assertEquals(200, parent.getLanguageVersion());
        assertTrue(parent.isStrict());
        assertTrue(parent.isScript());
        assertTrue(parent.isTopLevel());
        assertTrue(parent.getParamOrVarConst(0));
        assertNull(parent.getParent());
        assertNull(parent.getConstructor());
        assertInstanceOf(MHJSScriptCode.class, parent.getCode());
        assertFalse(parent.hasNoFunctionStatementNamed("f"));
        assertTrue(parent.hasNoFunctionStatementNamed("g"));
        assertArrayEquals(new String[] {"f"}, script.functionStatementNames());
    }

    private static void addBody(ClassFileWriter cfw, String name, String signature, String result) {
        cfw.startMethod(name, signature, (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.addLoadConstant(result);
        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod((short) 6);
    }
}
