package xiao;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiao.misc.Error;
import xiao.misc.Helper;
import xiao.misc.Location;

import java.util.*;
import java.util.function.Supplier;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;
import static xiao.Constant.ID_RETURN;
import static xiao.Literal.literal;
import static xiao.ToString.stringfy;
import static xiao.TypeEquiv.*;
import static xiao.Value.*;
import static xiao.front.Ast.*;

/**
 * @author chuxiaofeng
 *  int float bool str : primitive value type
 *  tuples : value type, immutable, covariant
 *  vector : ref type, mutable, invariant
 *  fun : ref type
 *  record : ref type
 */
@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
public interface Type {
    // 在和其他类型 union 过程, 在排除掉, union | T == T
    // 处理空 vec面量, UNKNOWN[]
    // 空vec面量既是子类又是超类
    // let v: Int[] = []  是其他vec的子类
    Value UNKNOWN = new UnknownType(); // Synthetic 合成类型
    Value NEVER   = new NeverType(); // 用来处理 没有 body 没有 break return 的 while true、或者只有 throw 的 block 等
    // Value UNIT = TupleType();
    Value VOID    = Value.VOID;
    Value ANY     = new AnyType(); // 类型安全的 TOP 类型
    Value BOOL    = new BoolType();
    Value INT     = new IntType();
    Value FLOAT   = new FloatType();
    Value STR     = new StringType();

    BoolType TYPE_TRUE     = new BoolType(true);
    BoolType TYPE_FALSE    = new BoolType(false);

    static BoolType     BoolType(boolean literal)           { return literal ? TYPE_TRUE : TYPE_FALSE; }
    static IntType      IntType(Long literal)               { return new IntType(literal);      }
    static FloatType    FloatType(Double literal)           { return new FloatType(literal);    }
    static StringType   StringType(String literal)          { return new StringType(literal);   }
    static TupleType    TupleType(List<Value> types)        { return new TupleType(types);      }
    // static TupleType    TupleType(Value ...types)           { return new TupleType(types);      }
    static VectorType   VectorType(Value eltType)           { return new VectorType(eltType);   }
    static ModuleType   ModuleType(Module m, Scope s)       { return new ModuleType(m, s);      }
    static Value        union(Collection<Value> types)      { return UnionType.union(types);    }
    static Value        union(Value ...types)               { return UnionType.union(types);    }
    static Value        union(Value v1, Value v2)           { return UnionType.union2(v1, v2);  }

    static FunType      FunType(FunDef fun, Scope props, Scope env)     { return new FunType(fun, props, env);   }
    static VectorType   VectorType(Value eltType, List<Value> types)    { return new VectorType(eltType, types); }
    static RecordType   RecordType(@Nullable String name, Node def, Scope props, Scope env) {
        return new RecordType(name, def, props, env);
    }

    static Value LiteralType(Value type) {
        if (type instanceof LiteralType) {
            return type;
        } else {
            if (isPrimLiteral(type)) {
                return new LiteralType(type);
            } else {
                return type;
            }
        }
    }

    static boolean isPrimLiteral(Value v) {
        boolean prim = v instanceof BoolType
                || v instanceof IntType
                || v instanceof FloatType
                || v instanceof StringType;
        return prim && isLiteral(v);
    }

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    static boolean isBool(Value v)      { return subtype(v, BOOL);     }
    static boolean isInt(Value v)       { return subtype(v, INT);      }
    static boolean isLiteral(Value v)   { return literal(v) != null;      } // 注意这里处理基础类型还返回了 tuple
    static boolean isLitTrue(Value v)   { return TRUE.equals(literal(v)); }
    static boolean isLitFalse(Value v)  { return FALSE.equals(literal(v));}
    static long    litInt(Value v)      { return requireNonNull(literal(v));     }

    static boolean isEmptyVectorLiteral(Value value) {
        return value instanceof VectorType && ((VectorType) value).isEmptyLiteral();
    }
    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    // literal  copy  typeof 等不想用 OO 的方式实现, 这样更简单一点, 改起来方便

