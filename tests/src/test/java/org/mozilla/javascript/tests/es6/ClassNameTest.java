/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for the {@code name} property of classes. */
public class ClassNameTest {

    @Test
    public void classStatement() {
        Utils.assertWithAllModes_ES6("A", "class A {}\nA.name");
    }

    @Test
    public void namedClassExpression() {
        Utils.assertWithAllModes_ES6("B", "var c = class B {};\nc.name");
    }

    @Test
    public void namedClassExpressionWithConstructor() {
        Utils.assertWithAllModes_ES6(
                "B", "var c = class B extends Object { constructor() {} };\nc.name");
    }

    @Test
    public void anonymousClassInVar() {
        Utils.assertWithAllModes_ES6("cls", "var cls = class {};\ncls.name");
    }

    @Test
    public void anonymousClassInLet() {
        Utils.assertWithAllModes_ES6("cls", "let cls = class {};\ncls.name");
    }

    @Test
    public void anonymousClassAssigned() {
        Utils.assertWithAllModes_ES6("cls", "var cls;\ncls = class {};\ncls.name");
    }

    @Test
    public void anonymousClassInObjectLiteral() {
        Utils.assertWithAllModes_ES6("k", "var o = { k: class {} };\no.k.name");
    }

    @Test
    public void anonymousClassAsDestructuringDefault() {
        Utils.assertWithAllModes_ES6(
                "d,e",
                "var { d = class {} } = {};\nvar [e = class {}] = [];\nd.name + ',' + e.name");
    }

    @Test
    public void staticNameMethodOverridesInferredName() {
        Utils.assertWithAllModes_ES6(
                "function,true,false,true",
                "xCls2 = class { static name() {} };\n"
                        + "var d = Object.getOwnPropertyDescriptor(xCls2, 'name');\n"
                        + "[typeof xCls2.name, d.writable, d.enumerable, d.configurable].join()");
    }

    @Test
    public void staticNameMethodOverridesClassName() {
        Utils.assertWithAllModes_ES6("function", "class A { static name() {} }\ntypeof A.name");
    }

    @Test
    public void staticNameAccessorsOverrideClassName() {
        Utils.assertWithAllModes_ES6(
                "g,5",
                "var v;\n"
                        + "class A { static get name() { return 'g'; } static set name(x) { v = x; } }\n"
                        + "A.name = 5;\n"
                        + "A.name + ',' + v");
    }

    @Test
    public void anonymousClassAssignedToProperty() {
        Utils.assertWithAllModes_ES6("", "var o = {};\no.p = class {};\no.p.name");
    }
}
