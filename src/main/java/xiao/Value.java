package xiao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiao.misc.Error;
import xiao.misc.Location;

import java.util.List;
import java.util.Objects;

import static xiao.front.Ast.*;
import static xiao.Constant.ID_FALSE;
import static xiao.Constant.ID_TRUE;
import static xiao.ToString.stringfy;
import static xiao.Type.RecordType;

/**
 * @author chuxiaofeng
 */
public interface Value {

    // 用作 define assign 动作或者 空 block 规约结果
    // 空 block 比如 没有 else 的 if, 比如 空 body 的函数等
    // Synthetic 不能主动声明
    Value VOID = new VoidValue();

    BoolValue TRUE = new BoolValue(true);
    BoolValue FALSE = new BoolValue(false);

    static IntValue     Int(long value)                 { return new IntValue(value);       }
    static FloatValue   Float(double value)             { return new FloatValue(value);     }
    static StringValue  Str(String value)               { return new StringValue(value);    }
    static TupleValue   Tuple(List<Value> values)       { return new TupleValue(values);    }
    static VectorValue  Vector(List<Value> values)      { return new VectorValue(values);   }
    static ModuleValue  ModuleValue(Module m, Scope s)  { return new ModuleValue(m, s);     }

    static RecordValue Record(@Nullable String name, RecordType type, Scope props) {
        return new RecordValue(name, type, props);
    }

    static Closure Closure(FunDef fun, Scope props, Scope env) {
        return new Closure(fun, props, env);
    }

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    // Synthetic
    final class VoidValue implements Value {
        private VoidValue() {}

        @Override
        public String toString() {
            return "Void";
        }
    }

    final class BoolValue implements Value {
        public final boolean value;

        private BoolValue(boolean value) {
            this.value = value;
        }

        public BoolValue not() {
            return value ? FALSE : TRUE;
        }

        public String toString() {
            return value ? ID_TRUE : ID_FALSE;
        }
    }

    final class IntValue implements Value {
        public final long value;

        private IntValue(long value) {
            this.value = value;
        }

        public int intValue() {
            return Math.toIntExact(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value == ((IntValue) o).value;
        }

        @Override
        public int hashCode() {
            return (int) (value ^ (value >>> 32));
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }
    }

    final class FloatValue implements Value {
        public final double value;

        private FloatValue(double value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return Double.compare(((FloatValue) o).value, value) == 0;
        }

        @Override
        public int hashCode() {
            long temp = Double.doubleToLongBits(value);
            return (int) (temp ^ (temp >>> 32));
        }

        @Override
        public String toString() {
            return Double.toString(value);
        }
    }

    final class StringValue implements Value {
        public final @NotNull String value;

