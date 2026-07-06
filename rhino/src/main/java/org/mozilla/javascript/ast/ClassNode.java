/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.mozilla.javascript.Token;

/**
 * AST node for an ES6 class declaration or expression.
 *
 * <p>Node type is {@link Token#CLASS}.
 *
 * <p>A class is a (possibly synthesized) constructor, an optional {@code extends} clause, instance
 * methods/getters/setters and instance fields with literal, computed, or private ({@code #name})
 * keys. Static members and computed field keys are not yet supported.
 *
 * <p>Private names flow through the same method/field lists as ordinary members - a name starting
 * with {@code #} is recognized specially wherever it matters (see {@link
 * org.mozilla.javascript.IRFactory#transformClass}). {@link #getPrivateNames} returns the distinct
 * private names declared in this class, for which a hidden per-class binding needs to be set up.
 *
 * <pre>
 * <i>ClassDeclaration</i> :
 *        <b>class</b> BindingIdentifier ClassTail
 * <i>ClassExpression</i> :
 *        <b>class</b> BindingIdentifieropt ClassTail
 * <i>ClassTail</i> :
 *        ClassHeritageopt { ClassBody }
 * </pre>
 */
public class ClassNode extends AstNode {

    /** Kind of a class member that is a function: normal method, getter, or setter. */
    public enum ElementKind {
        METHOD,
        GETTER,
        SETTER
    }

    private Name className;
    private AstNode superClass;
    private FunctionNode constructor;
    private boolean isStatement;
    private List<String> methodNames;
    private List<FunctionNode> methods;
    private List<ElementKind> methodKinds;
    private List<AstNode> methodComputedKeys;
    private List<String> fieldNames;
    private List<AstNode> fieldInitializers;
    private final Set<String> privateNames = new LinkedHashSet<>();
    private String privateNameScopeId;

    {
        type = Token.CLASS;
    }

    public ClassNode() {}

    public ClassNode(int pos) {
        super(pos);
    }

    public ClassNode(int pos, int len) {
        super(pos, len);
    }

    public Name getClassName() {
        return className;
    }

    public void setClassName(Name className) {
        this.className = className;
        if (className != null) {
            className.setParent(this);
        }
    }

    public AstNode getSuperClass() {
        return superClass;
    }

    public void setSuperClass(AstNode superClass) {
        this.superClass = superClass;
        if (superClass != null) {
            superClass.setParent(this);
        }
    }

    public FunctionNode getConstructor() {
        return constructor;
    }

    public void setConstructor(FunctionNode constructor) {
        this.constructor = constructor;
        if (constructor != null) {
            constructor.setParent(this);
        }
    }

    public boolean isStatement() {
        return isStatement;
    }

    public void setIsStatement(boolean isStatement) {
        this.isStatement = isStatement;
    }

    public void addMethod(String name, FunctionNode fn, ElementKind kind) {
        addMethodInternal(name, null, fn, kind);
    }

    /**
     * Adds a method/getter/setter whose property key is computed at runtime (e.g. {@code
     * [expr]()}). The key is evaluated exactly once, at class-definition time.
     */
    public void addComputedMethod(AstNode keyExpr, FunctionNode fn, ElementKind kind) {
        addMethodInternal(null, keyExpr, fn, kind);
    }

    private void addMethodInternal(
            String name, AstNode keyExpr, FunctionNode fn, ElementKind kind) {
        if (methodNames == null) {
            methodNames = new ArrayList<>();
            methods = new ArrayList<>();
            methodKinds = new ArrayList<>();
            methodComputedKeys = new ArrayList<>();
        }
        methodNames.add(name);
        methods.add(fn);
        methodKinds.add(kind);
        methodComputedKeys.add(keyExpr);
        fn.setParent(this);
        if (keyExpr != null) {
            keyExpr.setParent(this);
        }
    }

    public int getMethodCount() {
        return methodNames == null ? 0 : methodNames.size();
    }

    public List<String> getMethodNames() {
        return methodNames == null ? Collections.emptyList() : methodNames;
    }

    public List<FunctionNode> getMethods() {
        return methods == null ? Collections.emptyList() : methods;
    }

    public List<ElementKind> getMethodKinds() {
        return methodKinds == null ? Collections.emptyList() : methodKinds;
    }

    public List<AstNode> getMethodComputedKeys() {
        return methodComputedKeys == null ? Collections.emptyList() : methodComputedKeys;
    }

