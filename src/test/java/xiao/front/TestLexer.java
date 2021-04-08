package xiao.front;


import org.junit.Test;
import xiao.front.Lexer.TokenNode;
import xiao.misc.Error;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.*;
import static xiao.front.TokenType.*;

/**
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public class TestLexer {

    final static Pattern opPtn = Pattern.compile("(?:`[^`]*`)|(?:[\\Q:!#$%^&*+./<=>?@\\ˆ|-~\\E]+)");

    @Test
    public void test_resetNext() {
        Lexer lexer = lexer("1 2 3 4");
        TokenNode nxt = lexer.next;
        lexer.resetNext();
        TokenNode nxt2 = lexer.next;
        assertNotEquals(nxt, nxt2);

        lexer.peek(3);
        //noinspection ConstantConditions
        assertNotNull(lexer.next.next.next.next);
        lexer.resetNext();
        assertNull(lexer.next.next);
    }

    @Test
    public void test_operators() {
        String[] ops = {
                ".", "..",
                "ˆ", "ˆˆ",
                "^", "^^",
                "**",
                "*", "/", "`div`",
                "`mod`", "`rem`", "`quot`",
                "++",
                "==", "/=",  "<", "<=", ">", ">=",
                "`elem`", "`notElem`",
                "&&", "||",
                ">>", ">>=",
                "$", "$!","`seq`",
                "+", "-", "*", "/", ">>", ">>=", "!!",
                "..", ":", "::", "=", "\\", "|", "<-", "->", "@", "~", "=>",
        };

        for (String op : ops) {
            Matcher matcher = opPtn.matcher(op);
            if (!matcher.lookingAt()) {
                System.out.println(op);
            }
            assertTrue(matcher.lookingAt());
            assertEquals(op, matcher.group());
        }
    }

    @Test
    public void playground() {
        TreeSet<String> operatorsRules = new TreeSet<>((o1, o2) -> -o1.compareTo(o2));
        operatorsRules.add("<$>");
        operatorsRules.add(">");
        operatorsRules.add(">=");
        operatorsRules.add(">~");
        operatorsRules.add("<");
        for (String e : operatorsRules) {
            System.out.println(e);
        }


//        Parser1 parser = new Parser1(new Lexer(new Lexicon()), new GrammarRules());
//        Block block = parser.parse("infixl 170 <$>\n" +
//                "1 <$> 2");
        System.out.println();
    }

    @Test
    public void test_null() {
        Lexer lexer = lexer( "");
        assertTrue(lexer.peek().is(TokenType.EOF));
        assertTrue(lexer.eat().is(TokenType.EOF));

        Lexer lexer1 = lexer( " ");
        assertTrue(lexer1.peek().is(TokenType.EOF));
        assertTrue(lexer1.eat().is(TokenType.EOF));
    }

    @Test
    public void mark_reset() {
        String input = "1,2,3,4,5,6";
        Lexer lexer = lexer( input);
        assertEquals("1", lexer.next().lexeme);
        assertEquals(",", lexer.next().lexeme);
        assertEquals("2", lexer.next().lexeme);
        assertEquals(",", lexer.next().lexeme);
        TokenNode marked = lexer.mark();
        assertEquals("3", lexer.next().lexeme);
        assertEquals(",", lexer.next().lexeme);
        assertEquals("4", lexer.next().lexeme);
        assertEquals(",", lexer.next().lexeme);
        lexer.reset(marked);
        assertEquals("3", lexer.next().lexeme);
        assertEquals(",", lexer.next().lexeme);
        assertEquals("4", lexer.next().lexeme);
        assertEquals(",", lexer.next().lexeme);
    }

    @Test
    public void test_lexerRules() {
        Function<String, List<Token>> tokenizer = input -> {
            List<Token> r = new ArrayList<>();
            for (Token tok : lexer( input)) {
                r.add(tok);
            }
            return r;
        };

        // comment & space
        List<Token> toks = tokenizer.apply(" /*/**/  ");
        assertEquals(1, toks.size());
        assertSame(EOF, toks.get(0).type);
        toks = tokenizer.apply(" //...\na  ");
        assertEquals(3, toks.size());
        assertEquals(NEWLINE, toks.get(0).type);
        assertEquals(EOF, toks.get(2).type);

        // operator
        List<Token> lst = tokenizer.apply("1 + 2 <= 3");
        assertEquals("+", lst.get(1).lexeme);
        assertEquals("<=", lst.get(3).lexeme);

        // name
        assertEquals("abc_123", tokenizer.apply("abc_123 xyz").get(0).lexeme);

        // num
        // System.out.println(tokenizer.apply("-123.123E-123abc").get(0).lexeme); // todo !!!
        assertEquals("-", tokenizer.apply("-123.123E-123abc").get(0).lexeme);
        assertEquals("123.123E-123", tokenizer.apply("-123.123E-123abc").get(1).lexeme);
        assertEquals("-", tokenizer.apply("-0x123abc").get(0).lexeme);
        assertEquals("0x123abc", tokenizer.apply("-0x123abc").get(1).lexeme);

        // str
        assertEquals("\"he\\\"llo\"", tokenizer.apply("\"he\\\"llo\"abc").get(0).lexeme);
        assertEquals("'he\\'llo'", tokenizer.apply("'he\\'llo'abc").get(0).lexeme);
    }

    @Test
    public void test_peek() {
        Lexer lexer = lexer( "a b c");
        assertEquals("a", lexer.peek(0).lexeme);
        assertEquals("b", lexer.peek(1).lexeme);
        assertEquals("c", lexer.peek(2).lexeme);
        assertTrue(lexer.peek(3).is(EOF));

        try {
            lexer.peek(4);
            fail();
        } catch (IndexOutOfBoundsException e) {
            assertTrue(true);
        }

        assertEquals("a", lexer.eat().lexeme);
        assertEquals("b", lexer.eat().lexeme);
        assertEquals("c", lexer.eat().lexeme);
        assertTrue(lexer.eat().is(EOF));

        try {
            lexer.eat();
            fail();
        } catch (Error.Lexer e) {
            assertTrue(true);
        }

        assertTrue(lexer.peek(0).is(EOF));
        assertEquals("c", lexer.peek(-1).lexeme);
        assertEquals("b", lexer.peek(-2).lexeme);
        assertEquals("a", lexer.peek(-3).lexeme);

        try {
            lexer.peek(-4);
            fail();
        } catch (IndexOutOfBoundsException e) {
            assertTrue(true);
        }
    }

    @Test
    public void test_peek_eat() {
        Lexer lexer = lexer( "a = 1");

        assertEquals(requireNonNull(lexer.peek()).lexeme, "a");
        assertEquals(requireNonNull(lexer.eat()).lexeme, "a");

        assertEquals(requireNonNull(lexer.peek()).lexeme, "=");
        assertEquals(requireNonNull(lexer.eat()).lexeme, "=");

        assertEquals(requireNonNull(lexer.peek()).lexeme, "1");
        assertEquals(requireNonNull(lexer.eat()).lexeme, "1");

        assertTrue(lexer.peek().is(TokenType.EOF));
        assertTrue(lexer.eat().is(TokenType.EOF));

        assertEquals(lexer.peek(-1).lexeme, "1");
        assertEquals(lexer.peek(-2).lexeme, "=");
        assertEquals(lexer.peek(-3).lexeme, "a");
    }

    @Test
    public void test_any() {
        Lexer lexer = lexer( " i = 42");

        assertNull(lexer.peekAny(INT, EOF));
        assertNotEquals(lexer.peekAny(INT, EOF, NAME), null);
        assertEquals(requireNonNull(lexer.peekAny(INT, EOF, NAME)).type, NAME);

        assertNull(lexer.tryEatAny(INT, EOF));
        Token tok = lexer.tryEatAny(INT, EOF, NAME);
        assertNotEquals(tok, null);
        assert tok != null;
        assertEquals(tok.type, NAME);

        assertEquals(lexer.eatAny(ASSIGN, NAME).type, ASSIGN);
        assertEquals(lexer.eatAny(FLOAT, STRING, INT).type, INT);

        lexer.eat(EOF);
    }

    @Test
    public void test_seq() {
        Lexer lexer = lexer( " i = 42");

        assertNull(lexer.peekSeq(NAME, OPERATORS, EOF));

        List<Token> seq = lexer.peekSeq(NAME, ASSIGN, INT, EOF);
        assertNotNull(seq);
        assertArrayEquals(seq.stream().map(it -> it.type).toArray(), new TokenType[] {NAME, ASSIGN, INT, EOF});

        assertNull(lexer.tryEatSeq(NAME, ASSIGN, EOF));

        seq = lexer.tryEatSeq(NAME, ASSIGN, INT, EOF);
        assertNotNull(seq);
        assertArrayEquals(seq.stream().map(it -> it.type).toArray(), new TokenType[] {NAME, ASSIGN, INT, EOF});

        lexer.reset();

        assertArrayEquals(
                lexer.eatSeq(NAME, ASSIGN, INT, EOF).stream().map(it -> it.type).toArray(),
                new TokenType[] {NAME, ASSIGN, INT, EOF}
        );
    }

    @Test
    public void test_lexer() {
        Lexer lexer = lexer( "let x = 1 \n while(x > 2) { \n" +
                "def f(a,b) { a + 1} \n" +
                "break\n" +
                " }");

        Token tok = lexer.eat();
        assertEquals(tok.type, LET);
        assertEquals(tok.lexeme, "let");

        tok = lexer.eat();
        assertEquals(tok.type, NAME);
        assertEquals(tok.lexeme, "x");

        tok = lexer.eat();
        assertEquals(tok.type, ASSIGN);
        assertEquals(tok.lexeme, "=");

        tok = lexer.eat();
        assertEquals(tok.type, INT);
        assertEquals(tok.lexeme, "1");

        tok = lexer.peek(0);
        assertEquals(tok.type, NEWLINE);
        assertEquals(tok.lexeme, "\n");

        tok = lexer.peek(3);
        assertEquals(tok.type, NAME);
        assertEquals(tok.lexeme, "x");

        tok = lexer.peek(-1);
        assertEquals(tok.type, INT);
        assertEquals(tok.lexeme, "1");

        tok = lexer.peek(-2);
        assertEquals(tok.type, ASSIGN);
        assertEquals(tok.lexeme, "=");

        tok = lexer.eat();
        assertEquals(tok.type, NEWLINE);
        assertEquals(tok.lexeme, "\n");

        tok = lexer.eat();
        assertEquals(tok.type, WHILE);
        assertEquals(tok.lexeme, "while");

        tok = lexer.eat();
        assertEquals(tok.type, LEFT_PAREN);
        assertEquals(tok.lexeme, "(");

        tok = lexer.eat();
        assertEquals(tok.type, NAME);
        assertEquals(tok.lexeme, "x");

        tok = lexer.eat();
        assertEquals(tok.type, OPERATORS);
        assertEquals(tok.lexeme, ">");


        lexer.reset();

        for (Token it : lexer) {
            System.out.println(it.loc.inspect(it.toString()));
        }
    }

    static Lexer lexer(String input) {
        return new Lexer(new Lexicon(), input);
    }

}
