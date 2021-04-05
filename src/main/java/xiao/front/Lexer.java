package xiao.front;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiao.misc.Error;

import java.util.*;
import java.util.function.Supplier;

/**
 * Lexer
 * Regex Based Lexical Analyzer
 * tokenize :: stream<char> -> stream<tok>
 * @author chuxiaofeng
 */
@SuppressWarnings("unused")
public class Lexer implements Iterator<Token>, Iterable<Token> {

    // 通过 Node 把 Token 串成双向链表, 支持双向的 peek
    class TokenNode {
        @Nullable final TokenNode prev;

        @NotNull final Token current;

        @Nullable TokenNode next;

        TokenNode(@Nullable TokenNode prev, @NotNull Token current) {
            this.prev = prev;
            this.current = current;
        }

        public @NotNull Token token() {
            return current;
        }

        public @NotNull TokenNode next() {
            if (current.is(TokenType.EOF)) {
                throw new NoSuchElementException();
            }
            if (next == null) {
                Token nxtTok = lexicon.matchKeep(input, current);
                next = new TokenNode(this, nxtTok);
            }
            return next;
        }

        public @NotNull TokenNode prev() {
            if (prev == null) {
                throw new NoSuchElementException();
            }
            return prev;
        }
    }

    final TokenNode EOF = new TokenNode(null, Token.EOF);

    final @NotNull Lexicon lexicon;

    /*final */@NotNull String input;

    @NotNull TokenNode next; // next 会预先解析

    boolean hasNext;

    Lexer(@NotNull Lexicon lexicon, @NotNull String input) {
        this.lexicon = lexicon;
        this.input = input;
        this.next = EOF; // nonsense, make idea happy
        reset();
    }

    Lexer(@NotNull Lexicon lexicon) {
        this(lexicon, "");
    }

    // 加这个方法是为了处理操作符声明作用域, 里开作用域会清理 operatorRule 和 tokenType
    // 提前缓存的 token 可能存在用被清除的 rule 解析到的 tokenType
    // 如果存在特殊的语法需要提前 peek 很多 tok, 清理缓存会遇到性能问题的话, 到时候可以标记 tokenType 是否废弃
    // 遇到废弃的 tokenType 再废弃缓存, 或者用要清楚的 tokenType 去主动检查缓存
    void resetNext() {
        TokenNode prev = next.prev;
        if (prev == null) {
            reset();
        } else {
            Token tok = lexicon.matchKeep(input, prev.current);
            next = new TokenNode(prev, tok);
            hasNext = true;
        }
    }

    public Iterable<Token> tokenize(@NotNull String input) {
        this.input = input;
        reset();
        return this;
    }

    public @NotNull TokenNode mark() {
        return next;
    }

    public void reset(@NotNull TokenNode tok) {
        next = tok;
        hasNext = true;
    }

    public void reset() {
        Token fst = lexicon.matchKeep(input, null);
        next = new TokenNode(null, fst);
        hasNext = true;
    }

    public void skip(int n) {
        for (int i = 0; i < n; i++) {
            eat();
        }
    }

    public @NotNull Token eat() {
        try {
            return next();
        } catch (NoSuchElementException e) {
            throw Error.lexer(next.token().loc, "EOF");
        }
    }

    public @NotNull Token eat(@NotNull TokenType type) {
        if (peek(type) == null) {
            throw Error.lexer(peek().loc, String.format("期望 %s, 实际是%s", type, peek().type));
        } else {
            return eat();
        }
    }

    public @NotNull Token eatAny(TokenType ...types) {
        for (TokenType type : types) {
            Token tok = tryEat(type);
            if (tok != null) {
                return tok;
            }
        }
        throw Error.lexer(peek().loc, String.format("期望 %s 之一, 实际是%s", Arrays.toString(types), peek().type));
    }

    public List<Token> eatSeq(TokenType ...types) {
        List<Token> toks = new ArrayList<>(types.length);
        for (int i = 0; i < types.length; i++) {
            Token tok = peek(i);
            TokenType type = types[i];
            if (tok.is(type)) {
                toks.add(tok);
            } else {
                throw Error.lexer(peek().loc, String.format("期望 %s, 实际是%s", type, peek().type));
            }
        }
        return toks;
    }

    public @Nullable Token tryEat(TokenType type) {
        if (peek(type) == null) {
            return null;
        } else {
            return eat();
        }
    }

    public @Nullable Token tryEatAny(TokenType ...types) {
        for (TokenType type : types) {
            Token tok = tryEat(type);
            if (tok != null) {
                return tok;
            }
        }
        return null;
    }

    public @Nullable List<Token> tryEatSeq(TokenType ...types) {
        List<Token> toks = peekSeq(types);
        if (toks == null) {
            return null;
        } else {
            skip(types.length);
            return toks;
        }
    }

    public @NotNull Token peek() {
        return peek(0);
    }

    public @NotNull Token peek(int n) {
        try {
            TokenNode node = next;
            while (n != 0) {
                if (n < 0) {
                    node = node.prev();
                    n++;
                } else {
                    node = node.next();
                    n--;
                }
            }
            return node.token();
        } catch (NoSuchElementException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    public @Nullable Token peek(@NotNull TokenType type) {
        Token tok = peek();
        if (tok.is(type)) {
            return tok;
        } else {
            return null;
        }
    }

    public @Nullable Token peekAny(TokenType ...types) {
        for (TokenType type : types) {
            Token tok = peek(type);
            if (tok != null) {
                return tok;
            }
        }
        return null;
    }

    public @Nullable List<Token> peekSeq(TokenType ...types) {
        List<Token> toks = new ArrayList<>(types.length);
        for (int i = 0; i < types.length; i++) {
            Token tok = peek(i);
            if (tok.is(types[i])) {
                toks.add(tok);
            } else {
                return null;
            }
        }
        return toks;
    }

    public @Nullable Token tryPeek() {
        return tryPeek(0);
    }

    public @Nullable Token tryPeek(int n) {
        try {
            return peek(n);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public @Nullable Token tryPeek(@NotNull TokenType type) {
        try {
            return peek(type);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public @Nullable Token tryPeekAny(TokenType ...types) {
        try {
            return peekAny(types);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @SafeVarargs
    public final <T> T any(Supplier<T>... choices) {
        TokenNode marked = mark();
        List<Error> errors = new ArrayList<>();
        for (Supplier<T> choice : choices) {
            try {
                return choice.get();
            } catch (Error e) {
                errors.add(e);
                reset(marked);
            }
        }
        throw Error.mixed(errors);
    }

    @NotNull @Override
    public Iterator<Token> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public Token next() {
        if (!hasNext) {
            throw new NoSuchElementException();
        }

        if (next.token().is(TokenType.EOF)) {
            hasNext = false;
            return next.token();
        } else {
            TokenNode node = next;
            next = node.next();
            return node.token();
        }
    }
}
