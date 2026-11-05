package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class PrivateNameSyntaxTest {
    @Test
    public void superPropertyAccessOfPrivateNameIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "Unexpected private name after super (test#1)",
                "class C { #m = 1; D = class extends C { access() { return super.#m; } } }");
    }

    @Test
    public void superPrivateMethodCallIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "Unexpected private name after super (test#1)",
                "class C { #m = 1; D = class extends C { method() { return super.#m(); } } }");
    }

    @Test
    public void privateConstructorFieldIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "Classes may not have a private name called #constructor (test#1)",
                "class C { #constructor = 1; }");
    }

    @Test
    public void privateConstructorMethodIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "Classes may not have a private name called #constructor (test#1)",
                "class C { #constructor() {} }");
    }

    @Test
    public void privateConstructorAccessorIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "Classes may not have a private name called #constructor (test#1)",
                "class C { get #constructor() { return 1; } }");
    }

    @Test
    public void duplicatePrivateFieldIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "duplicate private name \"#x\" in class body (test#1)", "class C { #x; #x; }");
    }

    @Test
    public void duplicatePrivateMethodAndFieldIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "duplicate private name \"#x\" in class body (test#1)", "class C { #x() {} #x; }");
    }

    @Test
    public void duplicatePrivateGettersIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "duplicate private name \"#x\" in class body (test#1)",
                "class C { get #x() { return 1; } get #x() { return 2; } }");
    }

    @Test
    public void duplicatePrivateSettersIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "duplicate private name \"#x\" in class body (test#1)",
                "class C { set #x(v) {} set #x(v) {} }");
    }

    @Test
    public void privateGetterSetterPairWithDuplicateIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "duplicate private name \"#x\" in class body (test#1)",
                "class C { get #x() { return 1; } set #x(v) {} get #x() { return 2; } }");
    }

    @Test
    public void staticAndInstancePrivateAccessorsIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "duplicate private name \"#x\" in class body (test#1)",
                "class C { static get #x() { return 1; } set #x(v) {} }");
    }

    @Test
    public void privateGetterSetterPairIsAllowed() {
        Utils.assertWithAllModes_ES6(
                3,
                "class C { #v = 0; get #x() { return this.#v; } set #x(v) { this.#v = v; }"
                        + " run() { this.#x = 3; return this.#x; } }"
                        + " new C().run()");
    }

    @Test
    public void duplicatePublicNamesAreAllowed() {
        Utils.assertWithAllModes_ES6(
                2,
                "class C { a() { return 1; } a() { return 2; } get b() { return 1; }"
                        + " get b() { return 2; } } new C().a()");
    }

    @Test
    public void privateFieldsOnSameLineIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "missing ; after class field (test#1)", "class C { #x #y }");
    }

    @Test
    public void fieldFollowedByMethodOnSameLineIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "missing ; after class field (test#1)", "class C { x = 1 m() {} }");
    }

    @Test
    public void privateFieldsOnSeparateLinesAreAllowed() {
        Utils.assertWithAllModes_ES6(
                "ok",
                "var C = class {\n  #x\n  #y\n  x = 1\n  m() { return 'ok'; }\n}; new C().m()");
    }
}