    /** Adds an instance field declaration. {@code initializer} may be {@code null}. */
    public void addField(String name, AstNode initializer) {
        if (fieldNames == null) {
            fieldNames = new ArrayList<>();
            fieldInitializers = new ArrayList<>();
        }
        fieldNames.add(name);
        fieldInitializers.add(initializer);
        if (initializer != null) {
            initializer.setParent(this);
        }
    }

    public int getFieldCount() {
        return fieldNames == null ? 0 : fieldNames.size();
    }

    public List<String> getFieldNames() {
        return fieldNames == null ? Collections.emptyList() : fieldNames;
    }

    public List<AstNode> getFieldInitializers() {
        return fieldInitializers == null ? Collections.emptyList() : fieldInitializers;
    }

    /**
     * Registers a private name ({@code #name}, including the leading {@code #}) used somewhere in
     * this class - as a field/method/accessor key. Returns {@code true} the first time a given name
     * is registered (the caller should only set up the name's hidden binding then).
     */
    public boolean addPrivateNameIfNew(String name) {
        return privateNames.add(name);
    }

    /** Returns the distinct private names ({@code #name}) declared anywhere in this class. */
    public Set<String> getPrivateNames() {
        return privateNames;
    }

    /**
     * A scope-unique identifier for this specific class-literal site, used (via {@link
     * #privateVarName}) to disambiguate private name hidden variables from those of any other class
     * sharing the same enclosing scope and the same private name. Set once, at parse time.
     */
    public void setPrivateNameScopeId(String privateNameScopeId) {
        this.privateNameScopeId = privateNameScopeId;
    }

    public String getPrivateNameScopeId() {
        return privateNameScopeId;
    }

    /**
     * The identifier of the hidden, per-class-definition-evaluation binding that holds the {@link
     * org.mozilla.javascript.SymbolKey} for a given private name. Not a valid JS identifier, so it
     * can't collide with anything user-written; scoped by {@code scopeId} (see {@link
     * #setPrivateNameScopeId}) so two unrelated classes declaring the same private name in the same
     * enclosing scope don't clash.
     */
    public static String privateVarName(String scopeId, String privateName) {
        return "%priv" + scopeId + privateName;
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append("class");
        if (className != null) {
            sb.append(" ");
            sb.append(className.toSource(0));
        }
        if (superClass != null) {
            sb.append(" extends ");
            sb.append(superClass.toSource(0));
        }
        sb.append(" {\n");
        if (fieldNames != null) {
            for (int i = 0; i < fieldNames.size(); i++) {
                sb.append(makeIndent(depth + 1));
                sb.append(fieldNames.get(i));
                AstNode init = fieldInitializers.get(i);
                if (init != null) {
                    sb.append(" = ");
                    sb.append(init.toSource(0));
                }
                sb.append(";\n");
            }
        }
        if (constructor != null) {
            sb.append(makeIndent(depth + 1));
            sb.append("constructor");
            String ctorSrc = constructor.toSource(0);
            sb.append(ctorSrc.substring(ctorSrc.indexOf('(')));
            sb.append("\n");
        }
        if (methodNames != null) {
            for (int i = 0; i < methodNames.size(); i++) {
                sb.append(makeIndent(depth + 1));
                ElementKind kind = methodKinds.get(i);
                if (kind == ElementKind.GETTER) {
                    sb.append("get ");
                } else if (kind == ElementKind.SETTER) {
                    sb.append("set ");
                }
                String name = methodNames.get(i);
                if (name == null) {
                    sb.append("[");
                    sb.append(methodComputedKeys.get(i).toSource(0));
                    sb.append("]");
                } else {
                    sb.append(name);
                }
                String fnSrc = methods.get(i).toSource(0);
                sb.append(fnSrc.substring(fnSrc.indexOf('(')));
                sb.append("\n");
            }
        }
        sb.append(makeIndent(depth));
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            if (className != null) {
                className.visit(v);
            }
            if (superClass != null) {
                superClass.visit(v);
            }
            if (constructor != null) {
                constructor.visit(v);
            }
            if (methods != null) {
                for (int i = 0; i < methods.size(); i++) {
                    if (methodComputedKeys.get(i) != null) {
                        methodComputedKeys.get(i).visit(v);
                    }
                    methods.get(i).visit(v);
                }
            }
            if (fieldInitializers != null) {
                for (AstNode init : fieldInitializers) {
                    if (init != null) {
                        init.visit(v);
                    }
                }
            }
        }
    }
}
