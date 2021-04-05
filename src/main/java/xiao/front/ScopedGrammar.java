package xiao.front;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static xiao.front.Ast.Node;
import static xiao.front.Parser.Led;
import static xiao.front.Parser.Nud;

/**
 * @author chuxiaofeng
 */
public class ScopedGrammar extends SimpleGrammar {

    static class Wrapper implements Grammar {
        @NotNull ScopedGrammar grammar;

        Wrapper() {
            this.grammar = new ScopedGrammar();
        }

        @Override
        public <T extends Node> T scope(@NotNull Parser parser, @NotNull Supplier<T> supplier) {
            try {
                grammar = new ScopedGrammar(grammar);
                return supplier.get();
            } finally {
                assert grammar.parent != null;
                if (grammar.clear() > 0) {
                    // 清理可能会用被清除的语法规则解析的并缓存的 token
                    parser.lexer.resetNext();
                }
                grammar = grammar.parent;
            }
        }

        @Override public Nud prefixNud(@NotNull Token tok) { return grammar.prefixNud(tok); }
        @Override public Led infixLed(@NotNull Token tok) { return grammar.infixLed(tok); }
        @Override public int infixLbp(@NotNull Token tok) { return grammar.infixLbp(tok); }
    }

    public final @Nullable ScopedGrammar parent;

    private ScopedGrammar() {
        super();
        parent = null;
    }

    private ScopedGrammar(@NotNull ScopedGrammar parent) {
        this.parent = parent;
    }

    int clear() {
        // 清除动态生成的 tokenTypes
        // prefix infix lbs 不用删, 离开作用域会全部清除
        // 但是 tokenTypes 保存在 lexicon 中, 没做作用域处理, 暂时 hack 一把
        for (Runnable clear : clearTokenTypes) {
            clear.run();
        }
        return clearTokenTypes.size();
    }

    public Nud prefixNud(@NotNull Token tok) {
        Nud nud = prefix.get(tok.type);
        if (nud != null) {
            return nud;
        } else if (parent != null) {
            return parent.prefixNud(tok);
        } else {
            return super.prefixNud(tok);
        }
    }

    public Led infixLed(@NotNull Token tok) {
        Led led = infix.get(tok.type);
        if (led != null) {
            return led;
        } else if (parent != null) {
            return parent.infixLed(tok);
        } else {
            return super.infixLed(tok);
        }
    }

    public int infixLbp(@NotNull Token tok) {
        Integer lbp = lbps.get(tok.type);
        if (lbp != null) {
            return lbp;
        } else if (parent != null) {
            return parent.infixLbp(tok);
        } else {
            return super.infixLbp(tok);
        }
    }
}
