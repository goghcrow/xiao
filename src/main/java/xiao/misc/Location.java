package xiao.misc;

import org.jetbrains.annotations.NotNull;
import xiao.front.Ast;
import xiao.front.Token;

import java.util.List;

/**
 * @author chuxiaofeng
 */
public class Location {
    public final static Location None = new Location("", -1, -1, -1, -1, -1, -1);

    public @NotNull final String input;

    public final int idxBegin;  // inclusive
    public final int idxEnd;    // exclusive
    public final int rowBegin;  // inclusive
    public final int colBegin;  // inclusive
    public final int rowEnd;    // exclusive
    public final int colEnd;    // exclusive

    private Location(@NotNull String input,
                     int idxBegin, int idxEnd, int rowBegin, int colBegin, int rowEnd, int colEnd) {
        this.input = input;
        this.idxBegin = idxBegin;
        this.idxEnd = idxEnd;
        this.rowBegin = rowBegin;
        this.colBegin = colBegin;
        this.rowEnd = rowEnd;
        this.colEnd = colEnd;
    }

    public static Location ofStr(@NotNull String input, @NotNull String matched,
                          int idxBegin, int idxEnd, int rowBegin, int colBegin) {
        int rowEnd = rowBegin;
        int colEnd = colBegin;
        for (char c : matched.toCharArray()) {
            switch (c) {
                case '\r': break;
                case '\n':
                    rowEnd++;
                    colEnd = 1;
                    break;
                default: colEnd++;
            }
        }
        return new Location(input, idxBegin, idxEnd, rowBegin, colBegin, rowEnd, colEnd);
    }

    public String codeSpan() {
        if (this == None) {
            return "";
        } else {
            return input.substring(idxBegin, idxEnd);
        }
    }

    public boolean contains(@NotNull Location other) {
        if (this == None || other == None) {
            return false;
        }
        return idxBegin <= other.idxBegin && idxEnd >= other.idxEnd;
    }

    public String inspect(@NotNull String msg) {
        return Inspector.inspect(this, msg);
    }

    public static Location rangeNodes(Location loc, @NotNull List<Ast.Node> nodes) {
        if (nodes.isEmpty()) {
            return loc;
        }
        return range(nodes.get(0).loc, nodes.get(nodes.size() - 1).loc);
    }

    public static Location range(@NotNull List<Token> toks) {
        if (toks.isEmpty()) {
            return None;
        }
        return range(toks.get(0), toks.get(toks.size() - 1));
    }

    public static Location range(@NotNull Token from, @NotNull Token to) {
        return range(from.loc, to.loc);
    }

    public static Location range(@NotNull Location from, @NotNull Location to) {
        return new Location(from.input, from.idxBegin, to.idxEnd, from.rowBegin, from.colBegin, to.rowEnd, to.colEnd);
    }

    @Override
    public String toString() {
        if (this == None) {
            return "None";
        } else {
            return "第" + rowBegin + "行第" + colBegin + "列";
        }
    }

    @Override
    public int hashCode() {
        return idxBegin + idxEnd + rowBegin + colBegin + rowEnd + colEnd;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Location) {
            Location other = (Location) obj;
            return idxBegin == other.idxBegin && idxEnd == other.idxEnd
                    && rowBegin == other.rowBegin && colBegin == other.colBegin
                    && rowEnd == other.rowEnd && colEnd == other.colEnd;
        }
        return false;
    }

}
