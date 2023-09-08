# Glossary <!-- omit in toc -->

- [**Lexeme**](#lexeme)
- [**Lexical Grammar**](#lexical-grammar)
- [**Token**](#token)
- [**Scanner**](#scanner)
- [**CFG**](#cfg)
- [**Parser**](#parser)
- [**Precedence**](#precedence)
- [**Associativty**](#associativty)
- [**Top Down Parser**](#top-down-parser)


### **Lexeme**

The smallest meaningful unit in a language.

Can be an identifier, bracket, operator, etc...

Ex: `(` , `{`, `if`, `while`, variable names are all valid lexemes

<br>

### **Lexical Grammar**

Rules about how the language groups characters into lexemes

Usually expressed as a regular grammar

Lexical = مفردات
Grammar = قواعد

The **scanner** implements this grammar

<br>

### **Token**

Token is a lexeme and some metadata associated with it to help the next steps like

- Type of lexeme: bracket, equal operator,
- Location in code, line & offset
- Value if it's a number or a string literal

<br>

### **Scanner**

Consumes a set of characters & outputs a list of tokens

<br>

### **CFG**

A set of rules that combine the tokens into meaningful expressions

Basically the rules to form sentences of a language.

The rules are usually called productions. Rules are terminal or non-terminal.

Ex:

```
expression     → literal
               | unary
               | binary
               | grouping ;

literal        → NUMBER | STRING | "true" | "false" | "nil" ;
grouping       → "(" expression ")" ;
unary          → ( "-" | "!" ) expression ;
binary         → expression operator expression ;
operator       → "==" | "!=" | "<" | "<=" | ">" | ">="
               | "+"  | "-"  | "*" | "/" ;
```

The CFG is implemented in the **parser**

<br>

### **Parser**

Consumes a set of tokens & outputs a syntax tree of expressions

<br>


### **Precedence**

Which operators are evaluated first in an expression having multiple different operators

ex: multiply before adding

<br>

### **Associativty** 

Order of evaluation in a sequence of the same operator

ex: adding is right-associative & assigning is left-associative

```
5 - 3 - 1 --> (5 - 3) - 1

a = b = c --> a = (b = c)
```

<br>

### **Top Down Parser**

A parser that starts from the top rule in CFG (one with the least precedence) & works it's way to the leaves of the syntax tree & then back.

<br>