    // 子类型判断的场景: define, assign, 函数参数, 返回值, 创建 record 实例(域赋值) , vec 添加元素(set,add)
    // t1 = actual, t2 = expected
    static boolean subtype(@NotNull Value type1, @NotNull Value type2) {
        return isSubtype(type1, type2);
    }

    static boolean castSafe(@NotNull Value type1, @NotNull Value type2) {
        return TypeEquiv.castSafe(type1, type2);
    }

    static boolean typeMatched(@NotNull Value type1, @NotNull Value type2) {
        return TypeEquiv.typeMatched(type1, type2);
    }

    static Value typeof(Value v) {
        if (v instanceof BoolValue) {
            return ((BoolValue) v).value ? TYPE_TRUE : TYPE_FALSE;
        } else if (v instanceof IntValue) {
            return IntType(((IntValue) v).value);
        } else if (v instanceof FloatValue) {
            return FloatType(((FloatValue) v).value);
        } else if (v instanceof StringValue) {
            return StringType(((StringValue) v).value);
        } else if (v instanceof TupleValue) {
            List<Value> types = new ArrayList<>();
            for (Value it : ((TupleValue) v).values) {
                types.add(typeof(it));
            }
            return TupleType(types);
        } else if (v instanceof VectorValue) {
            List<Value> types = new ArrayList<>();
            for (Value it : ((VectorValue) v).values) {
                types.add(typeof(it));
            }
            return VectorType(union(types), types);
        } else if (v instanceof RecordValue) {
            return ((RecordValue) v).type;
        } else if (v instanceof ModuleValue) {
            ModuleValue mv = (ModuleValue) v;
            return ModuleType(mv.module, mv.scope);
        } else if (v instanceof Closure) {
            Closure c = (Closure) v;
            // 注意这里没有填充参数和返回值类型, 因为 closure 只有在 interp 中才会创建
            // 参数返回值类型仅用在类型检查时候, 所以这里也没什么用
            return FunType(c.fun, c.props, c.env);
        } else if (v instanceof PrimFun) {
            // todo
            return v;
        } else {
            return v;
        }
    }

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // Synthetic
    final class UnknownType implements Value {
        private UnknownType() {}

        @Override
        public String toString() {
            return "Unknown";
        }
    }

    // Synthetic
    final class Undefined implements Value {
        final Supplier<Value> lazy;

        Undefined(Supplier<Value> lazy) {
            this.lazy = lazy;
        }

        @Override
        public String toString() {
            return "Undefined";
        }
    }

    // 目前值类型才有字面量类型: bool, int, float, string, tuple
    final class LiteralType implements Value {
        final @NotNull Value type;

        private LiteralType(@NotNull Value type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            return Equals.typeEquals(this, o);
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }

        @Override
        public String toString() {
            return stringfy(this);
        }
    }

    final class NeverType implements Value {
        private NeverType() {}

        @Override
        public String toString() {
            return "Never";
        }
    }

    final class AnyType implements Value {
        private AnyType() {}

        @Override
        public String toString() {
            return "Any";
        }
    }

    final class BoolType implements Value {
        public final @Nullable Boolean literal;

        private BoolType() {
            this.literal = null;
        }

        private BoolType(@NotNull Boolean literal) {
            this.literal = literal;
        }

        @Override
        public boolean equals(Object o) {
            return Equals.typeEquals(this, o);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public String toString() {
            if (literal == null) {
                return "Bool";
            } else {
                // return "Bool#" + literal;
                return String.valueOf(literal);
            }
        }
    }

    final class IntType implements Value {
        public final @Nullable Long literal;

        private IntType() {
            this.literal = null;
        }

        private IntType(@NotNull Long literal) {
            this.literal = literal;
        }

        @Override
        public boolean equals(Object o) {
            return Equals.typeEquals(this, o);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public String toString() {
            if (literal == null) {
                return "Int";
            } else {
                // return "Int#" + literal;
                return String.valueOf(literal);
            }
        }
    }

