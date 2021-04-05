package xiao;

import org.jetbrains.annotations.NotNull;
import xiao.misc.Error;
import xiao.misc.Location;

import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static xiao.Literal.*;
import static xiao.Stack.*;
import static xiao.Type.*;
import static xiao.Value.*;

/**
 * @author chuxiaofeng
 */
public class PrimFunTypeChecker {

    public static Value add(@NotNull Location loc, @NotNull PrimArgs args) {
        if (args.size() == 1) {
            Value v1 = args.get(loc, 0);
            if (isInt(v1)) {
                return v1;
            } else if (isFloat(v1)) {
                return v1;
            } else {
                throw Error.type(loc, "不支持 " + "+" + ", lhs = " + v1);
            }
        } else {
            Value carry = args.get(loc, 0);
            for (int i = 1; i < args.size(); i++) {
                Value v2 = args.get(loc, i);

                if (isInt(carry) && isInt(v2)) {
                    if (isLiteral(carry) && isLiteral(v2)) {
                        carry = IntType(litInt(carry) + litInt(v2));
                    } else {
                        carry = INT;
                    }
                } else if (isInt(carry) && isFloat(v2)) {
                    if (isLiteral(carry) && isLiteral(v2)) {
                        carry = FloatType(litInt(carry) + litFloat(v2));
                    } else {
                        carry = FLOAT;
                    }
                } else if (isFloat(carry) && isInt(v2)) {
                    if (isLiteral(carry) && isLiteral(v2)) {
                        carry = FloatType(litFloat(carry) + litInt(v2));
                    } else {
                        carry = FLOAT;
                    }
                } else if (isFloat(carry) && isFloat(v2)) {
                    if (isLiteral(carry) && isLiteral(v2)) {
                        carry = FloatType(litFloat(carry) + litFloat(v2));
                    } else {
                        carry = FLOAT;
                    }
                } else if (isNum(carry) && isNum(v2)) {
                    carry = FLOAT;
                } else {
                    throw Error.type(loc, "不支持 " + "+" + ", lhs = " + carry + ". rhs = " + v2);
                }
            }
            return carry;
        }
    }

    public static Value sub(@NotNull Location loc, @NotNull PrimArgs args) {
        if (args.size() == 1) {
            Value v1 = args.get(loc, 0);
            if (isInt(v1)) {
                return v1;
            } else if (isFloat(v1)) {
                return v1;
            } else {
                throw Error.type(loc, "不支持 " + "-" + ", lhs = " + v1);
            }
        } else {
            Value carry = args.get(loc, 0);
            for (int i = 1; i < args.size(); i++) {
                Value v2 = args.get(loc, i);
                if (isInt(carry) && isInt(v2)) {
                    if (isLiteral(carry) && isLiteral(v2)) {
                        carry = IntType(litInt(carry) - litInt(v2));
                    } else {
                        carry = INT;
                    }
                } else if (isInt(carry) && isFloat(v2)) {
                    if (isLiteral(carry) && isLiteral(v2)) {
                        carry = FloatType(litInt(carry) - litFloat(v2));
                    } else {
                        carry = FLOAT;
                    }
                } else if (isFloat(carry) && isInt(v2)) {
                    if (isLiteral(carry) && isLiteral(v2)) {
                        carry = FloatType(litFloat(carry) - litInt(v2));
                    } else {
                        carry = FLOAT;
                    }
                } else if (isFloat(carry) && isFloat(v2)) {
                    if (isLiteral(carry) && isLiteral(v2)) {
                        carry = FloatType(litFloat(carry) - litFloat(v2));
                    } else {
                        carry = FLOAT;
                    }
                } else if (isNum(carry) && isNum(v2)) {
                    carry = FLOAT;
                } else {
                    throw Error.type(loc, "不支持 " + "-" + ", lhs = " + carry + ". rhs = " + v2);
                }
            }
            return carry;
        }
    }

