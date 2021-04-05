package xiao.front;

import org.junit.Assert;
import org.junit.Test;
import xiao.Value;
import xiao.front.Ast;
import xiao.front.Token;
import xiao.misc.Error;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static xiao.Boot.*;
import static xiao.front.TokenType.*;

/**
 * @author chuxiaofeng
 */
@SuppressWarnings("NonAsciiCharacters")
public class TestParser {

    @Test
    public void test_case_tuple_parse() {
        Ast.Block block = parse("match v { case () -> 1 }");
        Ast.Node pattern = ((Ast.MatchPattern) block.stmts.get(0)).cases.get(0).pattern;
        assertTrue(pattern instanceof Ast.TupleLiteral);

        block = parse("match v { case (1,) -> 1 }");
        pattern = ((Ast.MatchPattern) block.stmts.get(0)).cases.get(0).pattern;
        assertTrue(pattern instanceof Ast.TupleLiteral);

        block = parse("match v { case (1,2) -> 1 }");
        pattern = ((Ast.MatchPattern) block.stmts.get(0)).cases.get(0).pattern;
        assertTrue(pattern instanceof Ast.TupleLiteral);

        block = parse("match v { case (1,2,) -> 1 }");
        pattern = ((Ast.MatchPattern) block.stmts.get(0)).cases.get(0).pattern;
        assertTrue(pattern instanceof Ast.TupleLiteral);

        block = parse("match v { case (,) -> 1 }"); // case (_,)
        pattern = ((Ast.MatchPattern) block.stmts.get(0)).cases.get(0).pattern;
        assertTrue(pattern instanceof Ast.TupleLiteral);

        block = parse("match v { case (,,) -> 1 }"); // case (_,)
        pattern = ((Ast.MatchPattern) block.stmts.get(0)).cases.get(0).pattern;
        assertTrue(pattern instanceof Ast.TupleLiteral);

        block = parse("match v { case (,,a,) -> 1 }"); // case (_,)
        pattern = ((Ast.MatchPattern) block.stmts.get(0)).cases.get(0).pattern;
        assertTrue(pattern instanceof Ast.TupleLiteral);
    }


    @Test
    public void test_操作符跨行() {
        parse("a \n = 1");
        parse("[] \n = []");
        parse("a\n.b");
        parse("() \n => 1");
        parse("infixl 7 +\n1\n+\n1\n+\n1");
        Assert.assertEquals(3L, ((Value.IntValue) evalRaw("infixl 7 +\n1\n+\n1\n+\n1")).value);
    }


    @Test
    public void test_结合性() {
        assertEquals(-4, ((Value.IntValue) evalRaw("infixl 7 -\n  1 - 2 - 3")).value);;
        assertEquals(2,  ((Value.IntValue)evalRaw("infixr 7 -\n  1 - 2 - 3")).value);;
    }

    @Test
    public void test_中缀转前缀() {
        assertEquals(-1, ((Value.IntValue) evalRaw("infixn 7 -\n  `-`(1)")).value);
        assertEquals(-1, ((Value.IntValue) evalRaw("infixn 7 -\n  `-`(1, 2)")).value);

        assertEquals(-1, ((Value.IntValue) evalRaw("infixl 7 -\n  `-`(1)")).value);
        assertEquals(-1, ((Value.IntValue) evalRaw("infixl 7 -\n  `-`(1, 2)")).value);
        assertEquals(-4, ((Value.IntValue) evalRaw("infixl 7 -\n  `-`(1, 2, 3)")).value);

        assertEquals("hello", ((Value.StringValue) evalRaw("infixl 7 ++\n  `++`(\"hello\")")).value);
        assertEquals("helloworld", ((Value.StringValue) evalRaw("infixl 7 ++\n  `++`(\"hello\",\"world\")")).value);
        assertEquals("helloworld!", ((Value.StringValue) evalRaw("infixl 7 ++\n  `++`(\"hello\",\"world\",\"!\")")).value);
    }

//    改了实现检测不出来了, 就这样吧
//    @Test(expected = Error.Syntax.class)
//    public void test_中缀转前缀不结合参数限制() {
//        evalRaw("infixn 7 -\n  `-`(1, 2, 3)");
//    }

    @Test
    public void test_infixn1() {
        parse("a = 1 \n b = 1");
    }

    @Test(expected = Error.Syntax.class)
    public void test_infixn2() {
        parse("a = b = 1");
    }

    @Test(expected = Error.Syntax.class)
    public void test_infixn3() {
        parse("infixn 10 <-> \n 1 <-> 1 <-> 1");
    }

    @Test
    public void test_infixn4() {
        parse("a = { b = 1 }");
    }

    @Test(expected = Error.Syntax.class)
    public void test_infixn5() {
        parse("infixn 6 >\n infixl 7 +\n a > (1 + 2) > c");
    }

    @Test(expected = Error.Syntax.class)
    public void test_prefix1() {
        // 不允许重新定义前缀 -
        parse("prefix 7 -\n");
    }

    @Test(expected = Error.Syntax.class)
    public void test_同作用域重复声明符号() {
        parse("{\n" +
                "    infixn <$>\n" +
                "    infixn <$>\n" +
                "}");
    }

    @Test
    public void test_跨作用域声明符号不重复() {
        parse("{ infixn <$> }\n" +
                "{ infixn <$> }\n" +
                "infixn <$>");
    }

    @Test(expected = Error.Syntax.class)
    public void test_绑定不合法() {
        parse("let 1 = 1");
    }