    final class FloatType implements Value {
        public final @Nullable Double literal;

        private FloatType() {
            this.literal = null;
        }

        private FloatType(@NotNull Double literal) {
            this.literal = literal;
        }

        @Override
        public boolean equals(Object o) {
            return Equals.typeEquals(this, o);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public String toString() {
            if (literal == null) {
                return "Float";
            } else {
                // return "Float#" + literal;
                return String.valueOf(literal);
            }
        }
    }

    final class StringType implements Value {
        public final @Nullable String literal;

        private StringType() {
            this.literal = null;
        }

        private StringType(@NotNull String literal) {
            this.literal = literal;
        }

        @Override
        public boolean equals(Object o) {
            return Equals.typeEquals(this, o);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public String toString() {
            if (literal == null) {
                return "Str";
            } else {
                // return "Str#" + literal;
                return literal;
            }
        }
    }

    final class TupleType implements Value {
        public final @NotNull /*List<Value>*/TupleValue eltTypes;

        private TupleType(@NotNull List<Value> eltTypes) {
            this.eltTypes = Tuple(eltTypes);
        }

        public Value eltType() {
            if (size() == 0) {
                throw Error.bug("访问 unit 元素类型");
            } else {
                return union(eltTypes);
            }
        }

        public Value get(Location loc, int idx) {
            return eltTypes.get(loc, idx);
        }

        public int size() {
            return eltTypes.size();
        }

        @Override
        public boolean equals(Object o) {
            return Equals.typeEquals(this, o);
        }

        @Override
        public int hashCode() {
            return eltTypes.values.hashCode();
        }

        @Override
        public String toString() {
            return stringfy(this);
        }
    }

    final class VectorType implements Value {
        public final @NotNull Value eltType;
        public @Nullable /*List<Value>*/VectorValue positional;
        public int size;
        public static final int UNKNOWN_SZ = -1;

        // for declare, e.g. Int[]
        private VectorType(@NotNull Value eltType) {
            // assert eltType != UNKNOWN; // todo
            this.positional = null;
            this.size = UNKNOWN_SZ;
            this.eltType = eltType;
        }

        // for literal, e.g. [], [1, 3.14]
        private VectorType(@NotNull Value eltType, @NotNull List<Value> types) {
            this.eltType = eltType;
            this.positional = Vector(types);
            this.size = types.size();
        }

        public boolean isEmptyLiteral() {
            // let a: Any[] = []
            // let b: Int[] = a
            // Vectors.append(a, 3.14)
            // IO.println(b) // [3.14]
            // 所以 空数组字面量的类型不能是 Any[] 只能是 UNKNOWN[]

            // let a: Int[][] = [[]]
            // let b: Float[][] = a
            // b[0] = [3.14]
            // 必须递归判断, 否则上面的 case 有问题
            // return positional != null && size == 0 && eltType == UNKNOWN;
            if (positional == null) {
                return false;
            } else if (eltType == UNKNOWN) {
                // []
                return size == 0;
            } else if (eltType instanceof VectorType) {
                // [[]], [[[]]], [[],[]]
                return ((VectorType) eltType).isEmptyLiteral();
            } else {
                return false;
            }
        }

        public Value get(Location loc, int idx) {
            if (positional == null) {
                return eltType;
            } else {
                //noinspection UnnecessaryLocalVariable
                Value expected = eltType;

                if (positional.indexOutOfBounds(idx)) {
                    // todo 疑似数组越界!!!
                    return eltType;
                } else {
                    Value actual = positional.get(loc, idx);
                    return TypeChecker.select(loc, expected, actual);
                }
            }
        }

