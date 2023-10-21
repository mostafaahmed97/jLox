# jLox

This is my implementation of jLox from [Robert Nystrom's](https://journal.stuffwithstuff.com/) book [Crafting Interpreters](https://craftinginterpreters.com/)

The book is really awesome if you want to learn more about compilers in a hands on way. 

It's been awesome following along & Robert is really good at breaking down concepts in a simple & practical way.

**Lox** is a dynamically typed high level language. **jLox** is an interpreter for the Lox language written in Java.

Lox has the following features :

-  Primitive data types
   -  Boolean
   -  Number
   -  String
   -  Null
-  Expressions
   -  Arithmetic
   -  Comparison & Equality
   -  Logical Operators (and, or)
   -  Precedence & Grouping
-  Variables
-  Control Flow
   -  If statement
   -  For loops
   -  While loops
-  Functions
-  Classes
   -  Inheritance

While building the jLox interpreter we implement the following compiler components :

- Scanner
- Parser
- Resolver
- Interpreter