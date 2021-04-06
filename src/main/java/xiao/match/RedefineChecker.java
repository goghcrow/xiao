package xiao.match;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiao.Scope;
import xiao.Type;
import xiao.Value;
import xiao.misc.Error;

import java.util.HashSet;
import java.util.Set;

import static xiao.Value.VOID;
import static xiao.front.Ast.*;

/**
 * 检查是否重复绑定一个 name
 * @author chuxiaofeng
 */
class RedefineChecker implements Matcher<Set<String>, Value> {
    final @Nullable Scope s;

    private RedefineChecker(@Nullable Scope s) {
        this.s = s;
    }

    static void check(@NotNull Node pattern) {
        new RedefineChecker(null).match(pattern, new HashSet<>());
    }

    static void check(@NotNull Node pattern, @NotNull Scope s) {
        new RedefineChecker(s).match(pattern, new HashSet<>());
    }

    @Override
    public Value match(@NotNull Id ptn, @NotNull Set<String> names) {
        if (Matchers.isWildcards(ptn)) {
            return VOID;
        }

        String name = ptn.name;
        if (s != null) {
            Value existed = s.lookupLocal(name);
            // 因为 letrec 是先定义, 之后利用checkDefine再定义, 所以排除掉 undefined 场景
            if (existed != null && !(existed instanceof Type.Undefined)) {
                throw Error.type(ptn.loc, "变量重复绑定: " + name);
            }
        }

        if (names.contains(name)) {
            throw Error.type(ptn.loc, "绑定 pattern 中标识符重复: " + name);
        } else {
            names.add(name);
        }
        return VOID;
    }

    @Override
    public Value match(@NotNull TupleLiteral ptn, @NotNull Set<String> names) {
        for (Node el : ptn.elements) {
            match(el, names);
        }
        return VOID;
    }

    @Override
    public Value match(@NotNull VectorLiteral ptn, @NotNull Set<String> names) {
        for (Node el : ptn.elements) {
            match(el, names);
        }
        return VOID;
    }

    @Override
    public Value match(@NotNull RecordLiteral ptn, @NotNull Set<String> names) {
        for (String name : ptn.map.keySet()) {
            match(ptn.node(name), names);
        }
        return VOID;
    }

    @Override
    public Value match(@NotNull Subscript ptn, @NotNull Set<String> names) {
        return VOID;
    }

    @Override
    public Value match(@NotNull Attribute ptn, @NotNull Set<String> names) {
        return VOID;
    }

    @Override
    public Value match(@NotNull IntNum ptn, @NotNull Set<String> names) {
        return VOID;
    }

    @Override
    public Value match(@NotNull FloatNum ptn, @NotNull Set<String> names) {
        return VOID;
    }

    @Override
    public Value match(@NotNull Str ptn, @NotNull Set<String> names) {
        return VOID;
    }

    @Override
    public Value missing(@NotNull Node ptn, @NotNull Set<String> names) {
        return VOID;
    }
}
