package xiao.front;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static xiao.front.TokenType.Fixity.*;

/**
 * TokenType 和 Operator 合并到一起定义
 *
 * 几个维度描述 Operator：
 *  1. 类型：前缀、中缀、后缀
 *  2. 结合性：左结合、右结合、不结合
 *  3. 优先级：precedence
 *  4. 目数：arity
 *
 * 一些可选 KEYWORD
 *  throw, try, catch, finally
 *  match, case
 *  enum, class, struct
 *  static, extends, object, construct, new, this, super,
 *  sealed, final, private, protected, public
 *  namespace, export, module, then, with, as, end
 *  val val const let def mut mutable ref define set!
 * 一些可选 OPERATOR
 *  is, instanceof, typeof, in
 * @author chuxiaofeng
 */
final public class TokenType {

    // Associativity
    public enum Fixity {
        Prefix, Infixn, Infixl, Infixr, Postfix
    }

    // =+=+=+=+=+=+=+=+=+=+ 空白和注释 SPACE COMMENT =+=+=+=+=+=+=+=+=+=+=+
    public final static TokenType WHITESPACE = new TokenType("<space>");
    public final static TokenType EOF = new TokenType("<eof>");
    public final static TokenType BLOCK_COMMENT = new TokenType("/*");
    public final static TokenType LINE_COMMENT = new TokenType("//");

    // =+=+=+=+=+=+=+=+=+=+ 分隔符 SEPARATOR =+=+=+=+=+=+=+=+=+=+=+
    public final static TokenType COLON = new TokenType(":"); // todo 可以用 as 表示!!!
    public final static TokenType ARROW_BLOCK = new TokenType("->");
    public final static TokenType COMMA = new TokenType(",");
    public final static TokenType SEMICOLON = new TokenType(";");
    public final static TokenType NEWLINE = new TokenType("\n");
    /**
     * LED 作为CALL是左结合中缀操作符 CALL("(", 220, LEFT)
     * NUD 作为GROUPING是前缀操作符符号 GROUPING("(", 0, NA)
     * todo 这里直接声明 InfixL 不对
     */
    public final static TokenType LEFT_PAREN = new TokenType("(", Infixl, 12);
    public final static TokenType RIGHT_PAREN = new TokenType(")");
    /**
     * LED 作为属性访问是左结合中缀操作符 COMPUTED_MEMBER_ACCESS("[", 230, LEFT),
     * NUD 作为数组字面量是前缀操作符 VECTOR("[", 0),
     * todo 这里直接声明 InfixL 不对
     */
    public final static TokenType LEFT_BRACKET = new TokenType("[", Infixl, 13);
    public final static TokenType RIGHT_BRACKET = new TokenType("]");
    /**
     * 作为BLOCK是前缀操作符 BLOCK("{"),
     * 作为RECORD是前缀操作符 RECORD_LITERAL("{"),
     */
    public final static TokenType LEFT_BRACE = new TokenType("{", Prefix, 1);
    public final static TokenType RIGHT_BRACE = new TokenType("}");


    // =+=+=+=+=+=+=+=+=+=+ 标识符 IDENTIFIER =+=+=+=+=+=+=+=+=+=+=+
    public final static TokenType NAME = new TokenType("<name>");


    // =+=+=+=+=+=+=+=+=+=+ 字面量 LITERAL =+=+=+=+=+=+=+=+=+=+=+
    // true, false 不需要 TokenType, 是因为就是普通的值, 可以通过 "true", "false" 的 name 去 scope 中查找
    public final static TokenType FLOAT = new TokenType("<float>");
    public final static TokenType INT = new TokenType("<int>");
    public final static TokenType STRING = new TokenType("<string>");


    // =+=+=+=+=+=+=+=+=+=+ 算子 OPERATOR =+=+=+=+=+=+=+=+=+=+=+
    // "(?:`[^`]*`)|(?:[\\Q:!#$%^&*+./<=>?@\\ˆ|-~\\E]+)"
    public final static TokenType OPERATORS = new TokenType("<operators>"); // ! 注意不能直接 eat
    public final static TokenType ASSIGN = new TokenType("=", Infixn, 2); // assignment, 不结合
    public final static TokenType ARROW = new TokenType("=>", Infixr, 13);
    public final static TokenType DOT = new TokenType(".", Infixl, 13); // MEMBER_ACCESS
    public final static TokenType UNARY_MINUS = new TokenType("-", Prefix, 10);

