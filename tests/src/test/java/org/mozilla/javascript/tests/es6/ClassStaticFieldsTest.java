/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for class static fields. */
public class ClassStaticFieldsTest {

    @Test
    public void staticFieldIsDefinedOnConstructor() {
        String code =
                "class C { static b = 7; }\n"
                        + "var o = new C();\n"
                        + "C.b + ',' + C.hasOwnProperty('b') + ',' + o.hasOwnProperty('b')"
                        + " + ',' + C.prototype.hasOwnProperty('b')";
        Utils.assertWithAllModes_ES6("7,true,false,false", code);
    }

    @Test
    public void staticFieldsWithoutInitializers() {
        String code =
                "class C { static a; static b = 2; }\n"
                        + "C.hasOwnProperty('a') + ',' + C.a + ',' + C.b"
                        + " + ',' + Object.getOwnPropertyDescriptor(C, 'b').enumerable";
        Utils.assertWithAllModes_ES6("true,undefined,2,true", code);
    }

    @Test
    public void staticInitializersRunOnceWithConstructorAsThis() {
        String code =
                "var count = 0;\n"
                        + "class C { static a = ++count; static b = this.a + 1;"
                        + " static c = typeof this; }\n"
                        + "new C(); new C();\n"
                        + "count + ',' + C.a + ',' + C.b + ',' + C.c";
        Utils.assertWithAllModes_ES6("1,1,2,function", code);
    }

    @Test
    public void staticAndInstanceFieldsTogether() {
        String code =
                "class C { a = 1; static a = 2; b = this.constructor.a; }\n"
                        + "var o = new C();\n"
                        + "o.a + ',' + C.a + ',' + o.b + ',' + Object.keys(o).join()";
        Utils.assertWithAllModes_ES6("1,2,2,a,b", code);
    }

    @Test
    public void computedAndPrivateStaticFields() {
        String code =
                "var s = Symbol('s');\n"
                        + "class C { static ['x' + 1] = 1; static [s]; static #p = 3;"
                        + " static ['y'] = this.#p; }\n"
                        + "C.x1 + ',' + C.hasOwnProperty(s) + ',' + C.y";
        Utils.assertWithAllModes_ES6("1,true,3", code);
    }

    @Test
    public void staticFieldsAreNotInherited() {
        String code =
                "class A { static a = 1; }\n"
                        + "class B extends A { static b = 2; }\n"
                        + "B.a + ',' + B.b + ',' + B.hasOwnProperty('a') + ',' + A.b";
        Utils.assertWithAllModes_ES6("1,2,false,undefined", code);
    }

    @Test
    public void membersNamedStatic() {
        String code =
                "class C { static = 1; static() { return 's'; } static static = 2; }\n"
                        + "var o = new C();\n"
                        + "o.static + ',' + C.prototype.static() + ',' + C.static";
        Utils.assertWithAllModes_ES6("1,s,2", code);
    }

    @Test
    public void staticPrototypeFieldIsAnError() {
        Utils.assertEvaluatorExceptionES6(
                "a class field may not be named \"prototype\"",
                "class C { static prototype = 1; }");
    }

    @Test
    public void constructorFieldIsAnError() {
        Utils.assertEvaluatorExceptionES6(
                "a class field may not be named \"constructor\"",
                "class C { static constructor = 1; }");
    }
}
