package lox;

import lox.Expr.*;
import lox.Stmt.*;
import lox.Stmt.Class;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/* 
 * The interpretor evaluates the AST that the parser produced
 *  it's implemented as a visitor instead of stuffing that
 * logic inside each node, similar to how we did the printer 
 */

/*
 * To support dynamic types in Lox
 * we use Object to correspond to our
 * variables in Java, in runtime we can
 * check what the variable's type is
 * bec. JVM supports type checking in
 * runtime
 */

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();

    // The runtime storage for variables
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {

        /*
         * Here we define the native functions.
         * Functions that are available in the global scope
         * and implemented in the runtime's language.
         * (vars and fns live in the same space, the environment)
         */

        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }

        });

    }

    /*
     * Takes in the syntax tree
     * generated by the parser & kicks
     * off the evalution chain.
     */
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements)
                execute(stmt);
        }
        // Catch our custom runtime exception
        catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    /*
     * A literal is our last stop,
     * similar to how primary was during
     * parsing
     */
    @Override
    public Object visitLiteralExpr(Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left))
                return left;
        }
        // If operation was an and
        else {
            // & left was not true then we short circuit
            if (!isTruthy(left))
                return left;
        }

        return evaluate(expr.right);
    }

    /*
     * A grouping expr only has one sub
     * expression, so we evaluate that
     * & return the value
     */
    @Override
    public Object visitGroupingExpr(Grouping expr) {
        return evaluate(expr.expression);
    }

    /*
     * Unary evaluates it's sub expr
     * and then does it's own action
     * (P.S: A unary can be ! or -)
     * 
     * We're basically doing a post order
     * traversal of the AST, an expr can't
     * evaluate it's value before it's children
     */
    @Override
    public Object visitUnaryExpr(Unary expr) {

        // Get the value for right sub expr
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                // We're casting to a double in runtime bec. we
                // can't know type statically
                return -(double) right;

            case BANG:
                return !isTruthy(right);

        }

        // Unreachable
        return null;
    }

    @Override
    public Object visitVariableExpr(Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    /*
     * Get the variable from the environment at
     * the distance that was specified by the
     * resolver.
     * 
     * Resolver only resolved local variables,
     * so if there's no distance we assume it's
     * a global variable.
     */
    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);

        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {

        // This order is part of the language semantics
        // Binary expressions are evaluated left to right
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperand(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperand(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double) left <= (double) right;
            case MINUS:
                checkNumberOperand(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double)
                    return (double) left + (double) right;

                if (left instanceof String && right instanceof String)
                    return (String) left + (String) right;

                if (left instanceof String || right instanceof String)
                    return left.toString() + right.toString();

                throw new RuntimeError(
                        expr.operator,
                        "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperand(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperand(expr.operator, left, right);
                return (double) left * (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);

        }

        // Unreachable
        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double)
            return;

        throw new RuntimeError(operator, "Operand must be a number");
    }

    private void checkNumberOperand(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double)
            return;

        throw new RuntimeError(operator, "Operands must be numbers");
    }

    @Override
    public Object visitCallExpr(Call expr) {

        /*
         * evaluate() the callee in a call expr.
         * If it's an Identifier (variablExpr)
         * then it will be fetched from the environment.
         */
        Object callee = evaluate(expr.callee);

        /*
         * If it's not sth that implements a
         * LoxCallable (function, class), like a
         * string or a number, then we
         * probably shouldn't invoke it lol.
         */
        if (!(callee instanceof LoxCallable))
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments)
            arguments.add(evaluate(argument));

        /*
         * Cast the callee as LoxCallablle to use it's methods
         * It should be so bec we checked above
         */
        LoxCallable function = (LoxCallable) callee;

        if (arguments.size() != function.arity())
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Get expr) {

        // Evaluate the left hand of the dot from the env
        Object object = evaluate(expr.object);

        /*
         * If the thing whose property is
         * getting accessed is an instance
         * return the property, else it's
         * a runtime error.
         */
        if (object instanceof LoxInstance)
            return ((LoxInstance) object).get(expr.name);

        throw new RuntimeError(expr.name,
                "Only instances have properties.");

    }

    @Override
    public Object visitSetExpr(Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance))
            throw new RuntimeError(expr.name,
                    "Only instances have fields.");

        Object value = evaluate(expr.value);
        ((LoxInstance) object).set(expr.name, value);

        return null;
    }

    /*
     * This method defines what our language
     * considers truthy or falsey.
     */
    private boolean isTruthy(Object object) {
        if (object == null)
            return false;
        if (object instanceof Boolean)
            return (boolean) object;

        // Everything else is true, empty strings, empty lists, etc..
        return true;
    }

    /*
     * This method is as important as isTruhty()
     * It defines the notion of equality for Lox in
     * terms of it's Java implementation
     */
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null)
            return true;
        if (a == null)
            return false;

        // No implicit conversions
        // '3' != 3
        return a.equals(b);
    }

    /*
     * The same idea as print() in ASTPrinter.
     * Given an expression, this method calls
     * accept() of that expression & passes
     * to it this (which is the visitor instance).
     * That way the expression calls the appropriate
     * method for itself (visitBinaryExpr, visitUnaryExpr, etc..)
     * & returns the value.
     */

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /*
     * For a statement
     * 1 - visitor calls accept() of stmt
     * and passes itself
     * 2 - statement calls correct handling for itself
     * from visitor (check Stmt.java)
     * 3 - we come back here, in visitExpressionStatement()
     * for example
     * 4 - call evaluate on the expression object
     * 5 - same idea until we reach the end
     */
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    @Override
    public Void visitBlockStmt(Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;

        try {
            this.environment = environment;

            for (Stmt statement : statements)
                execute(statement);
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitClassStmt(Class stmt) {
        environment.define(stmt.name.lexeme, null);

        /*
         * Convert the AST nodes for methodes within
         * class to runtime representations of LoxFunction
         * & pass them to the constructor of the LoxClass
         */
        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(method, environment);
            methods.put(method.name.lexeme, function);
        }

        LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    /*
     * Adds the runtime representation of a
     * function declaration to the environment.
     * 
     * The current environment is passed to be the
     * closure for the function. This builds a chain
     * of environments through the invocations that
     * starts from the global environment.
     */
    @Override
    public Void visitFunctionStmt(Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(If stmt) {
        if (isTruthy(evaluate(stmt.condition)))
            execute(stmt.thenBranch);
        else if (stmt.elseBranch != null)
            execute(stmt.elseBranch);

        return null;
    }

    @Override
    public Void visitWhileStmt(While stmt) {
        while (isTruthy(evaluate(stmt.condition)))
            execute(stmt.body);
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    /*
     * A return statement evaluates the expression
     * for it's value if present, then throws that
     * value as an exception to unwind the call stack.
     * 
     * This exception is caught in the LoxFunction
     * runtime implementation.
     */
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;

        if (stmt.value != null)
            value = evaluate(stmt.value);

        throw new Return(value);
    }

    // Declares a variable
    @Override
    public Void visitVarStmt(Var stmt) {
        Object value = null;
        if (stmt.initializer != null)
            value = evaluate(stmt.initializer);

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitAssignExpr(Assign expr) {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);

        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

        // Assignment is an expr not a statement, so it returns a value
        return value;
    }

    // A util to print a value based on it's runtime type
    // Another bridge between Lox & Java (like isEqual & isTruthy)
    private String stringify(Object object) {
        if (object == null)
            return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

}
