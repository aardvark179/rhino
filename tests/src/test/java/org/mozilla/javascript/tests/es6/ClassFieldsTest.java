/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.testutils.Utils;

/** Tests for class instance fields. */
public class ClassFieldsTest {

    @Test
    public void fieldsAreInitializedInOrder() {
        String code =
                "var log = [];\n"
                        + "class A { a = log.push('a'); b = log.push('b'); c; }\n"
                        + "var o = new A();\n"
                        + "log.join() + ',' + Object.keys(o).join() + ',' + o.c";
        Utils.assertWithAllModes_ES6("a,b,a,b,c,undefined", code);
    }

    @Test
    public void initializersSeeEarlierFields() {
        String code =
                "class A { a = 1; b = this.a + 1; c = this.hasOwnProperty('d'); d = 4; }\n"
                        + "var o = new A();\n"
                        + "o.a + ',' + o.b + ',' + o.c";
        Utils.assertWithAllModes_ES6("1,2,false", code);
    }

    @Test
    public void fieldsWithoutInitializers() {
        String code =
                "class C { alice; bob = 2; charlie; }\n"
                        + "var o = new C();\n"
                        + "Object.keys(o).join() + ',' + o.alice + ',' + o.bob + ',' + o.charlie";
        Utils.assertWithAllModes_ES6("bob,alice,charlie,undefined,2,undefined", code);
    }

    @Test
    public void fieldsWithoutInitializersAreDefinedAfterInitializersRun() {
        String code =
                "class C { a; b = this.hasOwnProperty('a'); }\n"
                        + "var o = new C();\n"
                        + "o.hasOwnProperty('a') + ',' + o.b";
        Utils.assertWithAllModes_ES6("true,false", code);
    }

    @Test
    public void computedFieldNames() {
        String code =
                "var s = Symbol('s');\n"
                        + "class C { ['a' + 'b']; c; [s]; [1 + 1]; }\n"
                        + "var o = new C();\n"
                        + "Object.keys(o).join() + ',' + o.ab + ',' + o.hasOwnProperty('ab')"
                        + " + ',' + o.hasOwnProperty(s) + ',' + o.hasOwnProperty('2')";
        Utils.assertWithAllModes_ES6("2,ab,c,undefined,true,true,true", code);
    }

    @Test
    public void computedFieldNamesAreEvaluatedOnceAtClassDefinition() {
        String code =
                "var log = [];\n"
                        + "var key = { toString() { log.push('key'); return 'k'; } };\n"
                        + "class C { [(log.push('expr'), key)]; }\n"
                        + "log.push('defined');\n"
                        + "var o1 = new C(), o2 = new C();\n"
                        + "log.join() + ',' + o1.hasOwnProperty('k') + ',' + o2.hasOwnProperty('k')";
        Utils.assertWithAllModes_ES6("expr,key,defined,true,true", code);
    }

    @Test
    public void computedFieldNamesWithInitializers() {
        String code =
                "var s = Symbol('s');\n"
                        + "class C { ['a' + 'b'] = 7; c = 1; [s] = this.ab + this.c; [2]; ['d'] = this[2]; }\n"
                        + "var o = new C();\n"
                        + "Object.keys(o).join() + ',' + o.ab + ',' + o[s] + ',' + o.d";
        Utils.assertWithAllModes_ES6("2,ab,c,d,7,8,undefined", code);
    }

    @Test
    public void computedFieldKeysAreEvaluatedInOrderOnceAndInitializersPerInstance() {
        String code =
                "var log = [];\n"
                        + "class C { [log.push('k1')] = log.push('i1'); [log.push('k2')];"
                        + " [log.push('k3')] = log.push('i3'); }\n"
                        + "log.push('defined');\n"
                        + "new C(); new C();\n"
                        + "log.join()";
        Utils.assertWithAllModes_ES6("k1,k2,k3,defined,i1,i3,i1,i3", code);
    }

    @Test
    public void computedFieldKeysAreFreshPerClassEvaluation() {
        String code =
                "var classes = [];\n"
                        + "for (let k of ['x', 'y']) { classes.push(class { [k] = k; }); }\n"
                        + "var a = new classes[0](), b = new classes[1]();\n"
                        + "Object.keys(a).join() + ',' + a.x + ',' + Object.keys(b).join() + ',' + b.y";
        Utils.assertWithAllModes_ES6("x,x,y,y", code);
    }

    @Test
    public void computedFieldNamesWithInitializersAndMethods() {
        String code =
                "class A { ['m' + 1]() { return 'm'; } [`x`]; y = 2; }\n"
                        + "class B extends A { ['z']; }\n"
                        + "var o = new B();\n"
                        + "Object.keys(o).join() + ',' + o.m1()";
        Utils.assertWithAllModes_ES6("y,x,z,m", code);
    }