        @SuppressWarnings("unused")
        public void set(Location loc, @NotNull Value idx, @NotNull Value type) {
            assert isInt(idx) && !isLiteral(idx);
//            if (!isInt(idx)) {
//                throw Error.type(loc, "访问 vector 下标必须为数字, 实际是 " + idx);
//            }
//            if (!subtype(type, eltType)) {
//                throw Error.type(loc, "vector 期望元素类型 " + eltType + ", 实际是 " + type);
//            }
            // eltType = union(eltType, type);
            // 更新之后干掉 positional
            this.positional = null;
        }

        public void set(Location loc, int idx, @NotNull Value type) {
//            if (!subtype(type, eltType)) {
//                throw Error.type(loc, "vector 期望元素类型 " + eltType + ", 实际是 " + type);
//            }

            // (t: Float[]) => t[0] = 3.14
            // assert positional != null; // 不能断言 positional 不为 null
            if (positional != null) {
                if (positional.indexOutOfBounds(idx)) {
                    // todo 疑似数组越界!!!
                } else {
                    positional.set(loc, idx, type);
                }
            }
            // eltType = union(positional);
        }

        public void add(Location loc, @NotNull Value type) {
//            if (!subtype(type, eltType)) {
//                throw Error.type(loc, "vector 期望元素类型 " + eltType + ", 实际是 " + type);
//            }

            if (positional != null) {
                positional.add(loc, type);
            }
            size++;
        }

        public int size() {
            assert positional == null || size == positional.size();
            return size;
        }

        @Override
        public int hashCode() {
            // todo test 递归 vector type 的 hashcode
            return eltType.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return Equals.typeEquals(this, o);
        }

        @Override
        public String toString() {
            return stringfy(this);
        }
    }

    final class RecordType implements Value {
        public final @Nullable String name;
        public final @NotNull Node def; // definition: RecordDef | RecordLiteral
        // 这里的 props 的值是被 interpDeclare 过的 Value
        // 只有 type default mut, 没有 value
        public final @NotNull Scope props; // fields
        public final @NotNull Scope env;

        private RecordType(@Nullable String name,
                           @NotNull Node def,
                           @NotNull Scope props,
                           @NotNull Scope env) {
            this.name = name;
            this.def = def;
            this.props = props;
            this.env = env;
        }

        @SuppressWarnings("AliControlFlowStatementWithoutBraces")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return def.equals(((RecordType) o).def);
            // return Equals.typeEquals(this, o);
        }

        @Override
        public int hashCode() {
            return def.hashCode();
        }

        @Override
        public String toString() {
            return stringfy(this);
        }
    }

    class Arrow implements Value {
        // public Map<TupleType, Value> arrows = new HashMap<>();
        public TupleType paramsType;
        public Value retType;

        @Override
        public String toString() {
            if (paramsType != null && retType != null) {
                List<Value> vals = paramsType.eltTypes.values;
                return "(" + Helper.join(vals, ",") + ") "  + ID_RETURN + " " + retType;
            } else {
                return "Fun";
            }
        }
    }

    final class FunType extends Arrow {
        public final @NotNull FunDef def;
        public final @NotNull Scope props;
        public final @NotNull Scope env;
        @Nullable Runnable fillParamRetType;

        private FunType(@NotNull FunDef def, @NotNull Scope props, @NotNull Scope env) {
            this.def = def;
            this.props = props;
            this.env = env;
        }

        @SuppressWarnings("AliControlFlowStatementWithoutBraces")
        @Override
        public boolean equals(Object o) {
            // return Equals.typeEquals(this, o);
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return def.equals(((FunType) o).def);
        }

        @Override
        public int hashCode() {
            return def.hashCode();
        }

        @Override
        public String toString() {
            return stringfy(this);
        }
    }

    // https://en.wikipedia.org/wiki/Union_type
    // union 类型支持的值 无需知道实际类型就能支持的操作：赋值，判等
    // sum type
    final class UnionType implements Value {
        public final Set<Value> types = createValueSet();

        static Set<Value> createValueSet() {
            //  不用默认 equals,
            // return new HashSet<>();
            return new TreeSet<>(unionComp);
        }

