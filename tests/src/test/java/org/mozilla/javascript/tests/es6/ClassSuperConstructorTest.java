/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests that {@code super(...)} in a derived class constructor is a constructor call. */
public class ClassSuperConstructorTest {

    @Test
    public void superReceivesNewTarget() {
        Utils.assertWithAllModes_ES6(
                true,
                "var nt;\n"
                        + "class A { constructor() { nt = new.target; } }\n"
                        + "class B extends A { constructor() { super(); } }\n"
                        + "new B();\n"
                        + "nt === B");
    }

    @Test
    public void defaultDerivedConstructorPassesNewTarget() {
        Utils.assertWithAllModes_ES6(
                true,
                "var nt;\n"
                        + "class A { constructor() { nt = new.target; } }\n"
                        + "class B extends A {}\n"
                        + "new B();\n"
                        + "nt === B");
    }

    @Test
    public void thisIsObjectReturnedByParent() {
        Utils.assertWithAllModes_ES6(
                "true,1,2",
                "var o = { a: 1 };\n"
                        + "function A() { return o; }\n"
                        + "class B extends A { constructor() { super(); this.b = 2; } }\n"
                        + "var b = new B();\n"
                        + "[b === o, b.a, b.b].join()");
    }

    @Test
    public void superCallValueIsThis() {
        Utils.assertWithAllModes_ES6(
                true,
                "var r;\n"
                        + "class A {}\n"
                        + "class B extends A { constructor() { r = super(); r.self = this; } }\n"
                        + "var b = new B();\n"
                        + "b === r && b.self === b");
    }

    @Test
    public void prototypeChainUsesNewTarget() {
        Utils.assertWithAllModes_ES6(
                "true,true,true",
                "class A { constructor(x) { this.x = x; } }\n"
                        + "class B extends A { constructor(x) { super(x); } m() { return this.x; } }\n"
                        + "class C extends B {}\n"
                        + "var c = new C(1);\n"
                        + "[Object.getPrototypeOf(c) === C.prototype, c instanceof A, c.m() === 1].join()");
    }

    @Test
    public void parametersSurviveSuperCall() {
        Utils.assertWithAllModes_ES6(
                "1,2",
                "class A { constructor(a, b) { this.v = a + ',' + b; } }\n"
                        + "class B extends A { constructor(x, y) { super(9, 9); this.v = x + ',' + y; } }\n"
                        + "new B(1, 2).v");
    }

    @Test
    public void spreadArguments() {
        Utils.assertWithAllModes_ES6(
                "1,2,3",
                "class A { constructor(...a) { this.v = a.join(); } }\n"
                        + "class B extends A { constructor(...a) { super(...a); } }\n"
                        + "new B(1, 2, 3).v");
    }

    @Test
    public void earlyReturnReturnsThis() {
        Utils.assertWithAllModes_ES6(
                true,
                "class A {}\n"
                        + "class B extends A { constructor() { super(); if (true) return; } }\n"
                        + "new B() instanceof B");
    }

    @Test
    public void returningObjectOverridesThis() {
        Utils.assertWithAllModes_ES6(
                1,
                "class A {}\n"
                        + "class B extends A { constructor() { super(); return { v: 1 }; } }\n"
                        + "new B().v");
    }

    @Test
    public void fieldsInitializedOnConstructedObject() {
        Utils.assertWithAllModes_ES6(
                "true,1",
                "var o = {};\n"
                        + "function A() { return o; }\n"
                        + "class B extends A { f = 1; }\n"
                        + "var b = new B();\n"
                        + "[b === o, o.f].join()");
    }

    @Test
    public void superCalledTwiceThrows() {
        Utils.assertWithAllModes_ES6(
                "ReferenceError",
                "class A {}\n"
                        + "class B extends A { constructor() { super(); super(); } }\n"
                        + "try { new B(); 'no error' } catch (e) { e.name }");
    }

    @Test
    public void missingSuperCallThrows() {
        Utils.assertWithAllModes_ES6(
                "ReferenceError",
                "class A {}\n"
                        + "class B extends A { constructor() {} }\n"
                        + "try { new B(); 'no error' } catch (e) { e.name }");
    }

    @Test
    public void primitiveReturnThrows() {
        Utils.assertWithAllModes_ES6(
                "TypeError",
                "class A {}\n"
                        + "class B extends A { constructor() { super(); return 1; } }\n"
                        + "try { new B(); 'no error' } catch (e) { e.name }");
    }
}
