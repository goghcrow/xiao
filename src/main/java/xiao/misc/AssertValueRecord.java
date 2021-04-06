package xiao.misc;

import org.jetbrains.annotations.NotNull;
import xiao.Value;

import java.util.List;

import static xiao.misc.Helper.lists;

/**
 * @author chuxiaofeng
 */
class AssertValueRecord {

    static class Val {
        final Value v;
        final int col; // index start with 1

        Val(@NotNull Value v, int col) {
            this.v = v;
            this.col = col;
        }
    }

    final List<Val> vals = lists();

    public void clear() {
        vals.clear();
    }

    public Value rec(Value v, int col) {
        for (Val it : vals) {
            if (it.col == col) {
                return rec(v, col + 1);
            }
        }
        vals.add(new Val(v, col));
        return v;
    }
}
