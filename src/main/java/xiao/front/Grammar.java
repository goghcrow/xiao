package xiao.front;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static xiao.front.Ast.Node;
import static xiao.front.Parser.Led;
import static xiao.front.Parser.Nud;

/**
 * @author chuxiaofeng
 */
public interface Grammar {

    Nud prefixNud(@NotNull Token tok);

    Led infixLed(@NotNull Token tok);

    int infixLbp(@NotNull Token tok);

    // 处理文法作用域内的规则等
    default <T extends Node> T scope(@NotNull Parser parser, @NotNull Supplier<T> supplier) {
        return supplier.get();
    }

    static Grammar simple() {
        return new SimpleGrammar();
    }

    static Grammar scoped() {
        return new ScopedGrammar.Wrapper();
    }

}