    // =+=+=+=+=+=+=+=+=+=+ 关键词 KEYWORD =+=+=+=+=+=+=+=+=+=+=+
    public final static TokenType PREFIX = new TokenType("prefix");
    public final static TokenType INFIXN = new TokenType("infixn");
    public final static TokenType INFIXL = new TokenType("infixl");
    public final static TokenType INFIXR = new TokenType("infixr");

    public final static TokenType RECORD = new TokenType("record");
    public final static TokenType EXTENDS = new TokenType("extends");

    public final static TokenType BREAK = new TokenType("break");
    public final static TokenType CONTINUE = new TokenType("continue");
    public final static TokenType RETURN = new TokenType("return");

    public final static TokenType IF = new TokenType("if");
    public final static TokenType ELSE = new TokenType("else");
    public final static TokenType FOR = new TokenType("for");
    // todo do while 没有处理, 或者不支持...
    public final static TokenType DO = new TokenType("do");
    public final static TokenType WHILE = new TokenType("while");
    public final static TokenType MATCH = new TokenType("match");
    public final static TokenType CASE = new TokenType("case");

    public final static TokenType TYPE = new TokenType("type");
    public final static TokenType TYPEREC = new TokenType("typerec");
    public final static TokenType LET = new TokenType("let");
    public final static TokenType LETREC = new TokenType("letrec");
    public final static TokenType MUT = new TokenType("mut");
    public final static TokenType FUN = new TokenType("fun");

    public final static TokenType MODULE = new TokenType("module");
    public final static TokenType IMPORT = new TokenType("import");
    public final static TokenType FROM = new TokenType("from");
    public final static TokenType AS = new TokenType("as", Infixn, 6);

    public final static TokenType ASSERT = new TokenType("assert");
    public final static TokenType DEBUGGER = new TokenType("debugger");

    public final String name;
    public final @Nullable Fixity fixity;
    public final int precedence; // binding power

    final static int maxBindingPower = 20;

    private TokenType(String name) {
        this.name = name;
        this.fixity = null;
        this.precedence = -1;
    }

    private TokenType(String name, @Nullable Fixity fixity, int precedence) {
        this.name = name;
        this.fixity = fixity;
        this.precedence = precedence;
    }

    public static TokenType operator(@NotNull String name, @NotNull Fixity fixity, int precedence) {
        return new TokenType(name, fixity, precedence);
    }