    @Test
    public void fieldsAreInitializedBeforeConstructorBody() {
        String code =
                "class A { a = 1; constructor(x) { this.b = this.a + x; } }\n"
                        + "var o = new A(2);\n"
                        + "o.a + ',' + o.b";
        Utils.assertWithAllModes_ES6("1,3", code);
    }

    @Test
    public void initializersAreEvaluatedPerInstance() {
        String code =
                "var n = 0;\n"
                        + "class A { a = ++n; o = {}; }\n"
                        + "var x = new A(), y = new A();\n"
                        + "x.a + ',' + y.a + ',' + (x.o === y.o)";
        Utils.assertWithAllModes_ES6("1,2,false", code);
    }

    @Test
    public void derivedFieldsAreInitializedAfterSuper() {
        String code =
                "var log = [];\n"
                        + "class A { a = log.push('A.a'); constructor() { log.push('A()'); } }\n"
                        + "class B extends A {\n"
                        + "  b = log.push('B.b');\n"
                        + "  constructor() { log.push('before'); super(); log.push('after'); }\n"
                        + "}\n"
                        + "new B();\n"
                        + "log.join()";
        Utils.assertWithAllModes_ES6("before,A.a,A(),B.b,after", code);
    }

    @Test
    public void derivedFieldsWithDefaultConstructor() {
        String code =
                "class A { a = 1; }\n"
                        + "class B extends A { b = this.a + 1; }\n"
                        + "var o = new B();\n"
                        + "o.a + ',' + o.b";
        Utils.assertWithAllModes_ES6("1,2", code);
    }

    @Test
    public void superCallNotAtTopLevelOfConstructor() {
        String code =
                "class A {}\n"
                        + "class B extends A {\n"
                        + "  b = 2;\n"
                        + "  constructor(x) { if (x) { super(); } else { super(); } }\n"
                        + "}\n"
                        + "new B(true).b + new B(false).b";
        Utils.assertWithAllModes_ES6(4, code);
    }

    @Test
    public void privateFields() {
        assertInterpretedModes(
                "1,undefined,2,b",
                "class A { #a = 1; b = 2; #c; getA() { return this.#a + ',' + this.#c; } }\n"
                        + "var o = new A();\n"
                        + "o.getA() + ',' + o.b + ',' + Object.keys(o).join()");
    }

    @Test
    public void privateNameFromEnclosingClass() {
        assertInterpretedModes(
                "1,2",
                "class A { #a = 1; m(o) {\n"
                        + "  class B { #a = 2; get(x) { return x.#a; } }\n"
                        + "  class C { get(x) { return x.#a; } }\n"
                        + "  return new C().get(this) + ',' + new B().get(new B()); } }\n"
                        + "new A().m()");
    }

    private static void assertInterpretedModes(String expected, String code) {
        // Private names cannot yet be compiled to class files.
        for (var mode :
                new Context.EvaluationMethod[] {
                    Context.EvaluationMethod.Interpreter, Context.EvaluationMethod.InterpreterV2
                }) {
            Utils.runWithMode(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_ES6);
                        var scope = cx.initStandardObjects();
                        assertEquals(expected, cx.evaluateString(scope, code, "test.js", 1, null));
                        return null;
                    },
                    mode);
        }
    }

    @Test
    public void arrowInInitializerCapturesThis() {
        String code =
                "class A { a = 1; f = () => this.a; }\n"
                        + "var o = new A();\n"
                        + "var f = o.f;\n"
                        + "f()";
        Utils.assertWithAllModes_ES6(1, code);
    }

    @Test
    public void superPropertyInInitializer() {
        String code =
                "class A { m() { return 'A'; } }\n"
                        + "class B extends A { m() { return 'B'; } x = super.m(); }\n"
                        + "new B().x";
        Utils.assertWithAllModes_ES6("A", code);
    }

    @Test
    @Disabled("Field initializers currently assign to this rather than define the field")
    public void fieldsAreDefinedNotAssigned() {
        String code =
                "var called = false;\n"
                        + "class A { set a(v) { called = true; } }\n"
                        + "class B extends A { a = 1; }\n"
                        + "var o = new B();\n"
                        + "called + ',' + o.hasOwnProperty('a') + ',' + o.a";
        Utils.assertWithAllModes_ES6("false,true,1", code);
    }

    @Test
    public void fieldsAreEnumerableWritableConfigurable() {
        String code =
                "class A { a = 1; }\n"
                        + "var d = Object.getOwnPropertyDescriptor(new A(), 'a');\n"
                        + "d.enumerable + ',' + d.writable + ',' + d.configurable";
        Utils.assertWithAllModes_ES6("true,true,true", code);
    }
}
