package lox;

import java.util.Map;
import java.util.List;

/*
 * The runtime representation of a 
 * class in Lox.
 */
public class LoxClass implements LoxCallable {

    final String name;

    /*
     * Methods are stored in LoxClass.
     * Instace properties are stored on LoxInstance
     */
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name))
            return methods.get(name);

        return null;
    }

    /*
     * The class' implementation of call, creates
     * & returns a new instance of the class
     */
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);

        /*
         * If class has a defined constructor
         * call it before returning the instance
         */
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer != null)
            return initializer.arity();

        return 0;
    }

    @Override
    public String toString() {
        return "<class> " + name;
    }

}