    public static Value multi(@NotNull Location loc, @NotNull PrimArgs args) {
        Value carry = args.get(loc, 0);
        for (int i = 1; i < args.size(); i++) {
            Value v2 = args.get(loc, i);
            if (isInt(carry) && isInt(v2)) {
                if (isLiteral(carry) && isLiteral(v2)) {
                    carry = IntType(litInt(carry) * litInt(v2));
                } else {
                    carry = INT;
                }
            } else if (isInt(carry) && isFloat(v2)) {
                if (isLiteral(carry) && isLiteral(v2)) {
                    carry = FloatType(litInt(carry) * litFloat(v2));
                } else {
                    carry = FLOAT;
                }
            } else if (isFloat(carry) && isInt(v2)) {
                if (isLiteral(carry) && isLiteral(v2)) {
                    carry = FloatType(litFloat(carry) * litInt(v2));
                } else {
                    carry = FLOAT;
                }
            } else if (isFloat(carry) && isFloat(v2)) {
                if (isLiteral(carry) && isLiteral(v2)) {
                    carry = FloatType(litFloat(carry) * litFloat(v2));
                } else {
                    carry = FLOAT;
                }
            } else if (isNum(carry) && isNum(v2)) {
                carry = FLOAT;
            } else {
                throw Error.type(loc, "不支持 " + "*" + ", lhs = " + carry + ". rhs = " + v2);
            }
        }
        return carry;
    }

    public static Value div(@NotNull Location loc, @NotNull PrimArgs args) {
        Value carry = args.get(loc, 0);
        for (int i = 1; i < args.size(); i++) {
            Value v2 = args.get(loc, i);
            if (isInt(carry) && isInt(v2)) {
                if (isLiteral(carry) && isLiteral(v2)) {
                    carry = IntType(litInt(carry) / litInt(v2));
                } else {
                    carry = INT;
                }
            } else if (isInt(carry) && isFloat(v2)) {
                if (isLiteral(carry) && isLiteral(v2)) {
                    carry = FloatType(litInt(carry) / litFloat(v2));
                } else {
                    carry = FLOAT;
                }
            } else if (isFloat(carry) && isInt(v2)) {
                if (isLiteral(carry) && isLiteral(v2)) {
                    carry = FloatType(litFloat(carry) / litInt(v2));
                } else {
                    carry = FLOAT;
                }
            } else if (isFloat(carry) && isFloat(v2)) {
                if (isLiteral(carry) && isLiteral(v2)) {
                    carry = FloatType(litFloat(carry) / litFloat(v2));
                } else {
                    carry = FLOAT;
                }
            } else if (isNum(carry) && isNum(v2)) {
                carry = FLOAT;
            } else {
                throw Error.type(loc, "不支持 " + "/" + ", lhs = " + carry + ". rhs = " + v2);
            }
        }
        return carry;
    }

    public static Value lt(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);

