/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for class static methods and accessors. */
public class ClassStaticMethodsTest {

    @Test
    public void staticMethodIsDefinedOnConstructor() {
        String code =
                "class C { static b() { return 'bob'; } }\n"
                        + "C.b() + ',' + C.hasOwnProperty('b') + ',' + C.prototype.hasOwnProperty('b')"
                        + " + ',' + Object.getOwnPropertyDescriptor(C, 'b').enumerable";
        Utils.assertWithAllModes_ES6("bob,true,false,false", code);
    }

    @Test
    public void staticAndInstanceMethodsWithTheSameName() {
        String code =
                "class C { m() { return 'i'; } static m() { return 's'; } }\n"
                        + "new C().m() + C.m()";
        Utils.assertWithAllModes_ES6("is", code);
    }

    @Test
    public void staticMethodThisIsTheConstructor() {
        String code =
                "class C { static create() { return new this(); } }\n" + "C.create() instanceof C";
        Utils.assertWithAllModes_ES6(Boolean.TRUE, code);
    }

    @Test
    public void staticMethodsAreInheritedAndSuperReachesParentStatics() {
        String code =
                "class A { static m() { return 'a'; } n() { return 'proto'; } }\n"
                        + "class B extends A { static m() { return super.m() + 'b'; }"
                        + " static n() { return typeof super.n; } }\n"
                        + "B.m() + ',' + B.n() + ',' + Object.getPrototypeOf(B).m()";
        Utils.assertWithAllModes_ES6("ab,undefined,a", code);
    }

    @Test
    public void staticSuperFollowsTheConstructorsPrototype() {
        String code =
                "class A { static m() { return 'a'; } }\n"
                        + "class B { static m() { return super.m(); } }\n"
                        + "Object.setPrototypeOf(B, A);\n"
                        + "B.m()";
        Utils.assertWithAllModes_ES6("a", code);
    }

    @Test
    public void methodsAreFreshPerClassEvaluation() {
        String code =
                "function make(v) { return class { m() { return v; } static s() { return v; } }; }\n"
                        + "var A = make(1), B = make(2);\n"
                        + "new A().m() + ',' + new B().m() + ',' + A.s() + ',' + B.s()"
                        + " + ',' + (A.prototype.m === B.prototype.m)";
        Utils.assertWithAllModes_ES6("1,2,1,2,false", code);
    }

    @Test
    public void staticAccessors() {
        String code =
                "class C { static get x() { return this._x; } static set x(v) { this._x = v * 2; } }\n"
                        + "C.x = 2;\n"
                        + "C.x + ',' + C.prototype.hasOwnProperty('x')";
        Utils.assertWithAllModes_ES6("4,false", code);
    }

    @Test
    public void computedPrivateAndGeneratorStaticMethods() {
        String code =
                "class C { static ['a' + 'b']() { return 1; } static #p() { return 2; }"
                        + " static *g() { yield this.ab(); yield C.#p(); } }\n"
                        + "[...C.g()].join()";
        Utils.assertWithAllModes_ES6("1,2", code);
    }

    @Test
    public void staticMethodNamedConstructor() {
        String code =
                "class C { constructor() { this.x = 1; } static constructor() { return 's'; } }\n"
                        + "new C().x + ',' + C.constructor()";
        Utils.assertWithAllModes_ES6("1,s", code);
    }

    @Test
    public void staticMethodsAndFieldsTogether() {
        String code = "class C { static f = this.m(); static m() { return 'm'; } }\n" + "C.f";
        Utils.assertWithAllModes_ES6("m", code);
    }

    @Test
    public void staticPrototypeMethodIsAnError() {
        Utils.assertEvaluatorExceptionES6(
                "a class may not have a static method named \"prototype\"",
                "class C { static prototype() {} }");
    }

    @Test
    public void numericAccessorAndAsyncNames() {
        String code =
                "var stringSet;\n"
                        + "var C = class {\n"
                        + "  static get 0b10() { return 'got'; }\n"
                        + "  static set 0b10(param) { stringSet = param; }\n"
                        + "  get 1() { return 'inst'; }\n"
                        + "  async 5() { return 1; }\n"
                        + "};\n"
                        + "C[2] = 'set';\n"
                        + "Object.getOwnPropertyNames(C).filter(function (k) { return k !== 'length' && k !== 'name' && k !== 'prototype'; }).join()"
                        + " + ',' + C[2] + ',' + stringSet + ',' + new C()[1] + ',' + typeof C.prototype[5]";
        Utils.assertWithAllModes_ES6("2,got,set,inst,function", code);
    }

    @Test
    public void methodNames() {
        String code =
                "var C = class { static #m() { } static 0b101() { } static 'a b'() { } foo() { }\n"
                        + "  static get #g() { } static get x() { } set 7(v) { }\n"
                        + "  static names() { return [this.#m.name, this[5].name, this['a b'].name,"
                        + " this.prototype.foo.name,"
                        + " Object.getOwnPropertyDescriptor(this, 'x').get.name,"
                        + " Object.getOwnPropertyDescriptor(this.prototype, '7').set.name]; } };\n"
                        + "C.names().join()";
        Utils.assertWithAllModes_ES6("#m,5,a b,foo,get x,set 7", code);
    }

    @Test
    public void accessorConstructorIsAnError() {
        Utils.assertEvaluatorExceptionES6(
                "a class constructor may not be an accessor", "class C { get constructor() {} }");
        Utils.assertEvaluatorExceptionES6(
                "a class constructor may not be an accessor",
                "class C { set 'constructor'(v) {} }");
    }

    @Test
    public void staticAccessorNamedConstructor() {
        String code =
                "var v;\n"
                        + "class C { static get constructor() { return 'g'; }"
                        + " static set constructor(x) { v = x; } }\n"
                        + "C.constructor = 's';\n"
                        + "C.constructor + ',' + v";
        Utils.assertWithAllModes_ES6("g,s", code);
    }
}