    @SuppressWarnings("AliControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TokenType tokenType = (TokenType) o;
        // Lexicon.lbps 用 TokenType 做 key, 必须同时考虑 fixity , e.g. -
        if (!name.equals(tokenType.name)) return false;
        return fixity == tokenType.fixity;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (fixity != null ? fixity.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", fixity=" + fixity +
                ", prec=" + precedence +
                '}';
    }
}


//    public final static TokenType BACKTICK = new TokenType("`", SEPERATOR, BP_NONE, NA);
//    public final static TokenType OBJECT = new TokenType("object");
//    public final static TokenType VECTOR = new TokenType("vector");

//    public final static TokenType OPT_DOT = new TokenType("?.", OPERATOR, BP_MEMBER, LEFT); // OPTIONAL_CHAINING
//    public final static TokenType IS = new TokenType("is", OPERATOR, BP_IS, LEFT); // INSTANCE
//    public final static TokenType IN = new TokenType("in", OPERATOR, BP_IS, LEFT);
//    public final static TokenType INSTANCEOF = new TokenType("instanceof", OPERATOR, BP_IS, LEFT); // INSTANCE
//    public final static TokenType AS = new TokenType("as", OPERATOR, BP_IS, LEFT);
//    public final static TokenType TYPEOF = new TokenType("typeof", KEYWORD, 200, RIGHT);
//    public final static TokenType YIELD = new TokenType("yield", KEYWORD, 60, RIGHT);
//    public final static TokenType AT = new TokenType("@", OPERATOR, BP_MEMBER, RIGHT);

// =+=+=+=+=+=+=+=+=+=+ 类型 =+=+=+=+=+=+=+=+=+=+=+
// 类型都是值不需要token, 用 name 直接从 scope 中获取
//    public final static TokenType TYPE_BOOL = new TokenType("Bool", KEYWORD, BP_NONE, NA);
//    public final static TokenType TYPE_INT = new TokenType("Int", KEYWORD, BP_NONE, NA);
//    public final static TokenType TYPE_FLOAT = new TokenType("Float", KEYWORD, BP_NONE, NA);
//    public final static TokenType TYPE_STRING = new TokenType("String", KEYWORD, BP_NONE, NA);

// =+=+=+=+=+=+=+=+=+=+ 算子 =+=+=+=+=+=+=+=+=+=+=+
//    public final static TokenType PLUS = new TokenType("+", OPERATOR, BP_TERM, LEFT);
//    public final static TokenType MINUS = new TokenType("-", OPERATOR, BP_TERM, LEFT); // SUBTRACT | NEGATIVE
//    public final static TokenType UNARY_PLUS = new TokenType("+", OPERATOR, BP_PREFIX_UNARY, RIGHT);
//    public final static TokenType UNARY_MINUS = new TokenType("-", OPERATOR, BP_PREFIX_UNARY, RIGHT);
//    public final static TokenType BIT_NOT = new TokenType("~", OPERATOR, BP_PREFIX_UNARY, RIGHT); // TILDE
//    public final static TokenType LOGIC_NOT = new TokenType("!", OPERATOR, BP_PREFIX_UNARY, RIGHT);


// =+=+=+=+=+=+=+=+=+=+ 算子：算术 =+=+=+=+=+=+=+=+=+=+=+
//    public final static TokenType MUL = new TokenType("*", OPERATOR, BP_FACTOR, LEFT); // MULTIPLY | STAR | ASTERISK
//    public final static TokenType DIV = new TokenType("/", OPERATOR, BP_FACTOR, LEFT); //   DIVIDE | SLASH
//    public final static TokenType MOD = new TokenType("%", OPERATOR,BP_FACTOR, LEFT); // MODULE | PERCENT
//    public final static TokenType POWER = new TokenType("**", OPERATOR, BP_EXP, RIGHT); // EXPONENTIATION
// =+=+=+=+=+=+=+=+=+=+ 算子：比较 =+=+=+=+=+=+=+=+=+=+=+
//    public final static TokenType GT = new TokenType(">", OPERATOR, BP_COMP, LEFT);
//    public final static TokenType LT = new TokenType("<", OPERATOR, BP_COMP, LEFT);
//    public final static TokenType LE = new TokenType("<=", OPERATOR, BP_COMP, LEFT);
//    public final static TokenType GE = new TokenType(">=", OPERATOR, BP_COMP, LEFT);
//    public final static TokenType EQ = new TokenType("==", OPERATOR, BP_EQ, LEFT);
//    public final static TokenType NE = new TokenType("!=", OPERATOR, BP_EQ, LEFT);
//    public final static TokenType STRICT_EQ = new TokenType("===", OPERATOR, BP_EQ, LEFT);
//    public final static TokenType STRICT_NE = new TokenType("!==", OPERATOR, BP_EQ, LEFT);

// =+=+=+=+=+=+=+=+=+=+ 算子：位移 =+=+=+=+=+=+=+=+=+=+=+
//    public final static TokenType GT_GT = new TokenType(">>", OPERATOR, BP_BIT_SHIFT, RIGHT); // BITWISE_RIGHT_SHIFT
//    public final static TokenType LT_LT = new TokenType("<<", OPERATOR, BP_BIT_SHIFT, RIGHT); // BITWISE_LEFT_SHIFT
//    public final static TokenType LT_LT_LT = new TokenType("<<<", OPERATOR, BP_BIT_UNSIGNED_SHIFT, RIGHT); // UNSIGNED_BITWISE_LEFT_SHIFT
//    public final static TokenType GT_GT_GT = new TokenType(">>>", OPERATOR, BP_BIT_UNSIGNED_SHIFT, RIGHT); // UNSIGNED_BITWISE_RIGHT_SHIFT

// =+=+=+=+=+=+=+=+=+=+ 算子：三元 =+=+=+=+=+=+=+=+=+=+=+
//    public final static TokenType COND = new TokenType("?", OPERATOR, BP_COND, RIGHT); // CONDITIONAL | QUESTION

// =+=+=+=+=+=+=+=+=+=+ 算子：Updater =+=+=+=+=+=+=+=+=+=+=+
//    public final static TokenType PREFIX_INCR = new TokenType("++", OPERATOR, BP_PREFIX_UNARY, RIGHT); // PREFIX_PLUS_PLUS
//    public final static TokenType PREFIX_DECR = new TokenType("--", OPERATOR, BP_PREFIX_UNARY, RIGHT); // PREFIX__SUB_SUB
//    public final static TokenType POSTFIX_INCR = new TokenType("++", OPERATOR, BP_POSTFIX_UNARY, NA); // POSTFIX_PLUS_PLUS
//    public final static TokenType POSTFIX_DECR = new TokenType("--", OPERATOR, BP_POSTFIX_UNARY, NA); //  POST_SUB_SUB

// =+=+=+=+=+=+=+=+=+=+ 算子：赋值 =+=+=+=+=+=+=+=+=+=+=+
//    public final static TokenType ASSIGN = new TokenType("=", OPERATOR, BP_ASSIGN, RIGHT); // assignment
//    public final static TokenType ASSIGN_PLUS = new TokenType("+=", OPERATOR, BP_ASSIGN, RIGHT);
//    public final static TokenType ASSIGN_SUB = new TokenType("-=", OPERATOR, BP_ASSIGN, RIGHT);
//    public final static TokenType ASSIGN_MUL = new TokenType("*=", OPERATOR, BP_ASSIGN, RIGHT);
//    public final static TokenType ASSIGN_DIV = new TokenType("/=", OPERATOR, BP_ASSIGN, RIGHT);
//    public final static TokenType ASSIGN_MOD = new TokenType("%=", OPERATOR, BP_ASSIGN, RIGHT);
//    public final static TokenType ASSIGN_LEFT_SHIFT = new TokenType("<<=", OPERATOR, BP_ASSIGN, RIGHT);
//    public final static TokenType ASSIGN_SIGNED_RIGHT_SHIFT = new TokenType(">>=", OPERATOR, BP_ASSIGN, RIGHT);
//    public final static TokenType ASSIGN_UNSIGNED_RIGHT_SHIFT = new TokenType(">>>=", OPERATOR, BP_ASSIGN, RIGHT);
//    public final static TokenType ASSIGN_BIT_OR = new TokenType("|=", OPERATOR, BP_ASSIGN, RIGHT);
//    public final static TokenType ASSIGN_BIT_XOR = new TokenType("^=", OPERATOR, BP_ASSIGN, RIGHT);
//    public final static TokenType ASSIGN_BIT_AND = new TokenType("&=", OPERATOR, BP_ASSIGN, RIGHT);

// =+=+=+=+=+=+=+=+=+=+ 算子：位运算 =+=+=+=+=+=+=+=+=+=+=+
//    public final static TokenType BIT_OR = new TokenType("|", OPERATOR, BP_BIT_OR, LEFT); // BITWISE_OR | PIPE
//    public final static TokenType BIT_XOR = new TokenType("^", OPERATOR, BP_BIT_XOR, LEFT); // BITWISE_XOR | CARET
//    public final static TokenType BIT_AND = new TokenType("&", OPERATOR, BP_BIT_AND, LEFT); // BITWISE_AND | AMP
//    public final static TokenType BIT_NOT = new TokenType("~", OPERATOR, BP_PREFIX_UNARY, RIGHT); // TILDE

// =+=+=+=+=+=+=+=+=+=+ 算子：Range =+=+=+=+=+=+=+=+=+=+=+
//    public final static TokenType RANGE = new TokenType("..", OPERATOR, BP_RANGE, LEFT); // DOT_DOT
//    public final static TokenType EXCLUSIVE_RANGE = new TokenType("...", OPERATOR, BP_RANGE, LEFT); // DOT_DOT

// =+=+=+=+=+=+=+=+=+=+ 算子：逻辑运算 =+=+=+=+=+=+=+=+=+=+=+
//    public final static TokenType LOGIC_NOT = new TokenType("!", OPERATOR, BP_PREFIX_UNARY, RIGHT);
//    public final static TokenType LOGIC_AND = new TokenType("&&", OPERATOR, BP_LOGIC_AND, LEFT); // AMP_AMP
//    public final static TokenType LOGIC_OR = new TokenType("||", OPERATOR, BP_LOGIC_OR, LEFT); // PIPE_PIPE