        if (isInt(v1) && isInt(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litInt(v1) < litInt(v2));
            } else {
                return BOOL;
            }
        }
        if (isInt(v1) && isFloat(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litInt(v1) < litFloat(v2));
            } else {
                return BOOL;
            }
        }
        if (isFloat(v1) && isInt(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litFloat(v1) < litInt(v2));
            } else {
                return BOOL;
            }
        }
        if (isFloat(v1) && isFloat(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litFloat(v1) < litFloat(v2));
            } else {
                return BOOL;
            }
        }
        if (isNum(v1) && isNum(v2)) {
            return BOOL;
        }
        if (isString(v1) && isString(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litString(v1).compareTo(litString(v2)) < 0);
            } else {
                return BOOL;
            }
        }

        throw Error.type(loc, "不支持 " + "<" + ", lhs = " + v1 + ". rhs = " + v2);
    }
    
    public static Value lte(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);

        if (isInt(v1) && isInt(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litInt(v1) <= litInt(v2));
            } else {
                return BOOL;
            }
        }
        if (isInt(v1) && isFloat(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litInt(v1) <= litFloat(v2));
            } else {
                return BOOL;
            }
        }
        if (isFloat(v1) && isInt(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litFloat(v1) <= litInt(v2));
            } else {
                return BOOL;
            }
        }
        if (isFloat(v1) && isFloat(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litFloat(v1) <= litFloat(v2));
            } else {
                return BOOL;
            }
        }
        if (isNum(v1) && isNum(v2)) {
            return BOOL;
        }
        if (isString(v1) && isString(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litString(v1).compareTo(litString(v2)) <= 0);
            } else {
                return BOOL;
            }
        }

        throw Error.type(loc, "不支持 " + "<=" + ", lhs = " + v1 + ". rhs = " + v2);
    }

    public static Value gt(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);

        if (isInt(v1) && isInt(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litInt(v1) > litInt(v2));
            } else {
                return BOOL;
            }
        }
        if (isInt(v1) && isFloat(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litInt(v1) > litFloat(v2));
            } else {
                return BOOL;
            }
        }
        if (isFloat(v1) && isInt(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litFloat(v1) > litInt(v2));
            } else {
                return BOOL;
            }
        }
        if (isFloat(v1) && isFloat(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litFloat(v1) > litFloat(v2));
            } else {
                return BOOL;
            }
        }
        if (isNum(v1) && isNum(v2)) {
            return BOOL;
        }
        if (isString(v1) && isString(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litString(v1).compareTo(litString(v2)) > 0);
            } else {
                return BOOL;
            }
        }

        throw Error.type(loc, "不支持 " + ">" + ", lhs = " + v1 + ". rhs = " + v2);
    }

    public static Value gte(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);

        if (isInt(v1) && isInt(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litInt(v1) >= litInt(v2));
            } else {
                return BOOL;
            }
        }
        if (isInt(v1) && isFloat(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litInt(v1) >= litFloat(v2));
            } else {
                return BOOL;
            }
        }
        if (isFloat(v1) && isInt(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litFloat(v1) >= litInt(v2));
            } else {
                return BOOL;
            }
        }
        if (isFloat(v1) && isFloat(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litFloat(v1) >= litFloat(v2));
            } else {
                return BOOL;
            }
        }
        if (isNum(v1) && isNum(v2)) {
            return BOOL;
        }
        if (isString(v1) && isString(v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                return BoolType(litString(v1).compareTo(litString(v2)) >= 0);
            } else {
                return BOOL;
            }
        }

        throw Error.type(loc, "不支持 " + ">=" + ", lhs = " + v1 + ". rhs = " + v2);
    }

    public static Value eq(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);

        if (TypeEquiv.support(v1, v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                if (Objects.equals(literal(v1), literal(v2))) {
                    return TYPE_TRUE;
                } else {
                    return TYPE_FALSE;
                }
            } else {
                return BOOL;
            }
        } else {
            throw Error.type(loc, "不支持 ==, lhs = " + v1 + ". rhs = " + v2);
        }
    }

    public static Value ne(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        Value v2 = args.get(loc, 1);

        if (TypeEquiv.support(v1, v2)) {
            if (isLiteral(v1) && isLiteral(v2)) {
                if (Objects.equals(literal(v1), literal(v2))) {
                    return TYPE_FALSE;
                } else {
                    return TYPE_TRUE;
                }
            } else {
                return BOOL;
            }
        } else {
            throw Error.type(loc, "不支持 !=, lhs = " + v1 + ". rhs = " + v2);
        }
    }

    public static Value and(@NotNull Location loc, @NotNull PrimArgs args) {
        Value[] argVals = args.args(loc);
        boolean mustFalse = false;
        for (Value arg : argVals) {
            if (isBool(arg)) {
                if (isLiteral(arg) && !litBool(arg)) {
                    mustFalse = true;
                }
            } else {
                throw Error.type(loc, "不支持 " + "&&" + ", arg = " + arg);
            }
        }
        if (mustFalse) {
            // return LiteralType(TYPE_FALSE);
            return TYPE_FALSE;
        } else {
            return BOOL;
        }
    }

    public static Value or(@NotNull Location loc, @NotNull PrimArgs args) {
        Value[] argVals = args.args(loc);
        boolean mustTrue = false;
        for (Value arg : argVals) {
            if (isBool(arg)) {
                if (isLiteral(arg) && litBool(arg)) {
                    mustTrue = true;
                }
            } else {
                throw Error.type(loc, "不支持 " + "||" + ", arg = " + arg);
            }
        }
        if (mustTrue) {
            // return LiteralType(TYPE_TRUE);
            return TYPE_TRUE;
        } else {
            return BOOL;
        }
    }

    public static Value not(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v1 = args.get(loc, 0);
        if (isBool(v1)) {
            if (isLitTrue(v1)) {
                return TYPE_FALSE;
            } else if (isLitFalse(v1)) {
                return TYPE_TRUE;
            } else {
                return BOOL;
            }
        } else {
            throw Error.type(loc, "不支持 ！, arg = " + v1);
        }
    }

    static boolean supportConnect(Value v, MonoStack st) {
        if (st.contains(v)) {
            return true;
        }
        if (v instanceof UnionType) {
            st.push(v);
            for (Value t : ((UnionType) v).types) {
                if (!supportConnect(t, st)) {
                    st.pop(v);
                    return false;
                }
            }
            st.pop(v);
            return true;
        } else if (isString(v) || isBool(v) || isFloat(v) || isInt(v)) {
            // 只有这几种东西支持字符串拼接, isFloat 判断优先 isInt, isInt 这里其实没用
            return true;
        } else {
            return false;
        }
    }

    public static Value connect(@NotNull Location loc, @NotNull PrimArgs args) {
        boolean allLit = true;
        Value[] argVals = args.args(loc);
        for (Value arg : argVals) {
            if (supportConnect(arg, valueStack())) {
                if (!isLiteral(arg)) {
                    allLit = false;
                }
            } else {
                throw Error.type(loc, "不支持 ++, arg = " + arg);
            }
        }

        if (allLit) {
            StringBuilder s = new StringBuilder();
            for (Value arg : argVals) {
                // s.append(litString(arg));
                Object lit = literal(arg);
                assert lit != null;
                s.append(lit.toString());
            }
            return StringType(s.toString());
        } else {
            return STR;
        }
    }

    public static Value unionOf(@NotNull Location loc, @NotNull PrimArgs args) {
        return union(args.args(loc));
    }

    public static Value as(@NotNull Location loc, @NotNull PrimArgs args) {
        Value v = args.get(loc, 0);
        Value cast = args.get(loc, 1);
        if (/*v == UNKNOWN || */v == ANY) {
            return cast;
        }
        if (!castSafe(v, cast)) {
            throw Error.type(loc, v + " 不能 cast 成 " + cast);
        }
        return TypeChecker.hackExpected(cast, v);
    }

