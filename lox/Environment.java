package lox;

import java.util.Map;
import java.util.HashMap;

public class Environment {
    // The parent scope
    final Environment enclosing;

    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme))
            return values.get(name.lexeme);

        /*
         * If a var is not in this local scope
         * & we have an enclosing scope
         * Walk up the chain till we find it
         * 
         * This adds shadowing & getting vars
         * from enclosing scopes
         */
        if (enclosing != null)
            return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");

    }

    /*
     * assign() is not allowed to define
     * a new variable, only modify existing ones
     */
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // Modify the enclosing state if var isnt local
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
