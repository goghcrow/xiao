package xiao;

import org.jetbrains.annotations.NotNull;
import xiao.misc.Error;
import xiao.misc.Location;

import java.util.Objects;

import static xiao.Type.*;
import static xiao.Type.typeof;
import static xiao.Type.union;
import static xiao.Value.Float;
import static xiao.Value.*;

/**
 * @author chuxiaofeng
 */
public class PrimFunInterp {

    // throw new IllegalStateException(); 代表 typecheck 已经检查过了 不会发生

    static Value add2(Location loc, Value v1, Value v2) {
        if (v1 instanceof IntValue && v2 instanceof IntValue) {
            return Int(((IntValue) v1).value + ((IntValue) v2).value);
        }
        if (v1 instanceof IntValue && v2 instanceof FloatValue) {
            return Float(((IntValue) v1).value + ((FloatValue) v2).value);
        }
        if (v1 instanceof FloatValue && v2 instanceof IntValue) {
            return Float(((FloatValue) v1).value + ((IntValue) v2).value);
        }
        if (v1 instanceof FloatValue && v2 instanceof FloatValue) {
            return Float(((FloatValue) v1).value + ((FloatValue) v2).value);
        }
        throw Error.type(loc, "不支持 + : v1 = " + v1 + ", v2 = " + v2);
    }

    public static Value add(@NotNull Location loc, @NotNull PrimArgs args) {
        if (args.size() == 1) {
            Value v1 = args.get(loc, 0);
            if (v1 instanceof IntValue) {
                return v1;
            }
            if (v1 instanceof FloatValue) {
                return v1;
            }
            throw new IllegalStateException();
        } else {
            Value carry = args.get(loc, 0);
            for (int i = 1; i < args.size(); i++) {
                carry = add2(loc, carry, args.get(loc, i));
            }
            return carry;
        }
    }

    static Value sub2(Location loc, Value v1, Value v2) {
        if (v1 instanceof IntValue && v2 instanceof IntValue) {
            return Int(((IntValue) v1).value - ((IntValue) v2).value);
        }
        if (v1 instanceof IntValue && v2 instanceof FloatValue) {
            return Float(((IntValue) v1).value - ((FloatValue) v2).value);
        }
        if (v1 instanceof FloatValue && v2 instanceof IntValue) {
            return Float(((FloatValue) v1).value - ((IntValue) v2).value);
        }
        if (v1 instanceof FloatValue && v2 instanceof FloatValue) {
            return Float(((FloatValue) v1).value - ((FloatValue) v2).value);
        }
        throw Error.type(loc, "不支持 - : v1 = " + v1 + ", v2 = " + v2);
    }

    public static Value sub(@NotNull Location loc, @NotNull PrimArgs args) {
        if (args.size() == 1) {
            Value v1 = args.get(loc, 0);
            if (v1 instanceof IntValue) {
                return Int(-((IntValue) v1).value);
            }
            if (v1 instanceof FloatValue) {
                return Float(-((FloatValue) v1).value);
            }
            throw new IllegalStateException();
        } else {
            Value carry = args.get(loc, 0);
            for (int i = 1; i < args.size(); i++) {
                carry = sub2(loc, carry, args.get(loc, i));
            }
            return carry;
        }
    }

    static Value multi2(Location loc, Value v1, Value v2) {
        if (v1 instanceof IntValue && v2 instanceof IntValue) {
            return Int(((IntValue) v1).value * ((IntValue) v2).value);
        }
        if (v1 instanceof IntValue && v2 instanceof FloatValue) {
            return Float(((IntValue) v1).value * ((FloatValue) v2).value);
        }
        if (v1 instanceof FloatValue && v2 instanceof IntValue) {
            return Float(((FloatValue) v1).value * ((IntValue) v2).value);
        }
        if (v1 instanceof FloatValue && v2 instanceof FloatValue) {
            return Float(((FloatValue) v1).value * ((FloatValue) v2).value);
        }
        throw Error.type(loc, "不支持 * : v1 = " + v1 + ", v2 = " + v2);
    }

    public static Value multi(@NotNull Location loc, @NotNull PrimArgs args) {
        Value carry = args.get(loc, 0);
        for (int i = 1; i < args.size(); i++) {
            carry = multi2(loc, carry, args.get(loc, i));
        }
        return carry;
    }

    static Value div2(Location loc, Value v1, Value v2) {
        if (v1 instanceof IntValue && v2 instanceof IntValue) {
            return Int(((IntValue) v1).value / ((IntValue) v2).value);
        }
        if (v1 instanceof IntValue && v2 instanceof FloatValue) {
            return Float(((IntValue) v1).value / ((FloatValue) v2).value);
        }
        if (v1 instanceof FloatValue && v2 instanceof IntValue) {
            return Float(((FloatValue) v1).value / ((IntValue) v2).value);
        }
        if (v1 instanceof FloatValue && v2 instanceof FloatValue) {
            return Float(((FloatValue) v1).value / ((FloatValue) v2).value);
        }
        throw Error.type(loc, "不支持 / : v1 = " + v1 + ", v2 = " + v2);
    }

    public static Value div(@NotNull Location loc, @NotNull PrimArgs args) {
        Value carry = args.get(loc, 0);
        for (int i = 1; i < args.size(); i++) {
            carry = div2(loc, carry, args.get(loc, i));
        }
        return carry;
    }