        private UnionType(@NotNull Value ...types) {
            for (Value type : types) {
                assert type != UNKNOWN;
                add(type);
            }
        }

        void add(@NotNull Value value) {
            if (value instanceof UnionType) {
                types.addAll(((UnionType) value).types);
            } else {
                types.add(value);
            }
        }

        private static Value union2(@NotNull Value u, @NotNull Value v) {
            assert u != UNKNOWN;
            assert v != UNKNOWN;

            // 1|2|"a"|"b" => Int|Str
            if (isPrimLiteral(u)) {
                u = Literal.clear(u);
            }
            if (isPrimLiteral(v)) {
                v = Literal.clear(v);
            }

            boolean containsLit = isLiteral(u) || isLiteral(v);
            if (u == v) { return u; }
            // if (u.equals(v)) { return u; } // 这里不处理, UnionType 的 hashset 的 eq 方法会全局处理
            else if (u == ANY || v == ANY) {
                return ANY;
            } else if (!containsLit && subtype(u, v)) {
                // return v;
                // 特殊处理
                if (Type.isEmptyVectorLiteral(v)) {
                    // let a:Int[][] = [[], []] 特殊处理空数组, clear 之后就没 positional 了
                    // todo 或者 clear 不清除 positional
                    return v;
                } else {
                    // 3 | 4
                    return Literal.clear(v);
                }
            } else if (!containsLit && subtype(v, u)) {
                // return u;
                if (Type.isEmptyVectorLiteral(u)) {
                    return u;
                } else {
                    return Literal.clear(u);
                }
            } else {
                return new UnionType(u, v);
            }
        }

        static Value union(Value ...types) {
            /*
            if (types.length == 0) {
                throw Error.bug("union empty");
            } else {
                UnionType u = new UnionType();
                for (Value v : types) {
                    u.add(v);
                }
                if (u.size() == 1) {
                    return u.first();
                } else if(u.types.contains(ANY)){
                    return ANY;
                } else {
                    return u;
                }
            }
            */

            if (types.length == 0) {
                throw Error.bug("union empty");
            } else {
                Value t = types[0];
                for (int i = 1; i < types.length; i++) {
                    t = union2(t, types[i]);
                }
                if (t instanceof UnionType) {
                    UnionType u = (UnionType) t;
                    if (u.size() == 1) {
                        return u.first();
                    }
                }
                return t;
            }
        }

        static Value union(Collection<Value> types) {
            /*
            if (types.isEmpty()) {
                throw Error.bug("union empty");
            } else {
                UnionType u = new UnionType();
                for (Value v : types) {
                    u.add(v);
                }
                if (u.size() == 1) {
                    return u.first();
                } else if(u.types.contains(ANY)){
                    return ANY;
                } else {
                    return u;
                }
            }
            */

            if (types.isEmpty()) {
                throw Error.bug("union empty");
            } else {
                Iterator<Value> iter = types.iterator();
                Value t = iter.next();
                while (iter.hasNext()) {
                    t = union2(t, iter.next());
                }
                if (t instanceof UnionType) {
                    UnionType u = (UnionType) t;
                    if (u.size() == 1) {
                        return u.first();
                    }
                }
                return t;
            }
        }

        private Value first() {
            return types.iterator().next();
        }

        int size() {
            return types.size();
        }

        @Override
        public boolean equals(Object o) {
            return Equals.typeEquals(this, o);
        }

        // hashset 的 hashcode 实现是 elt 的 hashcode 加起来, 顺序无影响
        @Override
        public int hashCode() {
            // todo test 构造一个递归的 union...
            return types.hashCode();
        }

        @Override
        public String toString() {
            return stringfy(this);
        }
    }

    final class ModuleType implements Value {
        public final Module module;
        public final Scope scope;

        private ModuleType(Module module, Scope scope) {
            this.module = module;
            this.scope = scope;
        }

        @Override
        public String toString() {
            if (module.name == null) {
                return "Module";
            } else {
                return "Module<" + module.name + ">";
            }
        }
    }
}
