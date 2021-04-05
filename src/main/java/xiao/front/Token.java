package xiao.front;

import org.jetbrains.annotations.NotNull;
import xiao.misc.Location;

import java.util.Objects;

/**
 * @author chuxiaofeng
 */
public class Token {

    final static Token EOF = new Token(TokenType.EOF, "", Location.None, true);

    @NotNull /*final */TokenType type;
    @NotNull final String lexeme;
    public @NotNull final Location loc;
    final boolean keep;

    public Token(@NotNull TokenType type, @NotNull String lexeme, @NotNull Location loc, boolean keep) {
        this.type = type;
        this.lexeme = lexeme;
        this.loc = loc;
        this.keep = keep;
    }

    public boolean is(@NotNull TokenType type) {
        return Objects.equals(this.type, type);
    }

    @Override
    public String toString() {
        if (lexeme.equalsIgnoreCase("\n")) {
            return "\\n";
        } else {
            return lexeme;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return keep == token.keep &&
                type == token.type &&
                lexeme.equals(token.lexeme) &&
                loc.equals(token.loc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, lexeme, loc, keep);
    }
}
