/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for generator methods in classes. */
public class ClassGeneratorMethodsTest {

    @Test
    public void generatorMethod() {
        String code =
                "class C { *bob() { yield 1; yield 2; } }\n"
                        + "var o = new C();\n"
                        + "var res = [];\n"
                        + "for (var x of o.bob()) res.push(x);\n"
                        + "res.join() + ',' + C.prototype.hasOwnProperty('bob')"
                        + " + ',' + o.hasOwnProperty('bob')"
                        + " + ',' + Object.keys(C.prototype).length";
        Utils.assertWithAllModes_ES6("1,2,true,false,0", code);
    }

    @Test
    public void generatorMethodBodyRunsLazily() {
        String code =
                "var log = [];\n"
                        + "class C { *bob() { log.push('Hi!'); } }\n"
                        + "var g = new C().bob();\n"
                        + "log.push('created');\n"
                        + "var r = g.next();\n"
                        + "log.join() + ',' + r.done";
        Utils.assertWithAllModes_ES6("created,Hi!,true", code);
    }

    @Test
    public void generatorMethodSeesThisAndSuper() {
        String code =
                "class A { m() { return 'a'; } }\n"
                        + "class B extends A { constructor() { super(); this.x = 'x'; }"
                        + " *gen() { yield this.x; yield super.m(); } }\n"
                        + "[...new B().gen()].join()";
        Utils.assertWithAllModes_ES6("x,a", code);
    }

    @Test
    public void computedGeneratorMethods() {
        String code =
                "class C { *['g' + 1]() { yield 1; }"
                        + " *[Symbol.iterator]() { yield* this.g1(); yield 2; } }\n"
                        + "[...new C()].join()";
        Utils.assertWithAllModes_ES6("1,2", code);
    }

    @Test
    public void generatorMethodsNamedGetAndSet() {
        String code =
                "class C { *get() { yield 'g'; } *set() { yield 's'; } }\n"
                        + "var o = new C();\n"
                        + "o.get().next().value + o.set().next().value";
        Utils.assertWithAllModes_ES6("gs", code);
    }

    @Test
    public void generatorConstructorIsAnError() {
        Utils.assertEvaluatorExceptionES6(
                "a class constructor may not be a generator", "class C { *constructor() {} }");
    }

    @Test
    public void generatorFieldIsAnError() {
        Utils.assertEvaluatorExceptionES6(
                "class members other than a constructor are not yet supported", "class C { *x; }");
    }
}