//    public static Value typeOf(@NotNull Location loc, @NotNull PrimArgs args) {
//        return typeof(args.get(loc, 0));
//    }

    public static Value eltType(@NotNull Location loc, @NotNull PrimArgs args) {
        Value a = args.get(loc, 0);
        if (a instanceof VectorType) {
            return ((VectorType) a).eltType;
        } else {
            return NEVER;
        }
    }
    public static Value iff(@NotNull Location loc, @NotNull PrimArgs args) {
        Value test = args.get(loc, 0);
        if (isBool(test)) {
            Value t1 = args.get(loc, 1);
            Value t2 = args.get(loc, 2);
            t1 = TypeChecker.literalWrapIfNecessary(false, t1);
            t2 = TypeChecker.literalWrapIfNecessary(false, t2);
            if (isLitTrue(test)) {
                return t1;
            } else if (isLitFalse(test)) {
                return t2;
            } else {
                return union(t1, t2);
            }
        } else {
            throw Error.type(loc, "if 条件必须是 bool 类型");
        }
    }


    // 不能用 v instanceof XXXPrimType, 因为有字面类型
    static boolean isBool(Value v)      { return subtype(v, BOOL);  }
    static boolean isInt(Value v)       { return subtype(v, INT);   }
    static boolean isFloat(Value v)     { return subtype(v, FLOAT); }
    static boolean isNum(Value v)       { return isFloat(v);        }
    static boolean isString(Value v)    { return subtype(v, STR);   }

    static boolean litBool(Value v)     { return requireNonNull(literal(v)); }
    static long    litInt(Value v)      { return requireNonNull(literal(v)); }
    static double  litFloat(Value v)    { return requireNonNull(literal(v)); }
    static String  litString(Value v)   { return requireNonNull(literal(v)); }
}
