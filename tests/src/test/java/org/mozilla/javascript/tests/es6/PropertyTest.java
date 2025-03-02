package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.testutils.Utils;

public class PropertyTest {

    @Test
    public void prototypeProperty() throws Exception {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    final String expected = "undefined - true - true | function - function";

                    final String script =
                            "var desc = Object.getOwnPropertyDescriptor(MyHostObject, 'foo');"
                                    + "var result = '' + desc.writable + ' - ' + desc.configurable + ' - ' + desc.enumerable;"
                                    + "result = result + ' | ' + typeof desc.get + ' - ' + typeof desc.set;"
                                    + "result;";

                    try {
                        final MyHostObject myHostObject = new MyHostObject();

                        // define custom getter method
                        final Method getter = MyHostObject.class.getMethod("getFoo");
                        final Method setter = MyHostObject.class.getMethod("setFoo", String.class);
                        myHostObject.defineProperty(
                                "foo", null, getter, setter, ScriptableObject.EMPTY);
                        scope.put("MyHostObject", scope, myHostObject);
                    } catch (Exception e) {
                    }

                    final String result =
                            (String) cx.evaluateString(scope, script, "myScript", 1, null);

                    assertEquals(expected, result);

                    return null;
                });
    }

    @Test
    public void redefineGetterProperty() throws Exception {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    final String expected = "undefined - true - true | function - function";

                    final String script =
                            "Object.defineProperty(MyHostObject, 'foo', { enumerable: !0, configurable: !0, set: function() { return !0 }});\n"
                                    + "var desc = Object.getOwnPropertyDescriptor(MyHostObject, 'foo');"
                                    + "var result = '' + desc.writable + ' - ' + desc.configurable + ' - ' + desc.enumerable;"
                                    + "result = result + ' | ' + typeof desc.get + ' - ' + typeof desc.set;"
                                    + "result;";

                    try {
                        final MyHostObject myHostObject = new MyHostObject();

                        // define custom getter method
                        final Method getter = MyHostObject.class.getMethod("getFoo");
                        final Method setter = MyHostObject.class.getMethod("setFoo", String.class);
                        myHostObject.defineProperty(
                                "foo", null, getter, setter, ScriptableObject.EMPTY);
                        scope.put("MyHostObject", scope, myHostObject);
                    } catch (Exception e) {
                    }

                    final String result =
                            (String) cx.evaluateString(scope, script, "myScript", 1, null);

                    assertEquals(expected, result);

                    return null;
                });
    }

    @Test
    public void redefineSetterProperty() throws Exception {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    final String expected = "undefined - true - true | function - function";

                    final String script =
                            "Object.defineProperty(MyHostObject, 'foo', { enumerable: !0, configurable: !0, get: function() { return !0 }});\n"
                                    + "var desc = Object.getOwnPropertyDescriptor(MyHostObject, 'foo');"
                                    + "var result = '' + desc.writable + ' - ' + desc.configurable + ' - ' + desc.enumerable;"
                                    + "result = result + ' | ' + typeof desc.get + ' - ' + typeof desc.set;"
                                    + "result;";

                    try {
                        final MyHostObject myHostObject = new MyHostObject();

                        // define custom getter method
                        Method getter = MyHostObject.class.getMethod("getFoo");
                        final Method setter = MyHostObject.class.getMethod("setFoo", String.class);
                        myHostObject.defineProperty(
                                "foo", null, getter, setter, ScriptableObject.EMPTY);
                        scope.put("MyHostObject", scope, myHostObject);
                    } catch (Exception e) {
                    }

                    final String result =
                            (String) cx.evaluateString(scope, script, "myScript", 1, null);

                    assertEquals(expected, result);

                    return null;
                });
    }

    @Test
    public void redefinePropertyWithThreadSafeSlotMap() {

        final ContextFactory factory =
                Utils.contextFactoryWithFeatures(Context.FEATURE_THREAD_SAFE_OBJECTS);

        try (Context cx = factory.enterContext()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            ScriptableObject scope = cx.initStandardObjects();

            final String expected = "undefined - true - true | function - function";

            final String script =
                    "Object.defineProperty(MyHostObject, 'foo', { enumerable: !0, configurable: !0, set: function() { return !0 }});\n"
                            + "var desc = Object.getOwnPropertyDescriptor(MyHostObject, 'foo');"
                            + "var result = '' + desc.writable + ' - ' + desc.configurable + ' - ' + desc.enumerable;"
                            + "result = result + ' | ' + typeof desc.get + ' - ' + typeof desc.set;"
                            + "result;";

            try {
                final MyHostObject myHostObject = new MyHostObject();

                // define custom getter method
                final Method getter = MyHostObject.class.getMethod("getFoo");
                final Method setter = MyHostObject.class.getMethod("setFoo", String.class);
                myHostObject.defineProperty("foo", null, getter, setter, ScriptableObject.EMPTY);
                scope.put("MyHostObject", scope, myHostObject);
            } catch (Exception e) {
            }

            final String result = (String) cx.evaluateString(scope, script, "myScript", 1, null);

            assertEquals(expected, result);
        }
    }

    public static class MyHostObject extends ScriptableObject {

        private String foo;

        @Override
        public String getClassName() {
            return "MyHostObject";
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo.toUpperCase();
        }
    }
}
