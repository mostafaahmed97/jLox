package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import lox.Expr.Assign;
import lox.Expr.Binary;
import lox.Expr.Call;
import lox.Expr.Get;
import lox.Expr.Grouping;
import lox.Expr.Literal;
import lox.Expr.Logical;
import lox.Expr.Set;
import lox.Expr.Unary;
import lox.Expr.Variable;
import lox.Stmt.Class;
import lox.Stmt.Expression;
import lox.Stmt.Function;
import lox.Stmt.If;
import lox.Stmt.Print;
import lox.Stmt.Return;
import lox.Stmt.Var;
import lox.Stmt.While;

/*
 * A Resolver analyzes the code after parsing.
 * 
 * It's job is to bind variable access to the 
 * corresponding correct definition to avoid scoping
 * problems. 
 * 
 * Example
 * 
 * ```
 *  var a = "global";
    {
    fun showA() {
        print a;
    }

    showA();
    var a = "block";
    showA();
    }
 * ```
 * 
 * print a; here should always refer to 'global', the lexical
 * scope for this variable when the function was declared.
 * 
 * A Resolver performs a static pass on the code
 * to inject extra information to aid the interpreter 
 * perform the correct resolving.
 * 
 * The variant we went with counts how many
 * environments away the variable we're using at.
 * 
 * So in the example it tells the interpreter that
 * var a in the function is from an environment that's
 * 2 hops backward from the variable's usage. (the global env)
 * 
 * P.S: In the actual implementation global variable usages
 * are given a distance of null, which implicitly means use
 * the global env.
 */
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;

    /*
     * A stack to track the lexical scopes
     * we encounter through out the code.
     * Similar to how the interpreter does
     * with chains of environments but using
     * a stack.
     */
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    /*
     * To help track if we are in a fn
     * or not. To prevent returns from
     * outside a fn.
     */
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE, FUNCTION
    }

    // Statements

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Class stmt) {
        declare(stmt.name);
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Function stmt) {
        /*
         * Define a fn eagerly to allow
         * referring to it to recurse inside it's
         * own body.
         */
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);

        return null;
    }

    @Override
    public Void visitIfStmt(If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.thenBranch != null)
            resolve(stmt.thenBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null)
            resolve(stmt.value);
        return null;
    }

    @Override
    public Void visitWhileStmt(While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) {
        declare(stmt.name);

        if (stmt.initializer != null)
            resolve(stmt.initializer);

        define(stmt.name);

        return null;
    }

    // Expressions

    @Override
    public Void visitVariableExpr(Variable expr) {

        /*
         * Resolves a variable usage to the it's
         * corresponding declaration.
         * 
         * A variable has it's defined flag set to
         * false during the period in which it's initializer
         * is still running.
         * If we try to access it during that time we should
         * throw an error.
         * 
         * var a = a + 1 doesn't make sense
         */
        if (!scopes.empty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments)
            resolve(argument);
        return null;
    }

    @Override
    public Void visitGetExpr(Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSetExpr(Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Unary expr) {
        resolve(expr.right);
        return null;
    }

    // Entry function

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements)
            resolve(statement);
    }

    // Utilities

    private void resolveFunction(Function function, FunctionType type) {

        /*
         * Stash the old value because functions
         * can nest arbitrarily.
         */
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    /*
     * Similar to evaluate() & execute()
     * in the interpreter. They apply the
     * visitor to the given node.
     */
    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }

    /*
     * Utils to handle variable declaration & defining
     */

    private void declare(Token name) {
        if (scopes.isEmpty())
            return;

        Map<String, Boolean> scope = scopes.peek();

        if (scope.containsKey(name.lexeme))
            Lox.error(name, "Already a variable with this name in this scope.");

        // Variable is in environment but not defined yet
        scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        if (scopes.isEmpty())
            return;
        scopes.peek().put(name.lexeme, true);
    }

    private void resolveLocal(Expr expr, Token name) {
        /*
         * Walk backwards through the stacked scopes
         * If we find it, we resolve it with how far
         * it is from the place that uses it.
         * (0 = current scope, 1 = encolsing scope, etc...)
         */
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

}
