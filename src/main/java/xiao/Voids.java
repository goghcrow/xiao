package xiao;

import org.jetbrains.annotations.NotNull;
import xiao.misc.Error;
import xiao.misc.Location;

import static xiao.Stack.MonoStack;
import static xiao.Stack.valueStack;
import static xiao.Type.*;

/**
 * @author chuxiaofeng
 */
public class Voids {

    public static Value not(Location loc, Value v, String msg) {
        if (contains(v)) {
            throw Error.type(loc, msg);
        } else {
            return v;
        }
    }

    public static boolean contains(@NotNull Value t) {
        return contains(t, valueStack());
    }

    static boolean contains(@NotNull Value t, MonoStack st) {
        if (st.contains(t)) {
            return false;
        }

        if (t instanceof UnionType) {
            st.push(t);
            for (Value ut : ((UnionType) t).types) {
                if (contains(ut)) {
                    st.pop(t);
                    return true;
                }
            }
            st.pop(t);
            return false;
        } else if (t instanceof TupleType && ((TupleType) t).size() != 0) {
            st.push(t);
            boolean b = contains(((TupleType) t).eltType());
            st.pop(t);
            return b;
        } else if (t instanceof VectorType) {
            st.push(t);
            boolean b = contains(((VectorType) t).eltType);
            st.pop(t);
            return b;
        } else {
            return t == VOID;
        }
    }
}