    public static Value lt(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);
        if (v1 instanceof IntValue && v2 instanceof IntValue) {
            return ((IntValue) v1).value < ((IntValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof IntValue && v2 instanceof FloatValue) {
            return ((IntValue) v1).value < ((FloatValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof FloatValue && v2 instanceof IntValue) {
            return ((FloatValue) v1).value < ((IntValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof FloatValue && v2 instanceof FloatValue) {
            return ((FloatValue) v1).value < ((FloatValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof StringValue && v2 instanceof StringValue) {
            return ((StringValue) v1).value.compareTo(((StringValue) v2).value) < 0 ? TRUE : FALSE;
        }
        throw Error.type(loc, "不支持 < : v1 = " + v1 + ", v2 = " + v2);
    }

    public static Value lte(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);
        if (v1 instanceof IntValue && v2 instanceof IntValue) {
            return ((IntValue) v1).value <= ((IntValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof IntValue && v2 instanceof FloatValue) {
            return ((IntValue) v1).value <= ((FloatValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof FloatValue && v2 instanceof IntValue) {
            return ((FloatValue) v1).value <= ((IntValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof FloatValue && v2 instanceof FloatValue) {
            return ((FloatValue) v1).value <= ((FloatValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof StringValue && v2 instanceof StringValue) {
            return ((StringValue) v1).value.compareTo(((StringValue) v2).value) <= 0 ? TRUE : FALSE;
        }
        throw Error.type(loc, "不支持 <= : v1 = " + v1 + ", v2 = " + v2);
    }

    public static Value gt(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);
        if (v1 instanceof IntValue && v2 instanceof IntValue) {
            return ((IntValue) v1).value > ((IntValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof IntValue && v2 instanceof FloatValue) {
            return ((IntValue) v1).value > ((FloatValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof FloatValue && v2 instanceof IntValue) {
            return ((FloatValue) v1).value > ((IntValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof FloatValue && v2 instanceof FloatValue) {
            return ((FloatValue) v1).value > ((FloatValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof StringValue && v2 instanceof StringValue) {
            return ((StringValue) v1).value.compareTo(((StringValue) v2).value) > 0 ? TRUE : FALSE;
        }
        throw Error.type(loc, "不支持 > : v1 = " + v1 + ", v2 = " + v2);
    }

    public static Value gte(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);
        if (v1 instanceof IntValue && v2 instanceof IntValue) {
            return ((IntValue) v1).value >= ((IntValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof IntValue && v2 instanceof FloatValue) {
            return ((IntValue) v1).value >= ((FloatValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof FloatValue && v2 instanceof IntValue) {
            return ((FloatValue) v1).value >= ((IntValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof FloatValue && v2 instanceof FloatValue) {
            return ((FloatValue) v1).value >= ((FloatValue) v2).value ? TRUE : FALSE;
        }
        if (v1 instanceof StringValue && v2 instanceof StringValue) {
            return ((StringValue) v1).value.compareTo(((StringValue) v2).value) >= 0 ? TRUE : FALSE;
        }
        throw Error.type(loc, "不支持 >=: v1 = " + v1 + ", v2 = " + v2);
    }

    public static Value eq(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);
        return Objects.equals(v1, v2) ? TRUE : FALSE;
    }

    public static Value ne(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);
        return !Objects.equals(v1, v2) ? TRUE : FALSE;
    }

    public static Value and(@NotNull Location loc, @NotNull PrimArgs args) {
        // 注意短路
        for (int i = 0; i < args.size(); i++) {
            Value v = args.get(loc, i);
            TypeChecker.boolAssert(loc, v);
            if (v == FALSE) {
                return FALSE;
            }
        }
        return TRUE;
    }

    public static Value or(@NotNull Location loc, @NotNull PrimArgs args) {
        // 注意短路
        for (int i = 0; i < args.size(); i++) {
            Value v = args.get(loc, i);
            TypeChecker.boolAssert(loc, v);
            if (v == TRUE) {
                return TRUE;
            }
        }
        return FALSE;
    }

    public static Value not(@NotNull Location loc, @NotNull PrimArgs args) {
        return ((BoolValue) args.get(loc, 0)).not();
    }

    public static Value connect(@NotNull Location loc, @NotNull PrimArgs args) {
        StringBuilder buf = new StringBuilder();
        for (Value arg : args.args(loc)) {
            if (arg instanceof StringValue) {
                buf.append(((StringValue) arg).value);
            } else {
                buf.append(arg.toString());
            }
        }
        return Str(buf.toString());
    }

    public static Value unionOf(@NotNull Location loc, @NotNull PrimArgs args) {
        return union(args.args(loc));
    }

    public static Value as(@NotNull Location loc, @NotNull PrimArgs args) {
        return args.get(loc, 0);
    }

//    public static Value typeOf(@NotNull Location loc, @NotNull PrimArgs args) {
//        return typeof(args.get(loc, 0));
//    }

    public static Value eltType(@NotNull Location loc, @NotNull PrimArgs args) {
        Value a = args.get(loc, 0);
        Value t = typeof(a);
        if (t instanceof VectorType) {
            return ((VectorType) t).eltType;
        } else {
            return NEVER;
        }
    }

    public static Value iff(@NotNull Location loc, @NotNull PrimArgs args) {
        Value test = args.get(loc, 0);
        TypeChecker.boolAssert(loc, test);
        if (test == TRUE) {
            return args.get(loc, 1);
        } else {
            return args.get(loc, 2);
        }
    }
}