        private StringValue(@NotNull String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value.equals(((StringValue) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return '"' + value + '"';
        }
    }

    // tuple 是 immutable value type
    final class TupleValue implements Value {
        public final @NotNull List<Value> values;

        private TupleValue(@NotNull List<Value> values) {
            this.values = values;
        }

        int idx(Location loc, int idx) {
            int idx1 = idx;
            if (idx < 0) {
                idx1 = values.size() + idx;
            }
            if (idx1 >= values.size()) {
                throw Error.type(loc, "tuple[" + size() + "] 下标" + idx + "越界了😶");
            }
            return idx1;
        }

        public @NotNull Value get(Location loc, int idx) {
            return values.get(idx(loc, idx));
        }

        public int size() {
            return values.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            return o instanceof TupleValue && values.equals(((TupleValue) o).values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(values);
        }

        @Override
        public String toString() {
            return stringfy(this);
        }
    }

    // array
    final class VectorValue implements Value {
        public final @NotNull List<Value> values;

        private VectorValue(@NotNull List<Value> values) {
            this.values = values;
        }

        boolean indexOutOfBounds(int idx) {
            int idx1 = idx;
            if (idx < 0) {
                idx1 = values.size() + idx;
            }
            return idx1 < 0 || idx1 >= values.size();
        }

        int idx(Location loc, int idx) {
            int idx1 = idx;
            if (idx < 0) {
                idx1 = values.size() + idx;
            }
            if (idx1 < 0 || idx1 >= values.size()) {
                throw Error.type(loc, "vector[" + size() + "] 下标" + idx + "越界了😶");
            } else {
                return idx1;
            }
        }

        public @NotNull Value get(Location loc, int idx) {
            return values.get(idx(loc, idx));
        }

        public void set(Location loc, int idx, @NotNull Value value) {
            idx = idx(loc, idx);
            values.set(idx, value);
        }

        public void add(Location loc, @NotNull Value value) {
            values.add(value);
        }

        public int size() {
            return values.size();
        }

        @Override
        public String toString() {
            return stringfy(this);
        }
    }

    // struct
    final class RecordValue implements Value {
        // 不能把名字去掉, 否则不方便声明递归类型
        public final @Nullable String name;
        public final @NotNull RecordType type;
        /**
         * name => [ value=>, type=>, default=>, mutable=> ...]
         */
        public final @NotNull Scope props;

        private RecordValue(@Nullable String name, @NotNull RecordType type, @NotNull Scope props) {
            this.name = name;
            this.type = type;
            this.props = props;
        }

        public boolean contains(@NotNull String name) {
            return props.lookup(name) != null;
        }

        @Override
        public String toString() {
            return stringfy(this);
        }
    }

    final class Closure implements Value {
        public final @NotNull FunDef fun;
        /**
         * declare
         * name => [ value=>, type=>, default=>, mutable=> ...]
         */
        public final @NotNull Scope props;
        public final @NotNull Scope env;

        private Closure(@NotNull FunDef fun, @NotNull Scope props, @NotNull Scope env) {
            this.fun = fun;
            this.props = props;
            this.env = env;
        }

        @Override
        public String toString() {
            return fun.toString();
        }
    }

    // Primitive Function
    final class PrimFun implements Value {
        public final @NotNull String name;
        public final Arity arity;
        public final Apply apply;

        protected PrimFun(@NotNull String name, @NotNull Arity arity, Apply apply) {
            this.name = name;
            this.arity = arity;
            this.apply = apply;
        }

        public Value apply(@NotNull Location loc, @NotNull PrimArgs args) {
            return apply.apply(loc, args);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    final class ModuleValue implements Value {
        public final Module module;
        public final Scope scope;

        private ModuleValue(@NotNull Module module, @NotNull Scope scope) {
            this.module = module;
            this.scope = scope;
        }

        @Override
        public String toString() {
            if (module.name == null) {
                return "Module";
            } else {
                return "Module " + module.name;
            }
        }
    }

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    interface Apply {
        Value apply(@NotNull Location loc, @NotNull PrimArgs args);
    }

    // 支持 PrimFun 参数惰性求值, 用来支持 || && 短路
    class PrimArgs {
        private final Scope env;
        private final Evaluator<Scope, Value> eval;
        private final List<Node> args;
        private final Value[] cache;

        protected PrimArgs(Evaluator<Scope, Value> eval, Scope env, List<Node> args) {
            this.eval = eval;
            this.env = env;
            this.args = args;
            this.cache = new Value[args.size()];
        }

        // lazy 为了处理 || && 等短路的逻辑
        public Value get(Location loc, int i) {
            Value arg = cache[i];
            if (arg == null) {
                arg = this.eval.eval(args.get(i), env);
                cache[i] = arg;
            }
            return arg;
        }

        public Value[] args(Location loc) {
            int sz = size();
            for (int i = 0; i < sz; i++) {
                get(loc, i);
            }
            return cache;
        }

        public int size() {
            return args.size();
        }
    }

    class CheckedPrimArgs extends PrimArgs {
        protected CheckedPrimArgs(Evaluator<Scope, Value> eval, Scope env, List<Node> args) {
            super(eval, env, args);
        }

        @Override
        public Value get(Location loc, int i) {
            Value v = super.get(loc, i);
            Voids.not(loc, v, "不能为 Void");
            return v;
        }
    }

    final class Arity {
        static final Arity varLen = new Arity(0, Integer.MAX_VALUE);
        static final Arity zero = exact(0);
        static final Arity one = exact(1);
        static final Arity two = exact(2);
        static final Arity three = exact(3);
        static final Arity leastOne = least(1);
        static final Arity leastTwo = least(2);

        final int from; // inclusive
        final int to; // exclusive

        private Arity(int from, int to) {
            this.from = from;
            this.to = to;
        }

        static Arity exact(int n) {
            return new Arity(n, n + 1);
        }

        static Arity range(int minInclusive, int maxExclusive) {
            return new Arity(minInclusive, maxExclusive);
        }

        static Arity least(int minInclusive) {
            return new Arity(minInclusive, Integer.MAX_VALUE);
        }

        static Arity most(int maxInclusive) {
            return new Arity(0, maxInclusive + 1);
        }

        static Arity varLen() {
            return varLen;
        }

        public boolean check(int n) {
            return n >= from && n < to;
        }

        public void validate(@NotNull Location loc, int n) throws Error.Type{
            if (!check(n)) {
                if (to - from == 1) {
                    throw Error.type(loc, "参数个数错误, 期望 " + to + ", 实际 " + n);
                } else {
                    throw Error.type(loc, "参数个数错误, 期望至少 " + from
                            + " 至多 " + (to - 1) + ", 实际" + n);
                }
            }
        }
    }
}
