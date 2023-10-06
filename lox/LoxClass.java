package lox;

import java.util.Map;
import java.util.List;

/*
 * The runtime representation of a 
 * class in Lox.
 */
public class LoxClass {

    final String name;

    LoxClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "<class> " + name;
    }

}
