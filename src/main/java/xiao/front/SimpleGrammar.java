package xiao.front;

import org.jetbrains.annotations.NotNull;
import xiao.misc.Error;
import xiao.misc.Location;

import java.util.*;

import static xiao.front.Ast.Operator;
import static xiao.front.Ast.id;
import static xiao.front.Parser.Led;
import static xiao.front.Parser.Nud;
import static xiao.front.TokenType.*;
import static xiao.front.TokenType.Fixity.*;

/**
 * @author chuxiaofeng
 */
@SuppressWarnings("Convert2MethodRef")
public class SimpleGrammar implements Grammar {

    // todo token 相等包活结合性, 会导致同一个符号在同一个作用域可以定义两种结合性, 有问题

    /**
     * left binding powers
     */
    final Map<TokenType, Integer> lbps = new HashMap<>();

    final Map<TokenType, Nud> prefix = new HashMap<>();

    final Map<TokenType, Led> infix = new HashMap<>();

    final Set<TokenType> tokenTypes = new HashSet<>();

    SimpleGrammar() {
        init();
        reset();
    }

    void reset() {
        // 只内置了几个原始操作符, 且不允许重新定义
        tokenTypes.add(ASSIGN);
        tokenTypes.add(ARROW);
        tokenTypes.add(DOT);
        tokenTypes.add(UNARY_MINUS);
    }

    void init() {
        // 自定义操作符 prefix infixn infixr infixl
        // 可选优先级, 默认 20 最大
        // fixity   ->  infixl | infixr | infixn | prefix
        // operator ->  fixity [integer] op

        // todo 前缀和中缀操作符互相检查是否已经注册过对方...

        prefix(PREFIX, (p, tok) -> {
            Operator op = p.declareOperator(tok, Prefix);
            // 动态添加 token
            TokenType tokType = addTokenType(p, op);
            // 动态添加 语法规则
            prefix(tokType, (p1, tok1) -> p1.unary(tok1));
            return op;
        });

        prefix(INFIXN, (p, tok) -> {
            Operator op = p.declareOperator(tok, Infixn);
            // 动态添加 token
            TokenType tokType = addTokenType(p, op);
            // 动态添加 语法规则
            infix(tokType, (p1, lhs, tok1) -> p1.binaryN(lhs, fixOperators(tok1)));
            return op;
        });

        prefix(INFIXR, (p, tok) -> {
            Operator op = p.declareOperator(tok, Infixr);
            // 动态添加 token
            TokenType tokType = addTokenType(p, op);
            // 动态添加 语法规则
            infixRight(tokType, (p1, lhs, tok1) -> p1.binaryR(lhs, fixOperators(tok1)));
            return op;
        });


        prefix(INFIXL, (p, tok) -> {
            Operator op = p.declareOperator(tok, Infixl);
            // 动态添加 token
            TokenType tokType = addTokenType(p, op);
            // 动态添加 语法规则
            infixLeft(tokType, (p1, lhs, tok1) -> p1.binaryL(lhs, fixOperators(tok1)));
            return op;
        });


        prefix(NAME, (p, tok) -> p.name(tok));

        prefix(INT, (p, tok) -> p.litInt(tok));
        prefix(FLOAT, (p, tok) -> p.litFloat(tok));
        prefix(STRING, (p, tok) -> p.litStr(tok));

        prefix(UNARY_MINUS, (p, tok) -> p.unary(tok));

        prefix(LEFT_BRACKET, (p, tok) -> p.leftBracket(tok));
        prefix(LEFT_BRACE, (p, tok) -> p.blockParser.block(tok));
        prefix(LEFT_PAREN, (p, tok) -> p.leftParen(tok));

        prefix(TYPE, (p, tok) -> p.bindParser.type(tok));
        prefix(TYPEREC, (p, tok) -> p.bindParser.typeRecursive(tok));
        prefix(LET, (p, tok) -> p.bindParser.define(tok));
        prefix(LETREC, (p, tok) -> p.bindParser.defineRecursive(tok));
        prefix(FUN, (p, tok) -> p.funParser.defFun(tok));
        prefix(RECORD, (p, tok) -> p.recordParser.record(tok));

        prefix(BREAK, (p, tok) -> p.controlFlowParser.breakStmt(tok));
        prefix(CONTINUE, (p, tok) -> p.controlFlowParser.continueStmt(tok));
        prefix(FOR, (p, tok) -> p.controlFlowParser.forStmt(tok));
        prefix(IF, (p, tok) -> p.controlFlowParser.ifStmt(tok));
        prefix(RETURN, (p, tok) -> p.funParser.returnStmt(tok));
        prefix(WHILE, (p, tok) -> p.controlFlowParser.whileStmt(tok));
        prefix(MATCH, (p, tok) -> p.matchParser.matchStmt(tok));
        prefix(ASSERT, (p, tok) -> p.assertStmt(tok));
        prefix(DEBUGGER, (p, tok) -> p.debuggerStmt(tok));

        prefix(MODULE, (p, tok) -> p.moduleParser.module(tok));
        prefix(IMPORT, (p, tok) -> p.moduleParser.import1(tok));

        infixLeft(LEFT_PAREN, (p, expr, tok) -> p.funCallParser.callLeftParen(expr, tok));
        infixLeft(DOT, (p, expr, tok) -> p.assignParser.dot(expr, tok));
        infixRight(ASSIGN, (p, expr, tok) -> p.assignParser.assign(expr, tok));
        infixLeft(LEFT_BRACKET, (p, expr, tok) -> p.assignParser.subscript(expr, tok));
        infixRight(ARROW, (p, expr, tok) -> p.funParser.singleParamArrowFn(expr, tok));
    }

