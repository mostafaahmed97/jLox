package lox;

import java.util.List;

/*
 * This class will represent a lox function
 * during runtime. When the interpreter wants to 
 * define a func in the environment it will use
 * this representation.
 * 
 * Then later when an expression gets it from
 * there it can invoke it.
 */
public class LoxFunction implements LoxCallable {

    private final Stmt.Function declaration;
    private final Environment closure;

    /*
     * A helper flag to know if we're in a
     * constructor or not, if that's the case
     * then return *this*
     */
    private final boolean isInitializer;

    LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    /*
     * Enclose the method with an environment
     * that has this defined. Where this is
     * the parent instance of the method.
     * 
     */
    public LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer);
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    /*
     * Each function call get it's own environment
     * that is a child of the global env.
     * 
     * This how a function encapsulates it's parameters
     * within it's scope.
     */
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {

        Environment environment = new Environment(closure);

        for (int i = 0; i < declaration.params.size(); i++) {
            Token ithParam = declaration.params.get(i);
            environment.define(ithParam.lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            if (isInitializer)
                return closure.getAt(0, "this");

            return returnValue.value;
        }

        if (isInitializer)
            return closure.getAt(0, "this");

        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
