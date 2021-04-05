package xiao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiao.misc.Error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static xiao.Constant.*;
import static xiao.Copy.copyType;

/**
 * 目前多种使用场景
 * 可以加入 type 区分
 *
 * @author chuxiaofeng
 */
public class Scope {

    // todo 内嵌的 map 可以抽象成 binding
//    static class Binding {
//        Ast.Node node;
//
//        Value value;
//        Value type;
//        Boolean mutable;
//        Value defaultValue;
//    }

    final @NotNull Map<String, Map<String, Object>> table = new LinkedHashMap<>();

    final @Nullable Scope parent;

    public Scope() {
        parent = null;
    }

    public Scope(@NotNull Scope parent) {
        this.parent = parent;
    }

    public Scope shadowCopy() {
        Scope s = new Scope();
        table.forEach((k, v) -> s.table.put(k, new LinkedHashMap<>(v)));
        return s;
    }

    public void putAll(@NotNull Scope other) {
        table.putAll(other.shadowCopy().table);
    }

    Value valueCheck(Object v) {
        if (v == null) {
            return null;
        } else if (v instanceof Value) {
            return ((Value) v);
        } else {
            throw Error.bug("v 必须是 value, v = " + v);
        }
    }

    public @Nullable Value lookup(@NotNull String name) {
        Object v = lookupProp(name, KEY_VAL);
        return valueCheck(v);
    }

    public @Nullable Value lookupLocal(@NotNull String name) {
        Object v = lookupLocalProp(name, KEY_VAL);
        return valueCheck(v);
    }

    public @Nullable Value lookupType(@NotNull String name) {
        Object v = lookupProp(name, KEY_TYPE);
        return valueCheck(v);
    }

    public @Nullable Value lookupLocalType(@NotNull String name) {
        Object v = lookupLocalProp(name, KEY_TYPE);
        return valueCheck(v);
    }

    // 需要复制后使用!!!
    public @Nullable Value lookupLocalDefault(@NotNull String name) {
        Object v = lookupLocalProp(name, KEY_DEFAULT);
        return valueCheck(v);
    }

    // interp 阶段的 prop 的 value 都是 Value
    public @Nullable Object lookupProp(@NotNull String name, @NotNull String key) {
        Object v = lookupLocalProp(name, key);
        if (v != null) {
            return v;
        } else if (parent != null) {
            return parent.lookupProp(name, key);
        } else {
            return null;
        }
    }

    // interp 阶段的 prop 的 value 都是 Value
    public @Nullable Object lookupLocalProp(@NotNull String name, @NotNull String key) {
        Map<String, Object> props = table.get(name);
        if (props != null) {
            return props.get(key);
        } else {
            return null;
        }
    }

    public @Nullable Map<String, Object> lookupAllProps(@NotNull String name) {
        return table.get(name);
    }

    public void putAllProps(@NotNull String name, @NotNull Map<String, Object> props) {
        table.put(name, props);
    }

    public @Nullable Scope findDefinedScope(@NotNull String name) {
        Map<String, Object> props = table.get(name);
        if (props != null) {
            return this;
        } else if (parent != null) {
            return parent.findDefinedScope(name);
        } else {
            return null;
        }
    }

    public void putProps(@NotNull String name, @NotNull Map<String, Object> props) {
        table.computeIfAbsent(name, k -> new LinkedHashMap<>()).putAll(props);
    }

    public void put(@NotNull String name, @NotNull String key, Object value) {
        table.computeIfAbsent(name, k -> new LinkedHashMap<>()).put(key, value);
    }

    public void putValue(@NotNull String name, @NotNull Value value) {
        put(name, KEY_VAL, value);
    }

    public void putType(@NotNull String name, @NotNull Value type) {
        put(name, KEY_TYPE, type);
    }

    public Set<String> keySet() {
        return table.keySet();
    }

    public void forEach(BiConsumer<String, Map<String, Object>> action) {
        table.forEach(action);
    }

    @Override
    public String toString() {
        return "Scope{table= " + table + '}';
    }
}