    public Nud prefixNud(@NotNull Token tok) {
        Nud nud = prefix.get(tok.type);
        if (nud == null) {
            if (Lexicon.isSymbol(tok.lexeme)) {
                // 这里等于把 `op` symbol 类型的操作符转变成 identifier, 从作用域把操作符对应的函数提取出来
                // 顺带可以支持中缀转前缀使用, 因为提取了操作符变成了函数
                return (p, t1) -> Ast.id(t1.loc, Lexicon.retrieveSymbol(tok.lexeme));
            } else {
                if (tok.type == EOF) {
                    throw Error.syntax(tok.loc, "EOF");
                } else {
                    throw Error.syntax(tok.loc, "文法错误, 没有声明的前缀操作符 " + tok);
                }
            }
        }
        return nud;
    }

    public Led infixLed(@NotNull Token tok) {
        Led led = infix.get(tok.type);
        if (led == null) {
            if (tok.type == EOF) {
                throw Error.syntax(tok.loc, "EOF");
            } else {
                throw Error.syntax(tok.loc, "文法错误, 没有声明的前缀操作符 " + tok.type);
            }
        }
        return led;
    }

    public int infixLbp(@NotNull Token tok) {
        return lbps.getOrDefault(tok.type, 0);
    }

    final List<Runnable> clearTokenTypes = new ArrayList<>();

    Token fixOperators(Token tok) {
        if (Lexicon.isSymbol(tok.lexeme)) {
            String lexeme = Lexicon.retrieveSymbol(tok.lexeme);
            return new Token(tok.type, lexeme, tok.loc, tok.keep);
        } else {
            return tok;
        }
    }

    // 在当前作用域查重, 可以 shadow 父作用域
    TokenType addTokenType(Parser p, Operator op) {
        int prec = Math.toIntExact(op.precedence.value);
        int maxPrec = TokenType.maxBindingPower;
        if (prec > maxPrec) {
            throw Error.syntax(op.precedence.loc, "超过最大优先级: " + maxPrec);
        }
        TokenType tokType = TokenType.operator(op.operator.name, op.fixity, prec);

        // 离开作用域自动删除规则, 好像不能做成 parent 方式的, 跨作用域有优先级关系
        // e.g. 当前作用域声明 =, 父作用域声明 == 需要优先匹配 ==, 等验证一下...
        Runnable clear = p.lexer.lexicon.operator(tokType);
        clearTokenTypes.add(clear);

        if (tokenTypes.add(tokType)) {
            return tokType;
        } else {
            throw Error.syntax(Location.None, "token " + tokType + " 重复添加");
        }
    }

    /**
     * 注册 前缀操作符(都是右结合)
     */
    void prefix(@NotNull TokenType type, @NotNull Nud nud) {
        if (prefix.put(type, nud) != null) {
            throw Error.syntax(Location.None, "重复注册 prefix: " + type.name);
        }
        lbps.put(type, 0);
    }

    /**
     * 注册 不结合中缀操作符
     */
    void infix(@NotNull TokenType type, @NotNull Led led) {
        if (infix.put(type, led) != null) {
            throw Error.syntax(Location.None, "重复注册 infix: " + type.name);
        }
        lbps.put(type, type.precedence);
    }

    /**
     * 注册 右结合中缀操作符
     */
    void infixRight(@NotNull TokenType type, @NotNull Led led) {
        infix(type, led);
    }

    /**
     * 注册 左结合中缀操作符
     */
    void infixLeft(@NotNull TokenType type, @NotNull Led led) {
        infix(type, led);
    }

    /**
     * 后缀操作符（可以看成中缀操作符木有右边操作数）(都是左结合)
     */
    void postfix(@NotNull TokenType type, @NotNull Led led) {
        infix(type, led);
    }
}
