package xiao.front;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiao.Scope;
import xiao.misc.Error;
import xiao.misc.Helper;
import xiao.misc.Location;
import xiao.misc.Pair;

import java.util.*;

import static xiao.Constant.*;
import static xiao.front.Ast.*;
import static xiao.front.TokenType.*;
import static xiao.misc.Helper.join;
import static xiao.misc.Helper.lists;
import static xiao.misc.Location.*;

/**
 * Semantic Analysis
 * parser :: stream<tok> -> tree<node>
 *
 * ref: Top Down Operator Precedence Parser (Pratt Parser)
 * ref: Operator Precedence Grammar
 *
 *
 * @author chuxiaofeng
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
public class Parser {

    /**
     * 处理字面量、变量、前缀操作符
     */
    interface Nud {
        @NotNull Node parse(@NotNull Parser p, @NotNull Token tok);
    }

    /**
     * 处理中缀、后缀操作符
     */
    interface Led {
        @NotNull Node parse(@NotNull Parser p, @NotNull Node left, @NotNull Token tok);
    }

    static class PreParser extends Lexer {
        PreParser(@NotNull Lexicon lexicon) {
            super(lexicon);
        }

        boolean isOperator(Token tok) {
            return tok.type == OPERATORS || Lexicon.isOperators(tok.lexeme);
        }

        boolean isMinus(Token tok) {
            return "-".equals(tok.lexeme);
        }

        @Override
        public Token next() {
            // 修正下 `-`  一元前缀操作符
            @Nullable Token pre = next.prev == null ? null : next.prev.token();
            Token cur = next.token();
            if (isMinus(cur)) {
                if (pre == null || pre.type == NEWLINE) {
                    // -expr
                    cur.type = UNARY_MINUS;
                } else if (isOperator(pre)) {
                    // op -expr
                    // expr op -expr
                    cur.type = UNARY_MINUS;
                } else if (pre.type != INT && pre.type != FLOAT && pre.type != NAME && pre.type != RIGHT_PAREN) {
                    // id - expr
                    // ) - expr
                    cur.type = UNARY_MINUS;
                } else {
                    // cur.type = OPERATORS;
                }
            }
            return super.next();
        }
    }


    public final @NotNull Lexer lexer;

    final @NotNull Grammar grammar;

    public Parser(@NotNull Lexicon lexicon, @NotNull Grammar grammar) {
        this.lexer = new PreParser(lexicon);
        this.grammar = grammar;
    }

    int inTypeExpr = 0;

    Node expr() {
        return expr(0);
    }

    Node expr(int rbp) {
        Token tok = lexer.eat();
        Node left = grammar.prefixNud(tok).parse(this, tok);
        Node expr;
        if (inTypeExpr > 0) {
            expr = infixForTypeDeclare(left, rbp);
        } else {
            expr = infix(left, rbp);
        }
        infixnCheck(expr);
        return expr;
    }

    Node typeExpr(int rbp) {
        inTypeExpr++;
        try {
            return expr(rbp);
        } finally {
            inTypeExpr--;
        }
    }

    Node typeExpr() {
        inTypeExpr++;
        try {
            return expr();
        } finally {
            inTypeExpr--;
        }
    }

    void infixnCheck(Node expr) {
        if (expr instanceof Assign) {
            Assign assign = (Assign) expr;
            if (assign.pattern instanceof Assign) {
                // 这一段应该走不到, 因为 = 是左结合的
                Assign assign1 = (Assign) assign.pattern;
                throw Error.syntax(assign1.oploc, "操作符不结合");
            }
            if (assign.value instanceof Assign) {
                Assign assign1 = (Assign) assign.value;
                throw Error.syntax(assign1.oploc, "操作符不结合");
            }
        }
        if (expr instanceof Binary) {
            Binary bin = (Binary) expr;
            String op = bin.operator.name;
            if (bin.fixity == Fixity.Infixn) {
                if (bin.lhs instanceof Binary) {
                    Binary lhs = (Binary) bin.lhs;
                    if (lhs.operator.name.equals(op)) {
                        throw Error.syntax(lhs.oploc, "操作符不结合");
                    }
                }
                if (bin.rhs instanceof Binary) {
                    Binary rhs = (Binary) bin.rhs;
                    if (rhs.operator.name.equals(op)) {
                        throw Error.syntax(rhs.oploc, "操作符不结合");
                    }
                }
            }
        }
    }

    Node infix(@NotNull Node left, int rbp) {
        leftTrimLines();
        // 判断下一个 tok 是否要绑定 left ( 优先级 > left)
        while (grammar.infixLbp(lexer.peek()) > rbp) {
            Token tok = lexer.eat();
            left = grammar.infixLed(tok).parse(this, left, tok);
            // trim lines
        }
        return left;
    }

    // 类型声明时候, 遇到 =、=> 会构成 assign, 所以特殊处理
    // (因为 assign 支持 pattern 所以 Int|Str 会延迟到 pattern 处理阶段才会报错)
    // e.g. fun() : ( Int|Str = { "" } )
    // e.g. let a : ( Int|Str = 1 )
    // e.g. () : (Int|Str => { })
    Node infixForTypeDeclare(@NotNull Node left, int rbp) {
        leftTrimLines();
        while (grammar.infixLbp(lexer.peek()) > rbp) {
            Token peeked = lexer.peek();
            if (peeked.is(ASSIGN) || peeked.is(ARROW)) {
                return left;
            }
            Token tok = lexer.eat();
            left = grammar.infixLed(tok).parse(this, left, tok);
        }
        return left;
    }

    // a\n=>1 a\n=1 a\n.b=1 ...
    void leftTrimLines() {
        int i = 0;
        Token tok;
        while ((tok = lexer.tryPeek(i++)) != null) {
            if (tok.type != NEWLINE) {
                break;
            }
        }
        if (tok != null) {
            // 不处理 -, 必须换行
            if (tok.lexeme.equals(UNARY_MINUS.name)) {
                return;
            }
            if (Lexicon.isOperators(tok.lexeme) && i > 1) {
                lexer.skip(i - 1);
            }
        }
    }



    public Block parse(@NotNull String input) {
        lexer.tokenize(input);
        try {
            return program();
        } catch (Error error) {
            if (error.loc != null) {
                Helper.err(error.loc.inspect(error.getMessage()));
            }
            throw error;
        }
    }

    public Block prelude(String prelude) {
        lexer.tokenize(prelude);
        List<Node> stmts = new ArrayList<>();
        Block program = stmtsWithoutScope(lexer.peek().loc, stmts, EOF);
        lexer.eat(EOF);
        return program;
    }

    Block program() {
        List<Node> stmts = new ArrayList<>();
        Block program = stmts(lexer.peek().loc, stmts, EOF);
        lexer.eat(EOF);
        return program;
    }

    Block stmts(Location from, List<Node> stmts, TokenType until) {
        return grammar.scope(this, () -> stmtsWithoutScope(from, stmts,  until));
    }

    Block stmtsWithoutScope(Location from, List<Node> stmts, TokenType until) {
        tryEatLines();
        while (lexer.peek(until) == null) {
            stmts.add(stmt());
            tryEatLines();
        }
        return block(rangeNodes(from, stmts), stmts);
    }

    Node stmt() {
        return expr();
    }

    boolean tryEatLines() {
        if (lexer.tryEat(NEWLINE) == null) {
            return false;
        }
        //noinspection StatementWithEmptyBody,AliControlFlowStatementWithoutBraces
        while (lexer.tryEat(NEWLINE) != null);
        return true;
    }

    @Nullable Token peekLastNonLine(int from) {
        try {
            while (true) {
                Token tok = lexer.peek(from);
                if (tok.type == NEWLINE) {
                    from--;
                } else {
                    return tok;
                }
            }
        } catch (IndexOutOfBoundsException ignored) {
            return null;
        }

    }

    Block exprBlock() {
        tryEatLines();
        return exprBlock(expr());
    }

    Block exprBlock(@NotNull Node expr) {
        return block(expr.loc, lists(expr));
    }


    // todo 这里有 bug, 如果 redefine true false
    Node name(@NotNull Token tok) {
        Id id = id(tok.loc, tok.lexeme);
        if (ID_TRUE.equals(tok.lexeme) || ID_FALSE.equals(tok.lexeme)) {
            return lit(id);
        } else {
            return id;
        }
    }

    Node litInt(@NotNull Token tok) {
        IntNum lit = litIntNum(tok.loc, tok.lexeme);
        return lit(lit);
    }

    Node litFloat(@NotNull Token tok) {
        FloatNum lit = litFloatNum(tok.loc, tok.lexeme);
        return lit(lit);
    }

    Node litStr(@NotNull Token tok) {
        Str lit = Ast.litStr(tok.loc, tok.lexeme);
        return lit(lit);
    }

    Node lit(Node node) {
        if (inTypeExpr > 0) {
            return litType(node);
        } else {
            return node;
        }
    }

    Unary unary(@NotNull Token tok) {
        tryEatLines();
        Node arg = expr(tok.type.precedence);
        return Ast.unary(range(tok.loc, arg.loc), tok.loc, id(tok.loc, tok.lexeme), arg, true);
    }

    Binary binaryL(@NotNull Node lhs, @NotNull Token tok) {
        tryEatLines();
        Node rhs = expr(tok.type.precedence);
        Id bin = id(tok.loc, tok.lexeme);
        return Ast.binary(range(lhs.loc, rhs.loc), tok.loc, tok.type.fixity, bin, lhs, rhs);
    }

    Binary binaryR(@NotNull Node lhs, @NotNull Token tok) {
        tryEatLines();
        Node rhs;
        // 支持 as 字面类型,  a as 1|2|3
        // todo as 如果重写会有 bug
        if (ID_AS.equals(tok.lexeme)) {
            rhs = typeExpr(tok.type.precedence - 1);
        } else {
            rhs = expr(tok.type.precedence - 1);
        }
        Id bin = id(tok.loc, tok.lexeme);
        return Ast.binary(range(lhs.loc, rhs.loc), tok.loc, tok.type.fixity, bin, lhs, rhs);
    }

    Binary binaryN(@NotNull Node lhs, @NotNull Token tok) {
        return binaryR(lhs, tok); // 这里 binaryL + R 其实无所谓, 之后还得查一遍 infixnCheck
    }

    Ast.Operator declareOperator(@NotNull Token tok, @NotNull Fixity fixity) {
        Token intNum = lexer.tryEat(INT);
        if (intNum == null) {
            String maxPrec = String.valueOf(maxBindingPower);
            intNum = new Token(INT, maxPrec, Location.None, true);
        }
        Token tokOP = tryEatDeclaredOp();
        if (tokOP == null) {
            throw Error.syntax(lexer.peek().loc, "操作符不合法, 必须满足: " + Lexicon.operatorsReg);
        }
        String opName = tokOP.lexeme;
        IntNum precNum = litIntNum(intNum.loc, intNum.lexeme);
        Id op = id(tokOP.loc, opName);
        return Ast.operator(range(tok.loc, tokOP.loc), fixity, precNum, op);
    }

    @Nullable Token tryEatDeclaredOp() {
        // 这里会影声明操作符的顺序
        // e.g. infix 170 +  infixl 170 +++  如果不处理 后面的 +++ 会被处理成 +
        // 而匹配的顺序没有影响, 因为操作符规则 已经处理成 TreeSet 了
        int i = 0;
        List<Token> toks = new ArrayList<>();
        while (true) {
            Token tok = lexer.peek(i++);
            if (tok.type == NEWLINE || tok == Token.EOF) {
                break;
            }
            if (!toks.isEmpty()) {
                Token last = toks.get(toks.size() - 1);
                boolean skipSth = tok.loc.idxBegin > last.loc.idxEnd;
                if (skipSth) {
                    break;
                }
            }
            toks.add(tok);
        }
        String lexeme = join(toks, "");
        if (Lexicon.isOperators(lexeme)) {
            lexer.skip(i - 1);
            return new Token(OPERATORS, lexeme, range(toks), true);
        }
        return null;
    }

    @Nullable Token tryEatAnyOperator() {
        if (Lexicon.isOperators(lexer.peek().lexeme)) {
            Token ops = lexer.eat();
            return new Token(OPERATORS, ops.lexeme, ops.loc, true);
        }
        return null;
    }

    // 忽略 token 类型, 不受 infix? 定义影响
    @Nullable Token tryEatOperator(@NotNull TokenType operator) {
        Token ops = lexer.peek();
        if (Lexicon.isOperators(ops.lexeme)) {
            if (operator.name.equals(ops.lexeme)) {
                lexer.eat();
                return new Token(operator, ops.lexeme, ops.loc, true);
            }
        }
        return null;
    }

    @Nullable Token eatOperator(@NotNull TokenType operator) {
        Token op = tryEatOperator(operator);
        if (op == null) {
            throw Error.syntax(lexer.peek().loc, "期望 " + operator + " 实际是" + lexer.peek());
        }
        return op;
    }

    boolean isOperator(@NotNull Token tok, @NotNull TokenType type) {
        return tok.type == OPERATORS && type.name.equals(tok.lexeme);
    }

    // 决定 if/while/for/match 是否需要括号
    // 不用括号的原因, 比如 match(v)\n\n{} 会出现歧义, match v \n\n {} 则不会
    Node parenthesesExpr() {
        tryEatLines();
//        lexer.eat(LEFT_PAREN);
//        tryEatLines();
        Node expr = expr();
        tryEatLines();
//        lexer.eat(RIGHT_PAREN);
        return expr;
    }

    // Group ArrowFn
    Node leftParen(@NotNull Token lparen) {
        return lexer.any(
                () -> tuple(lparen),
                () -> funParser.arrowFn(lparen),
                () -> group(lparen)
        );
    }

    @Nullable Token peekArrow() {
        // object {a=1\nb=(1,)\nc=1} 不能多eat换行, record 解析需要依赖换行
        // () \n => 1     ()这里会识别成 tuple , 要提前判断
        Lexer.TokenNode marked = lexer.mark();
        try {
            bindParser.tryTypeDeclare();
            tryEatLines();
            return lexer.tryEat(ARROW);
        } finally {
            lexer.reset(marked);
        }
    }

    Node tuple(@NotNull Token lparen) {
        Token last2 = peekLastNonLine(-2);
        Token last3 = peekLastNonLine(-3);

        tryEatLines();
        List<Node> elems = new ArrayList<>();
        // () (a,) (a,b) (a,b,c)
        while(!lexer.peek().is(RIGHT_PAREN)) {
            tryEatLines();
            elems.add(expr());
            //noinspection DuplicatedCode
            tryEatLines();
            if (lexer.peek().is(RIGHT_PAREN)) {
                if (elems.size() == 1) {
                    // tuple 只有 1 个元素时候，为了避免语法与group (1+2)*3 冲突, 需要多加一个悬空逗号
                    // wtf  想了好几天怎么区分, 去了下 python 的语法 “Using a trailing comma for a singleton tuple: a, or (a,)”
                    // 想想也是，一般用不到没有元素的tuple
                    throw Error.syntax(lexer.peek().loc, "只有 1 个元素的 tuple 必须包含逗号");

                    // 更新：决定不支持 1 个元素的 tuple，第一，没实际用途，第二，容易写错，e.g. (a,b) 删除一个 (a)
                    // throw Error.syntax(range(lparen, lexer.peek()), "不支持 单个元素的 tuple ");
                }
                break;
            } else {
                lexer.eat(COMMA);
                tryEatLines();
            }
        }
        Token rp = lexer.eat(RIGHT_PAREN);

        // 类型声明遇到 tuple 和 fun 歧义, 类型声明后 优先识别 tuple
        // e.g. (): (Int,Str) => (42, "str")
        // 因为 fun 的声明是 (a: Int, b: Str) => xxx
        // [a: () => 1,]
        if (last2 != null && isOperator(last2, COLON)) {
            if (last3 != null && last3.type == RIGHT_PAREN) {
                return Ast.litTuple(range(lparen, rp), elems);
            }
        }

        Token arrow = peekArrow();
        if (arrow == null) {
            return Ast.litTuple(range(lparen, rp), elems);
        } else {
            throw Error.syntax(arrow.loc, "不能为" + ARROW);
        }
    }

    Node leftBracket(@NotNull Token leftBracket) {
        return lexer.any(
                () -> vectorParser.vector(leftBracket),
                () -> recordParser.litRecord(leftBracket)
        );
    }

    // ( expr )
    Node group(Token lparen) {
        //noinspection UnnecessaryLocalVariable
        Node group = group1(lparen);

//        Token arrow = peekArrow();
//        if (arrow == null) {
            return group;
//        } else {
//            throw Error.syntax(arrow.loc, "不能为" + ARROW);
//        }
    }

    Node group1(Token lparen) {
        tryEatLines();
        Node expr = expr();
        tryEatLines();
        //noinspection unused
        Token rp = lexer.eat(RIGHT_PAREN);
        // return Ast.group(range(lparen, rp), expr);
        return expr;
    }

    // assert   ->  assert expr [: msg]
    Assert assertStmt(@NotNull Token assert1) {
        Node expr = expr();
        if (expr.loc.rowEnd != -1 && assert1.loc.rowBegin != expr.loc.rowEnd) {
            throw Error.syntax(range(assert1.loc, expr.loc), "assert 语句不能换行");
        }
        Node msg = null;
        if (tryEatOperator(COLON) != null) {
            msg = expr();
        }
        return Ast.assertStmt(range(assert1.loc, expr.loc), expr, msg);
    }

    Debugger debuggerStmt(@NotNull Token debugger) {
        if (lexer.peek().is(NEWLINE)) {
            return Ast.debugger(debugger.loc, null);
        } else {
            return Ast.debugger(debugger.loc, expr());
        }
    }

    BlockParser blockParser = new BlockParser();
    BindParser bindParser = new BindParser();
    AssignParser assignParser = new AssignParser();
    // PatternParser patternParser = new PatternParser();
    PatternParser1 patternParser = new PatternParser1();
    MatchParser matchParser = new MatchParser();
    ParamsParser paramsParser = new ParamsParser();
    FunParser funParser = new FunParser();
    CallParser funCallParser = new CallParser();
    ControlFlowParser controlFlowParser = new ControlFlowParser();
    VectorParser vectorParser = new VectorParser();
    RecordParser recordParser = new RecordParser();
    ModuleParser moduleParser = new ModuleParser();

    class ModuleParser {

        public Module module(@NotNull Token module) {
            tryEatLines();
            Token name = lexer.tryEat(NAME);
            Id id = null;
            if (name != null) {
                id = id(name.loc, name.lexeme);
            }
            tryEatLines();
            Token lb = lexer.eat(LEFT_BRACE);
            Block block = blockParser.block(lb);
            return moduleStmt(range(module.loc, block.loc), id, block);
        }

        public Import import1(@NotNull Token import1) {
            tryEatLines();

            List<Pair<Id, Id>> aliasMap = new ArrayList<>();
            boolean hasStar = false;
            do {
                tryEatLines();
                Token tok = lexer.eat();
                if (tok.lexeme.equals(IMPORT_STAR)) {
                    if (hasStar) {
                        throw Error.syntax(tok.loc, "重复的 " + IMPORT_STAR + " 导入");
                    }
                    hasStar = true;
                    Id star = id(tok.loc, tok.lexeme);
                    aliasMap.add(new Pair<>(star, star));
                } else if (tok.is(NAME)) {
                    Id name = id(tok.loc, tok.lexeme);
                    Id alias = name;
                    tryEatLines();
                    Token as = lexer.tryEat(AS);
                    if (as != null) {
                        tryEatLines();
                        tok = lexer.eat(NAME);
                        alias = id(tok.loc, tok.lexeme);
                    }
                    aliasMap.add(new Pair<>(name, alias));
                } else {
                    throw Error.syntax(tok.loc, "期望 import * 或者 import name as alias 实际是 " + tok.lexeme);
                }
            } while (lexer.tryEat(COMMA) != null);

            if (aliasMap.isEmpty()) {
                throw Error.syntax(import1.loc, "import 不能为空");
            }

            tryEatLines();
            lexer.eat(FROM);
            tryEatLines();
            Node from = expr();

            return importStmt(range(import1.loc, from.loc), aliasMap, from);
        }
    }

    class BlockParser {

        /**
         * 允许 Statement 的地方都可以使用 block
         * if / for / while / fun-body / method-body 都是 block
         */
        public Block block(@NotNull Token lb) {
            return grammar.scope(Parser.this, () -> {
                tryEatLines();
                Token rb = lexer.tryEat(RIGHT_BRACE);
                if (rb == null) {
                    List<Node> stmts = new ArrayList<>();
                    do {
                        tryEatLines();
                        stmts.add(stmt());
                        // stmt 可以用 逗号分隔 写一行..
                        tryEatLines();
                        lexer.tryEat(COMMA);
                        tryEatLines();
                    } while (lexer.peek(RIGHT_BRACE) == null);
                    return Ast.block(range(lb, lexer.eat(RIGHT_BRACE)), stmts);
                } else {
                    return emptyBlock(range(lb, rb)); // { }
                }
            });
        }

        Block loopBlock(@NotNull Token lb) {
            try {
                controlFlowParser.inLoop++;
                return block(lb);
            } finally {
                controlFlowParser.inLoop--;
            }
        }

        @Nullable Block tryLoopBlock() {
            Token lb = lexer.tryEat(LEFT_BRACE);
            return lb == null ? null : loopBlock(lb);
        }
    }

    // destruct
    class BindParser {

        public Node type(Token type) {
            tryEatLines();
            if (lexer.tryEat(REC) != null) {
                return typeRecursive(type);
            } else {
                return type1(type);
            }
        }

        Typedef type1(Token type) {
            // 因为可以自定义操作符, 所以这里是可以是名字或者操作符
            Token tok = lexer.tryEat(NAME);
            if (tok == null) {
                tok = tryEatAnyOperator();
            }
            if (tok == null) {
                Token peek = lexer.peek();
                throw Error.syntax(peek.loc, "type 只能绑定 name 与 operators, 当前类型是 " + peek.type);
            }
            Id id = id(tok.loc, tok.lexeme);

            tryEatLines();
            lexer.eat(ASSIGN);
            tryEatLines();
            Node init = expr();
            return Ast.typedef(range(type.loc, init.loc), id, init);
        }

        Node typeRecursive(Token def) {
            tryEatLines();
            TupleLiteral lhs = patternParser.tuplePattern(false);
            for (Node el : lhs.elements) {
                if (!(el instanceof Id)) {
                    throw Error.syntax(el.loc, "typerec 只支持 let (a, b) = (,) 形式");
                }
            }
            tryEatLines();
            lexer.eat(ASSIGN);
            Node val = expr();
            if (!(val instanceof TupleLiteral)) {
                throw Error.syntax(val.loc, "typerec 只支持 let (a, b) = (,) 形式");
            }
            TupleLiteral rhs = (TupleLiteral) val;
            if (lhs.elements.size() != rhs.elements.size()) {
                throw Error.syntax(range(lhs.loc, rhs.loc), "数量不匹配");
            }
            return Ast.typedefRecursive(range(def.loc, val.loc), lhs, val);
        }

        public Node defineRecursive(Token def, boolean mut) {
            tryEatLines();
            TupleLiteral lhs = patternParser.tuplePattern(false);
            for (Node el : lhs.elements) {
                if (!(el instanceof Id)) {
                    throw Error.syntax(el.loc, "letrec 只支持 let (a, b) = (,) 形式");
                }
            }
            tryEatLines();
            lexer.eat(ASSIGN);
            Node val = expr();
            if (!(val instanceof TupleLiteral)) {
                throw Error.syntax(val.loc, "letrec 只支持 let (a, b) = (,) 形式");
            }
            TupleLiteral rhs = (TupleLiteral) val;
            if (lhs.elements.size() != rhs.elements.size()) {
                throw Error.syntax(range(lhs.loc, rhs.loc), "数量不匹配");
            }
            return Ast.defineRecursive(range(def.loc, val.loc), lhs, val, mut);
        }

        public Node define(Token def) {
            tryEatLines();
            boolean rec = lexer.tryEat(REC) != null;
            tryEatLines();
            boolean mut = lexer.tryEat(MUT) != null;
            tryEatLines();

            if (rec) {
                return defineRecursive(def, mut);
            } else {
                if (patternParser.isDestructPattern()) {
                    return definePattern(def, mut);
                } else {
                    return defineId(def, mut);
                }
            }
        }

        Node defineId(Token defTok, boolean mut) {
            return defineId(defTok.loc, mut);
        }

        // let a :Int = 1,   ++ = (l,r) => { ... }
        // 必须声明同时赋值, 实际上是 let-binding, 把值绑定到名字, 而不是声明&赋值
        Define defineId(Location loc, boolean mut) {
            // 因为可以自定义操作符, 所以这里是可以是名字或者操作符
            Token tok = lexer.tryEat(NAME);
            if (tok == null) {
                tok = tryEatAnyOperator();
            }
            if (tok == null) {
                Token peek = lexer.peek();
                throw Error.syntax(peek.loc, "let 只能绑定 name 与 operators, 当前类型是 " + peek.type);
            }
            return typeDefault(loc, tok, true, mut);
        }

        // 好多地方可以复用 这个结构
        // name_type_expr = name [:type_expr] [= default_expr]
        public Define optionalTypeDefault(Location loc, Token name) {
            return typeDefault(loc, name, false, false);
        }

        Define typeDefault(Location loc, Token name, boolean assignRequired, boolean mut) {
            Id id = id(name.loc, name.lexeme);
            tryEatLines();
            Location loc1 = lexer.peek().loc;
            Node type = tryTypeDeclare();
            if (type != null) {
                loc1 = type.loc;
            }
            // default
            if (lexer.tryEat(ASSIGN) == null) {
                if (assignRequired) {
                    throw Error.syntax(loc1, "缺失赋值或者默认值");
                } else {
                    Location range = range(loc, type == null ? id.loc : type.loc);
                    return Ast.define(range, id, type);
                }
            } else {
                tryEatLines();
                Node init = expr();
                return Ast.define(range(loc, init.loc), id, type, init, mut);
            }
        }

        @Nullable Node tryTypeDeclare() {
            if (tryEatOperator(COLON) == null) {
                return null;
            }
            // , ) = {
            return typeExpr();
        }

        Node definePattern(Token defTok, boolean mut) {
            return definePattern(defTok.loc, mut);
        }

        Define definePattern(Location loc, boolean mut) {
            Node pattern = patternParser.bindPattern();
            tryEatLines();
            lexer.eat(ASSIGN);
            Node init = expr();
            // 如果不解糖，就得靠 scope 的 define pattern 处理了
            // List<Node> return DefineAssignDesugar.desugarDefine(def.loc, assign.loc, mut, pattern, init);
            return Ast.define(range(loc, init.loc), pattern, init, mut);
        }
    }

    class AssignParser {

        // a.x / a.m() / a.f = 1
        public Node dot(@NotNull Node obj, @NotNull Token dot) {
            Token name = lexer.eat();
            // record 字段是否能为关键词!!!
            if (!name.is(NAME) /*&& name.type.type != TokenCategory.KEYWORD*/) {
                throw Error.syntax(lexer.peek().loc, "期望 name, 实际是 " + name);
            }
            Location loc = range(obj.loc, name.loc);
            Node expr = attribute(loc, dot.loc, obj, id(name.loc, name.lexeme));
            Token lp, assign;
            if ((lp = lexer.tryEat(LEFT_PAREN)) != null) {
                return funCallParser.callLeftParen(expr, lp);
            } else if ((assign = lexer.tryEat(ASSIGN)) != null) {
                return assign1(expr, assign);
            } else {
                return expr;
            }
        }

        public Node subscript(@NotNull Node lhs, @NotNull Token lbrace) {
            Token rb = lexer.tryEat(RIGHT_BRACKET);
            if (rb != null) {
                return Ast.vectorOf(range(lhs.loc, rb.loc), lbrace.loc, lhs);
            }

            tryEatLines();
            Node idxKey = expr();
            tryEatLines();
            rb = lexer.eat(RIGHT_BRACKET);
            Subscript subscript = Ast.subscript(range(lhs.loc, rb.loc), lbrace.loc, lhs, idxKey);
            tryEatLines();
            Token assign = lexer.tryEat(ASSIGN);
            if (assign == null) {
                return subscript;
            } else {
                return assign1(subscript, assign);
            }
        }

        public Node assign(@NotNull Node lhs, @NotNull Token tok) {
            lhs = patternParser.nodeToPattern(lhs, false);
            return assign1(lhs, tok);
        }

        Node assign1(@NotNull Node lhs, @NotNull Token tok) {
            tryEatLines();
            Node rhs = expr(tok.type.precedence - 1);
            return Ast.assign(range(lhs.loc, rhs.loc), tok.loc, lhs, rhs);
        }
    }

    // destruct & bind
    class PatternParser {

        boolean isTuplePattern() { return lexer.peek(LEFT_PAREN) != null; }

        boolean isVectorPattern() { return lexer.peek(LEFT_BRACKET) != null; }

        boolean isRecordPattern() { return lexer.peek(LEFT_BRACE) != null; }

        boolean isDestructPattern() { return isTuplePattern() || isVectorPattern() || isRecordPattern(); }

        public Node matchPattern() {
            // todo true 和 false 会遇到问题, 是 Name 不是 literal
            // todo 多支持 expr 和 literal
            return pattern(true);
        }

        public Node bindPattern() {
            return pattern(false);
        }

        Node pattern(boolean match) {
            if (isTuplePattern()) {
                return tuplePattern(match);
            } else if (isVectorPattern()) {
                return vectorPattern(match);
            } else if (isRecordPattern()) {
                return recordPattern(match);
            } else {
                Node expr = expr();
                if (match) {
                    if (expr instanceof Id) {
                        tryEatLines();
                        Token colon = tryEatOperator(COLON);
                        if (colon != null) {
                            Node type = expr();
                            return define(range(expr.loc, type.loc), (Id) expr, type);
                        } else {
                            return expr;
                        }
                    } else {
                        // int float str otherExpr
                        return expr;
                    }
                } else {
                    // bind
                    if (expr instanceof Id) {
                        return expr;
                    } else if (expr instanceof Assign) {
                        return expr;
                    } else if (expr instanceof Subscript) {
                        return expr;
                    } else if (expr instanceof Attribute) {
                        return expr;
                    } else {
                        throw Error.syntax(expr.loc, "不支持的 pattern: " + expr);
                    }
                }
            }
        }

        TupleLiteral tuplePattern(boolean match) {
            Token leftParen = lexer.eat(LEFT_PAREN);
            List<Node> elems = new ArrayList<>();

            // () (a,) (a,b) (a,b,c)
            while(!lexer.peek().is(RIGHT_PAREN)) {
                tryEatLines();
                Token comma = lexer.tryEat(COMMA);
                if (comma == null) {
                    elems.add(pattern(match));
                    tryEatLines();
                    if (lexer.peek().is(RIGHT_PAREN)) {
                        if (elems.size() == 1) {
                            // tuple 只有 1 个元素时候，为了避免语法与 group 冲突, 需要多加一个逗号
                            throw Error.syntax(lexer.peek().loc, "只有 1 个元素的 tuple 必须包含逗号");
                        }
                        break;
                    } else {
                        lexer.eat(COMMA);
                        tryEatLines();
                    }
                } else {
                    elems.add(wildcards(comma.loc));
                }
            }
            Token rp = lexer.eat(RIGHT_PAREN);
            return Ast.litTuple(range(leftParen, rp), elems);
        }

        VectorLiteral vectorPattern(boolean match) {
            Token leftBracket = lexer.eat(LEFT_BRACKET);
            List<Node> elems = new ArrayList<>();

            // E?(,(E?))*
            tryEatLines();
            while(!lexer.peek().is(RIGHT_BRACKET)) {
                tryEatLines();
                Token comma = lexer.tryEat(COMMA);
                if (comma == null) {
                    elems.add(pattern(match));
                    tryEatLines();
                    if (lexer.peek().is(RIGHT_BRACKET)) {
                        break;
                    } else {
                        lexer.eat(COMMA);
                        tryEatLines();
                    }
                } else {
                    elems.add(wildcards(comma.loc));
                }
            }

            tryEatLines();
            Token rb = lexer.eat(RIGHT_BRACKET);
            return litVector(range(leftBracket, rb), elems);
        }

        // recPattern :: { name, tuple[1], vec[1], rec.x, name = pattern }
        RecordLiteral recordPattern(boolean match) {
            Scope props = new Scope();
            Token lp = lexer.eat(LEFT_BRACE);
            tryEatLines();
            while (lexer.peek(RIGHT_BRACE) == null) {
                Node ptn = pattern(match);
                extractRecPattern(ptn, props, match);
                tryEatLines();
                lexer.tryEat(COMMA);
                tryEatLines();
            }
            Token rp = lexer.eat(RIGHT_BRACE);
            return Ast.litRecord(range(lp, rp), props);
        }

        Node nodeToPattern(Node n, boolean match) {
            if (n instanceof TupleLiteral) {
                List<Node> elems = new ArrayList<>();
                for (Node elem : ((TupleLiteral) n).elements) {
                    elems.add(nodeToPattern(elem, match));
                }
                return litTuple(n.loc, elems);
            } else if (n instanceof VectorLiteral) {
                List<Node> elems = new ArrayList<>();
                for (Node elem : ((VectorLiteral) n).elements) {
                    elems.add(nodeToPattern(elem, match));
                }
                return litVector(n.loc, elems);
            } else if (n instanceof RecordLiteral) {
                // 这里应该走不到, assign 的情况都会走到 Block 分支
                RecordLiteral litRec = (RecordLiteral) n;
                Scope props = new Scope();
                for (String name : litRec.map.keySet()) {
                    Node node = nodeToPattern(litRec.node(name), match);
                    props.put(name, KEY_VAL, node);
                }
                return litRecord(n.loc, props);
            } else if (n instanceof Block) {
                return blockToRecPattern(((Block) n), match);
            } else {
                if (match) {
                    return n;
                } else {
                    // todo assign ???
                    if (n instanceof Id) {
                        return n;
                    } else if (n instanceof Subscript) {
                        return n;
                    } else if (n instanceof Attribute) {
                        return n;
                    } else {
                        // todo 更人性化的错误信息
                        throw Error.syntax(n.loc, "错误的 pattern");
                    }
                }
            }
        }

        Node blockToRecPattern(Block block, boolean match) {
            Scope props = new Scope();
            for (Node stmt : block.stmts) {
                extractRecPattern(stmt, props, match);
            }
            return litRecord(block.loc, props);
        }

        // literal record pattern 只支持 id 与 assign
        void extractRecPattern(Node arg, Scope props, boolean match) {
            if (arg instanceof Id) {
                Id id = (Id) arg;
                props.put(id.name, KEY_VAL, id);
            } else if (arg instanceof Assign) {
                Assign assign = (Assign) arg;
                if (assign.pattern instanceof Id) {
                    props.put(((Id) assign.pattern).name, KEY_VAL, nodeToPattern(assign.value, match));
                } else {
                    // todo 更人性化的错误信息
                    throw Error.syntax(assign.pattern.loc, "错误的 pattern");
                }
            } else {
                // todo 更人性化的错误信息
                throw Error.syntax(arg.loc, "错误的 pattern");
            }
        }
    }

    class PatternParser1 {

        boolean isTuplePattern() {
            return lexer.peek(LEFT_PAREN) != null;
        }

        boolean isVectorOrRecordPattern() {
            return lexer.peek(LEFT_BRACKET) != null;
        }

        boolean isDestructPattern() {
            return isTuplePattern() || isVectorOrRecordPattern();
        }

        public Node matchPattern() {
            // todo true 和 false 会遇到问题, 是 Name 不是 literal
            // todo 多支持 expr 和 literal
            return pattern(true, true);
        }

        // todo 处理下 bind pattern 不支持类型 match !!!
        // todo 处理下 bind pattern 不支持类型 match !!!
        // todo 处理下 bind pattern 不支持类型 match !!!
        public Node bindPattern() {
            return pattern(false, false);
        }

        // 只有
        // case id: Type ->
        // case (a: Int, b: Str) ->
        // 支持 type 匹配, 支持 tuple 是为了做函数参数匹配, 做函数重载用
        Node pattern(boolean match, boolean supportedType) {
            if (isTuplePattern()) {
                return tuplePattern(match);
            } else if (isVectorOrRecordPattern()) {
                return lexer.any(
                        () -> vectorPattern(match),
                        () -> recordPattern(match)
                );
            } else {
                Node expr = expr();
                if (match) {
                    // 处理 type match
                    if (expr instanceof Id && supportedType) {
                        tryEatLines();
                        Token colon = tryEatOperator(COLON);
                        if (colon != null) {
                            Node type = expr();
                            return define(range(expr.loc, type.loc), (Id) expr, type);
                        } else {
                            return expr;
                        }
                    } else {
                        // int float str otherExpr
                        return expr;
                    }
                } else {
                    // bind
                    if (expr instanceof Id) {
                        return expr;
                    } else if (expr instanceof Assign) {
                        return expr;
                    } else if (expr instanceof Subscript) {
                        return expr;
                    } else if (expr instanceof Attribute) {
                        return expr;
                    } else {
                        throw Error.syntax(expr.loc, "不支持的 pattern: " + expr);
                    }
                }
            }
        }

        TupleLiteral tuplePattern(boolean match) {
            Token leftParen = lexer.eat(LEFT_PAREN);
            List<Node> elems = new ArrayList<>();

            // () (a,) (a,b) (a,b,c)
            while(!lexer.peek().is(RIGHT_PAREN)) {
                tryEatLines();
                Token comma = lexer.tryEat(COMMA);
                if (comma == null) {
                    elems.add(pattern(match, true));
                    tryEatLines();
                    if (lexer.peek().is(RIGHT_PAREN)) {
                        if (elems.size() == 1) {
                            // tuple 只有 1 个元素时候，为了避免语法与 group 冲突, 需要多加一个逗号
                            throw Error.syntax(lexer.peek().loc, "只有 1 个元素的 tuple 必须包含逗号");
                        }
                        break;
                    } else {
                        lexer.eat(COMMA);
                        tryEatLines();
                    }
                } else {
                    elems.add(wildcards(comma.loc));
                }
            }
            Token rp = lexer.eat(RIGHT_PAREN);
            return Ast.litTuple(range(leftParen, rp), elems);
        }

        VectorLiteral vectorPattern(boolean match) {
            Token lb = lexer.eat(LEFT_BRACKET);
            List<Node> elems = new ArrayList<>();

            // E?(,(E?))*
            tryEatLines();
            while(!lexer.peek().is(RIGHT_BRACKET)) {
                tryEatLines();
                // 允许连续多个 comma 跳过元素
                Token comma = lexer.tryEat(COMMA);
                if (comma == null) {
                    elems.add(pattern(match, false));
                    tryEatLines();
                    if (lexer.peek().is(RIGHT_BRACKET)) {
                        break;
                    } else {
                        lexer.eat(COMMA);
                        tryEatLines();
                    }
                } else {
                    elems.add(wildcards(comma.loc));
                }
            }

            tryEatLines();
            Token rb = lexer.eat(RIGHT_BRACKET);
            return litVector(range(lb, rb), elems);
        }

        // recPattern :: [name: pattern, name: tuple[1], name: vec[1], name: rec.x]
        // todo 这个语法有歧义, 如果定义了 : 操作符,
        // [a:1, b:2] 可以看成 a op 1, b op 2 结果的 vector
        // 目前这个功能失效, 只能先算好值再放字面量
        RecordLiteral recordPattern(boolean match) {
            Token lb = lexer.eat(LEFT_BRACKET);
            Scope props = new Scope();
            tryEatLines();

            Token colon = tryEatOperator(COLON);
            if (colon == null) {
                do {
                    tryEatLines();
                    if (lexer.peek(RIGHT_BRACKET) != null) {
                        break;
                    }

                    Token key = lexer.eat(NAME);
                    tryEatLines();
                    eatOperator(COLON);
                    tryEatLines();
                    Node ptn = pattern(match, false);
                    props.put(key.lexeme, KEY_VAL, nodeToPattern(ptn, match));
                } while (lexer.tryEatAny(COMMA, NEWLINE) != null);
            }

            tryEatLines();
            Token rb = lexer.eat(RIGHT_BRACKET);
            return Ast.litRecord(range(lb, rb), props);
        }

        Node nodeToPattern(Node n, boolean match) {
            if (n instanceof TupleLiteral) {
                List<Node> elems = new ArrayList<>();
                for (Node elem : ((TupleLiteral) n).elements) {
                    elems.add(nodeToPattern(elem, match));
                }
                return Ast.litTuple(n.loc, elems);
            } else if (n instanceof VectorLiteral) {
                List<Node> elems = new ArrayList<>();
                for (Node elem : ((VectorLiteral) n).elements) {
                    elems.add(nodeToPattern(elem, match));
                }
                return Ast.litVector(n.loc, elems);
            } else if (n instanceof RecordLiteral) {
                RecordLiteral litRec = (RecordLiteral) n;
                Scope props = new Scope();
                for (String name : litRec.map.keySet()) {
                    Node node = nodeToPattern(litRec.node(name), match);
                    props.put(name, KEY_VAL, node);
                }
                return Ast.litRecord(n.loc, props);
            } else if (match) {
                return n;
            } else {
                // todo assign ???
                if (n instanceof Id) {
                    return n;
                } else if (n instanceof Subscript) {
                    return n;
                } else if (n instanceof Attribute) {
                    return n;
                } else {
                    // todo 更人性化的错误信息
                    throw Error.syntax(n.loc, "错误的 pattern");
                }
            }
        }
    }

    class MatchParser {

        public Node matchStmt(@NotNull Token match) {
            Node val = parenthesesExpr();
            tryEatLines();
            return matchCaseBlock(match, val);
        }

        MatchPattern matchCaseBlock(@NotNull Token match, Node val) {
            List<CaseMatch> cases = new ArrayList<>();

            lexer.eat(LEFT_BRACE);
            tryEatLines();

            Token rb;
            // 允许空 match, 运行时再报错
            while ((rb = lexer.tryEat(RIGHT_BRACE)) == null) {
                Token matchCase = lexer.eat(CASE);
                tryEatLines();
                Node pattern = patternParser.matchPattern();
                tryEatLines();
                Token if1 = lexer.tryEat(IF);
                Node guard = null;
                if (if1 != null) {
                    guard = parenthesesExpr();
                }
                lexer.eat(ARROW_BLOCK);
                Block body = funParser.arrowBody();
                cases.add(caseMatch(range(matchCase.loc, body.loc), pattern, guard, body));
                tryEatLines();
            }
            return match(range(match.loc, rb.loc), val, cases);
        }
    }

    class ParamsParser {

        Parameters params(Token lp) {
            tryEatLines();
            Token rp = lexer.tryEat(RIGHT_PAREN);
            if (rp == null) {
                List<Id> names = new ArrayList<>();
                Scope declareProps = new Scope();
                params(names, declareProps);
                tryEatLines();
                rp = lexer.eat(RIGHT_PAREN);
                Declare declare = declare(range(lp, rp), declareProps);
                return Ast.params(range(lp, rp), names, declare);
            } else {
                Declare emptyDeclare = declare(range(lp, rp), new Scope());
                return Ast.params(range(lp, rp), new ArrayList<>(), emptyDeclare);
            }
        }

        void param(List<Id> params, Scope declare) {
            Token mut = lexer.tryEat(MUT);
            tryEatLines();
            // name 不能为关键词, 只能是标识符
            Token tok = lexer.eat(NAME);
            Lexer.TokenNode marked = lexer.mark();
            tryEatLines();
            Location loc = mut == null ? tok.loc : mut.loc;
            Define define = bindParser.optionalTypeDefault(loc, tok);
            // 处理 record {a \n b\n }
            if (define.type == null && define.value == null) {
                lexer.reset(marked);
            }

            Id name = (Id) define.pattern;
            params.add(name);
            // 保证有 param 一定有 declare
            if (mut == null) {
                declare.put(name.name, KEY_MUT, id(None, ID_FALSE));
            } else {
                declare.put(name.name, KEY_MUT, id(mut.loc, ID_TRUE));
            }
            if (define.type != null) {
                declare.put(name.name, KEY_TYPE, define.type);
            }
            if (define.value != null) {
                declare.put(name.name, KEY_DEFAULT, define.value);
            }
        }

        void params(List<Id> params, Scope declare) {
            do {
                tryEatLines();
                param(params, declare);
            } while (lexer.tryEat(COMMA) != null);
        }

        void attributes(List<Id> params, Scope declare) {
            do {
                if (lexer.peek(RIGHT_BRACE) != null) {
                    break;
                }
                tryEatLines();
                param(params, declare);
            } while (lexer.tryEatAny(COMMA, NEWLINE) != null);
        }

        void pair(LinkedHashMap<String, Node> map) {
            // name 不能为关键词, 只能是标识符
            Token name = lexer.eat(NAME);
            tryEatLines();
            lexer.tryEat(ASSIGN);
            tryEatLines();
            Node val = expr();
            map.put(name.lexeme, val);
        }

        void pairs(LinkedHashMap<String, Node> map) {
            do {
                if (lexer.peek(RIGHT_BRACE) != null) {
                    break;
                }
                tryEatLines();
                pair(map);
            } while (lexer.tryEatAny(COMMA, NEWLINE) != null);
        }
    }

    class FunParser {
        int inFun = 0;

        void enterFun() {
            inFun++;
        }

        void exitFun() {
            inFun--;
        }


        // ( a [:type][,...] ) [:type] => body
        public FunDef arrowFn(Token lparen) {
            FunDef f;
            try {
                enterFun();
                tryEatLines();
                Parameters params = paramsParser.params(lparen);
                tryEatLines();

                Node retType = bindParser.tryTypeDeclare();
                if (retType != null) {
                    params.declare.props.put(ID_RETURN, KEY_TYPE, retType);
                    tryEatLines();
                }

                lexer.eat(ARROW);
                Block body = arrowBody();
                Location loc = range(lparen.loc, body.loc);
                f = funDef(loc, null, params, body, true);
            } finally {
                exitFun();
            }
            return f;
        }

        Block arrowBody() {
            tryEatLines();
            Token lb = lexer.tryEat(LEFT_BRACE);
            if (lb == null) {
                return exprBlock();
            } else {
                return blockParser.block(lb);
            }
        }

        // todo remove
        // 单参数无类型默认值, 有括号或者无括号
        // a =>  这里只处理这一种情况, !!! 且不支持参数类型和返回值类型声明
        // ( a [:type][,...] ) [:type] => body 通过 prefix ( 来处理
        public FunDef singleParamArrowFn(@NotNull Node id, @NotNull Token arrow) {
            FunDef f;
            try {
                enterFun();

                List<Id> params = new ArrayList<>();
                Scope declareProps = new Scope();

                if (id instanceof Id) {
                    Id id1 = (Id) id;
                    params.add(id1);
                    // 为了保证所有 id 都有 declare, 且默认参数不可以修改
                    declareProps.put(id1.name, KEY_MUT, id(None, ID_FALSE));
                } else {
                    throw Error.syntax(id.loc, "不支持的箭头函数语法");
                }

                Block body = arrowBody();
                Location loc = range(id.loc, body.loc);
                Declare declare = declare(id.loc, declareProps);
                f = funDef(loc, null, params, declare, body, true);
            } finally {
                exitFun();
            }
            return f;
        }


        public Node defFun(@NotNull Token def) {
            try {
                enterFun();
                return defFun1(def);
            } finally {
                exitFun();
            }
        }

        FunDef defFun1(@NotNull Token def) {
            tryEatLines();
            Id name = funName();
            tryEatLines();
            Parameters params = paramsParser.params(lexer.eat(LEFT_PAREN));
            tryEatLines();
            Node retType = bindParser.tryTypeDeclare();
            if (retType != null) {
                params.declare.props.put(ID_RETURN, KEY_TYPE, retType);
                tryEatLines();
            }
            Block body = funBody(def);
            Location loc = range(def.loc, body.loc);
            return funDef(loc, name, params, body, false);
        }

        @Nullable Id funName() {
            Token tok;
            if (lexer.peek(0).is(LEFT_PAREN)) {
                return null;
            } else {
                tok = lexer.eat(NAME);
                return id(tok.loc, tok.lexeme);
            }
        }

        Block funBody(Token def) {
            Token assign = lexer.tryEat(ASSIGN);
            if (assign == null) {
                Token lb = lexer.tryEat(LEFT_BRACE);
                if (lb != null) {
                    return blockParser.block(lb);
                } else {
                    throw Error.syntax(def.loc, "方法/函数体必须为 fun $name() $block  或 fun $name() = $expr 形式");
                }
            } else {
                return exprBlock();
            }
        }


        Return returnStmt(@NotNull Token ret) {
            if (inFun > 0) {
                // return 表达式起始字符必须放在 return 后面, 后面可以换行
                if (lexer.peek().is(NEWLINE)) {
                    return Ast.returnStmt(ret.loc);
                } else {
                    Ast.Node expr = expr();
                    return Ast.returnStmt(range(ret.loc, expr.loc), expr);
                }
            } else {
                throw Error.syntax(ret.loc, "只能 return 函数");
            }
        }
    }

    class CallParser {

        public Call callLeftParen(@NotNull Node callee, @NotNull Token lparen) {
            tryEatLines();
            Arguments args = args(callee, lparen);
            // todo ... tryEat Assign 判断后面是不是赋值, 然后检查 pattern 是否合法..
            // 而不是放在运行时检查
            return call(range(callee.loc, lexer.peek(-1).loc), lparen.loc, callee, args);
        }

        Arguments args(Node callee, Token lparen) {
            List<Node> positional = new ArrayList<>();
            Map<String, Node> keyword = new HashMap<>();
            Token rp = lexer.tryEat(RIGHT_PAREN);
            if (rp == null) {
                List<Node> args = args(callee);
                for (Node arg : args) {
                    if (arg instanceof Assign) {
                        Assign kwArg = (Assign) arg;
                        if (!(kwArg.pattern instanceof Id)) {
                            throw Error.lexer(kwArg.loc, "keyword 调用语法错误");
                        }
                        Id name = (Id) kwArg.pattern;
                        positional.add(name);
                        keyword.put(name.name, kwArg.value);
                    } else {
                        positional.add(arg);
                    }
                }
                tryEatLines();
                lexer.eat(RIGHT_PAREN);
                return Ast.args(args, positional, keyword);
            } else {
                return Ast.args(new ArrayList<>(), positional, keyword);
            }
        }

        List<Node> args(Node callee) {
            List<Node> args = new ArrayList<>();
            boolean hasName = false;
            boolean hasKeyword = false;
            do {
                tryEatLines();
                Node arg = expr();
                if (arg instanceof Assign) {
                    hasKeyword = true;
                } else {
                    hasName = true;
                }
                args.add(arg);
            } while (lexer.tryEat(COMMA) != null);

            if (hasKeyword && hasName) {
                //todo rename err msg
                throw Error.syntax(rangeNodes(lexer.peek().loc, args), "不能混合两种调用方式");
            }
            return args;
        }
    }

    class VectorParser {

        public Node vector(@NotNull Token leftBracket) {
            List<Node> elems = new ArrayList<>();
            do {
                tryEatLines();
                if (lexer.peek(RIGHT_BRACKET) != null) {
                    break;
                }
                elems.add(expr());
            } while (lexer.tryEat(COMMA) != null);
            tryEatLines();

            Token rb = lexer.eat(RIGHT_BRACKET);
            return litVector(range(leftBracket, rb), elems);
        }

    }

    class RecordParser {

        public Node record(@NotNull Token record) {
            Scope declareProps = new Scope();
            List<Id> parents = new ArrayList<>();

            tryEatLines();
            Token tok = lexer.tryEat(NAME);
            Id name = tok == null ? null : id(tok.loc, tok.lexeme);
            tryEatLines();
            Token lb = lexer.eat(LEFT_BRACE);
            tryEatLines();
            Token rb = lexer.tryEat(RIGHT_BRACE);
            if (rb == null) {
                List<Id> names = new ArrayList<>();
                paramsParser.attributes(names, declareProps);
                tryEatLines();
                rb = lexer.eat(RIGHT_BRACE);
                tryEatLines();
            }
            if (lexer.tryEat(EXTENDS) != null) {
                tryEatLines();
                do {
                    Token parent = lexer.eat(NAME);
                    parents.add(id(parent.loc, parent.lexeme));
                    tryEatLines();
                } while (lexer.tryEat(COMMA) != null);
            }
            Declare declare = declare(range(lb, rb), declareProps);
            return Ast.recordDef(range(record, rb), name, parents, declare);
        }

        public Node litRecord(@NotNull Token leftBracket) {
            Scope map = new Scope();
            tryEatLines();

            // todo 这个语法有歧义, 如果定义了 : 操作符,
            // [a:1, b:2] 可以看成 a op 1, b op 2 结果的 vector
            // 目前这个功能失效, 只能先算好值再放字面量

            // let emptyLitRec = [:]
            // ket litRec = [a:1, mut b:2]
            Token colon = tryEatOperator(COLON);
            if (colon == null) {
                do {
                    tryEatLines();
                    if (lexer.peek(RIGHT_BRACKET) != null) {
                        break;
                    }

                    Token mutTok = lexer.tryEat(MUT);
                    boolean mut = mutTok != null;
                    tryEatLines();
                    Token key = lexer.eat(NAME);
                    tryEatLines();
                    eatOperator(COLON);
                    tryEatLines();
                    Node val = expr();
                    map.put(key.lexeme, KEY_VAL, val);
                    map.put(key.lexeme, KEY_MUT, mut);
                } while (lexer.tryEatAny(COMMA, NEWLINE) != null);
            }
            tryEatLines();
            Token rb = lexer.eat(RIGHT_BRACKET);
            return Ast.litRecord(range(leftBracket, rb), map);
        }
    }

    class ControlFlowParser {
        int inLoop  = 0;

        public Break breakStmt(@NotNull Token brk) {
            if (inLoop > 0) {
                return Ast.breakStmt(brk.loc);
            } else {
                throw Error.syntax(brk.loc, "只能 break 循环");
            }
        }

        public Continue continueStmt(@NotNull Token cont) {
            if (inLoop > 0) {
                return Ast.continueStmt(cont.loc);
            } else {
                throw Error.syntax(cont.loc, "只能 continue 循环");
            }
        }

        public Node ifStmt(@NotNull Token if1) {
            tryEatLines();

            // 特殊处理 if(test, then, else)  函数调用
            Call callIfFun = callIfFun(if1);
            if (callIfFun != null) {
                return callIfFun;
            }

            Node test = parenthesesExpr();
            tryEatLines();

            Token lb = lexer.tryEat(LEFT_BRACE);
            Block then;
            if (lb == null) {
                // todo throw Error.syntax(if1.loc, "if 的 {} 不能省略");
                then = exprBlock();
            } else {
                then = blockParser.block(lb);
            }

            tryEatLines();
            if (lexer.tryEat(ELSE)  == null) {
                return Ast.ifStmt(range(if1.loc, then.loc), test, then, ((Block) null));
            } else {
                tryEatLines();
                Token ifn = lexer.tryEat(IF);
                if (ifn == null) {
                    Block orElse;
                    if ((lb = lexer.tryEat(LEFT_BRACE)) == null) {
                        orElse = exprBlock();
                    } else {
                        orElse = blockParser.block(lb);
                    }
                    return Ast.ifStmt(range(if1.loc, orElse.loc), test, then, orElse);
                } else {
                    Node orElse = ifStmt(ifn);
                    if (orElse instanceof If) {
                        return Ast.ifStmt(range(if1.loc, orElse.loc), test, then, ((If) orElse));
                    } else {
                        throw Error.syntax(orElse.loc, "else 必须为 if test { } else [if test] { } 结构");
                    }
                }
            }
        }

        @Nullable
        Call callIfFun(@NotNull Token if1) {
            Lexer.TokenNode marked = lexer.mark();
            Token lp = lexer.tryEat(LEFT_PAREN);
            if (lp != null) {
                try {
                    // 这里 if 是 id
                    Call callIfFun = funCallParser.callLeftParen(id(if1.loc, if1.lexeme), lp);
                    if (callIfFun.args.positional.size() != 3) {
                        lexer.reset(marked);
                    } else {
                        return callIfFun;
                    }
                } catch (Error e) {
                    lexer.reset(marked);
                }
            }
            return null;
        }

        // for(i=1;i<10;i++) {}
        // for (v in obj) / for (k,v in obj)
        public Node forStmt(@NotNull Token for1) {
            return forIStmt(for1);

//            // 这一坨 peek 可能还不如直接换成先 mark 再抛异常回溯来得快...
//            int i = 0;
//            while (lexer.peek(i).is(NEWLINE)) i++;
//            if (!lexer.peek(i++).is(LEFT_PAREN)) {
//                throw Error.syntax(lexer.peek(i-1).loc, "for 的 {} 不能 省略");
//            }
//            int stack = 1;
//            while (unexpectedEOF(i)) {
//                Token t = lexer.peek(i++);
//                if (t.is(SEMICOLON)) {
//                    return forIStmt(for1);
//                }
//                if (t.is(IN)) {
//                    return forInStmt(for1);
//                }
//                if (t.is(LEFT_PAREN)) {
//                    stack++;
//                    continue;
//                }
//                // for(i=(1+1);i<10;i++) {}
//                if (t.is(RIGHT_PAREN)) {
//                    if (--stack <= 0) {
//                        return forInStmt(for1);
//                    }
//                }
//            }
//            throw new IllegalStateException();
        }

        Node forIStmt(@NotNull Token for1) {
            List<Node> stmts = lists();

            tryEatLines();
            lexer.eat(LEFT_PAREN);
            tryEatLines();
            if (!lexer.peek().is(SEMICOLON)) {
                Token def = lexer.tryEat(LET);
                Node init;
                if (def == null) {
                    init = expr();
                } else {
                    init = bindParser.define(def);
                }
                stmts.add(init);
                tryEatLines();
            }
            lexer.eat(SEMICOLON);
            tryEatLines();
            Node test;
            if (lexer.peek().is(SEMICOLON)) {
                test = id(lexer.peek().loc, ID_TRUE); // 省略 test
            } else {
                test = expr();
            }
            lexer.eat(SEMICOLON);
            tryEatLines();

            Node update = null;
            if (!lexer.peek().is(RIGHT_PAREN)) {
                update = expr();
                tryEatLines();
            }
            lexer.eat(RIGHT_PAREN);

            Block body = blockParser.tryLoopBlock();
            if (body == null) {
                throw Error.syntax(lexer.peek().loc, "for、while 的 {} 不能省略");
            }

            Location loc = range(for1.loc, body.loc);
            if (update != null) {
                body.stmts.add(update);
            }
            stmts.add(Ast.whileStmt(loc, test, body));
            return block(loc, stmts);
        }

//        Node forInStmt(@NotNull Token forIn) {
//            tryEatLines();
//            lexer.eat(LEFT_PAREN);
//            tryEatLines();
//            Token defTok = lexer.eat(LET);
//            boolean mut = TokenType.mutable(defTok.type);
//
//            Pattern pattern;
//            tryEatLines();
//            Token lb = lexer.tryEat(LEFT_BRACKET);
//            if (lb == null) {
//                Define def = lexer.defVarId(defTok, false);
//                pattern = def.id;
//            } else {
//                pattern = pattern(lb);
//                tryEatLines();
//                assert lexer.peek().is(IN);
//            }
//
//            tryEatLines();
//            lexer.eat(IN);
//            tryEatLines();
//            Node iterVal = expr(); // iter
//            Token rp = lexer.eat(RIGHT_PAREN);
//            Block body = lexer.tryLoopBlock();
//            if (body == null) {
//                throw Error.syntax(rp.loc, "for、while 的 {} 不能省略");
//            }
//
//            Location loc = range(forIn.loc, rp.loc);
//            IterFragment iter = Ast.iterator(loc, iterVal);
//
//            Id nextVar = anon(loc);
//            Define nextDef = Ast.define(loc, nextVar, iter.callNext, false, true);
//            List<Define> varDefs = DefineAssignDesugar.desugarDefine(loc, loc, mut, pattern, nextVar);
//
//            List<Expr> stmts = new ArrayList<>(1 + varDefs.size() + body.stmts.size());
//            stmts.add(nextDef);
//            stmts.addAll(varDefs);
//            stmts.addAll(body.stmts);
//
//            Block whileBody = Ast.block(body.loc, stmts, body.scope);
//            While while1 = Ast.whileStmt(range(loc, body.loc), iter.callHasNext, whileBody);
//            return Ast.block(range(iter.iterDef.loc, while1.loc), lists(iter.iterDef, while1), false);
//        }

        // while    ->   while ( test ) { body }
        public While whileStmt(@NotNull Token while1) {
            Node test = parenthesesExpr();
            tryEatLines();
            Block body = blockParser.tryLoopBlock();
            if (body == null) {
                // body = Ast.emptyBlock(rp.loc);
                throw Error.syntax(lexer.peek().loc, "for、while body {} 不能省略");
            }
            return Ast.whileStmt(range(while1.loc, body.loc), test, body);
        }
    }

}
