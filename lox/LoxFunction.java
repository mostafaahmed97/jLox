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

    LoxFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
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
            return returnValue.value;
        }

        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