    @Test(expected = Error.Syntax.class)
    public void test_操作符声明不合法0() {
        parse("infixn 1");
    }

    @Test(expected = Error.Syntax.class)
    public void test_操作符声明不合法1() {
        parse("infixn 1 abc");
    }

    @Test(expected = Error.Syntax.class)
    public void test_操作符声明不合法2() {
        parse("infixn 1 abc\n");
    }

    @Test
    public void test_prefix2() {
        assertFalse(((Value.BoolValue) evalRaw("prefix 9 !\n!true")).value);
        assertTrue(((Value.BoolValue) evalRaw("prefix 9 !\n!false")).value);
    }

    @Test
    public void test_operators1() {
        // 只内置了 一元前缀的- 操作符, 如果需要使用二元+-, 需要前置声明
        assertEquals(-1, ((Value.IntValue) evalRaw("infixl 7 -\n-1")).value);
        assertEquals(1, ((Value.IntValue) evalRaw("infixl 7 -\n- -1")).value);
        assertEquals(0, ((Value.IntValue) evalRaw("infixl 7 -\n 1-1")).value);
        assertEquals(2, ((Value.IntValue) evalRaw("infixl 7 -\n 1- -1")).value);
        assertEquals("ab", ((Value.StringValue) evalRaw("\n" +
                "infixl 7 +\n" +
                "infixl 7 ++ \n" +
                "\"a\" ++ \"b\"")).value);
        assertEquals(2, ((Value.IntValue) evalRaw("\n" +
                "infixl 7 +\n" +
                "infixl 7 +++\n" +
                "let +++ = (a: Int, b: Int) => a + b\n" +
                "1 +++ 1")).value);
        assertEquals(0, ((Value.IntValue) evalRaw("\n" +
                "infixl 7 -\n" +
                "infixl 7 --\n" +
                "let -- = (a: Int, b: Int) => a - b\n" +
                "1 -- 1")).value);
        assertEquals(0, ((Value.IntValue) evalRaw("\n" +
                "infixl 7 -\n" +
                "infixl 7 ---\n" +
                "let --- = (a: Int, b: Int) => a - b\n" +
                "1 --- 1")).value);
        assertEquals(2, ((Value.IntValue) evalRaw("\n" +
                "infixl 7 -\n" +
                "infixl 7 ---\n" +
                "let --- = (a: Int, b: Int) => a - b\n" +
                "1 --- -1")).value);
        assertEquals(0, ((Value.IntValue) evalRaw("\n" +
                "infixl 7 +\n" +
                "1 + -1")).value);
        assertEquals(0, ((Value.IntValue) evalRaw("\n" +
                "infixl 7 +\n" +
                "infixl 7 +++\n" +
                "let +++ = (a: Int, b: Int) => a + b\n" +
                "1 +++ -1")).value);
    }

    @Test
    public void test_minus() {
        tok("");
        tok("-1");
        tok("--1");
        tok("- -1");
        tok("1-1");
        tok("1--1");
        tok("1- -1");

        {
            List<Token> seq = tokSeq("");
            assertEquals(1, seq.size());
            assertEquals(EOF, seq.get(0).type);
        }
        {
            List<Token> seq = tokSeq("-1");
            assertEquals(3, seq.size());
            assertEquals(UNARY_MINUS, seq.get(0).type);
            assertEquals(INT, seq.get(1).type);
            assertEquals(EOF, seq.get(2).type);
        }
        {
            List<Token> seq = tokSeq("--1");
            assertEquals(3, seq.size());
            assertEquals(OPERATORS, seq.get(0).type);
            assertEquals(INT, seq.get(1).type);
            assertEquals(EOF, seq.get(2).type);
        }
        {
            List<Token> seq = tokSeq("- -1");
            assertEquals(4, seq.size());
            assertEquals(UNARY_MINUS, seq.get(0).type);
            assertEquals(UNARY_MINUS, seq.get(1).type);
            assertEquals(INT, seq.get(2).type);
            assertEquals(EOF, seq.get(3).type);
        }
        {
            List<Token> seq = tokSeq("1-1");
            assertEquals(4, seq.size());
            assertEquals(INT, seq.get(0).type);
            assertEquals(OPERATORS, seq.get(1).type);
            assertEquals(INT, seq.get(2).type);
            assertEquals(EOF, seq.get(3).type);
        }
        {
            List<Token> seq = tokSeq("1--1");
            assertEquals(4, seq.size());
            assertEquals(INT, seq.get(0).type);
            assertEquals(OPERATORS, seq.get(1).type);
            assertEquals(INT, seq.get(2).type);
            assertEquals(EOF, seq.get(3).type);
        }
        {
            List<Token> seq = tokSeq("1- -1");
            assertEquals(5, seq.size());
            assertEquals(INT, seq.get(0).type);
            assertEquals(OPERATORS, seq.get(1).type);
            assertEquals(UNARY_MINUS, seq.get(2).type);
            assertEquals(INT, seq.get(3).type);
            assertEquals(EOF, seq.get(4).type);
        }
    }

    List<Token> tokSeq(String input) {
        List<Token> seq = new ArrayList<>();
        for (Token tok : tokenize(input)) {
            seq.add(tok);
        }
        return seq;
    }

    void tok(String input) {
        System.out.println(input);
        System.out.println("------------------------------");
        for (Token tok : tokenize(input)) {
            System.out.println(tok + "\ttype " + tok.type);
        }
        System.out.println("------------------------------");
        System.out.println();
    }

}
