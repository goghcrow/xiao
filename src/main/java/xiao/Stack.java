package xiao;

import xiao.misc.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理循环引用
 * @author chuxiaofeng
 */
class Stack {

    private final static ThreadLocal<PairStack> typeStack = ThreadLocal.withInitial(PairStack::new);

    static MonoStack valueStack() {
        return new MonoStack();
    }

    static PairStack typeStack() {
        return typeStack.get();
    }

    static void resetTypeStack() {
        typeStack.set(new PairStack());
    }

    static class MonoStack {

        private MonoStack() { }

        final List<Object> stack = new ArrayList<>();

        void push(Object val) {
            stack.add(val);
        }

        void pop(Object val) {
            Object removed = stack.remove(stack.size() - 1);
            assert val == removed;
        }

        boolean contains(Object val) {
            for (Object v : stack) {
                if (v == val) {
                    return true;
                }
            }
            return false;
        }
    }

    static class PairStack {

        final List<Pair<Value, Value>> stack = new ArrayList<>();

        void push(Value fst, Value sed) {
            stack.add(new Pair<>(fst, sed));
        }

        void pop(Value fst, Value snd) {
            Pair<Value, Value> removed = stack.remove(stack.size() - 1);
            assert fst == removed.fst;
            assert snd == removed.snd;
        }

        boolean contains(Value fst, Value sed) {
            for (Pair<Value, Value> p : stack) {
                if (p.fst == fst && p.snd == sed) {
                    return true;
                } else if (p.fst == sed && p.snd == fst) {
                    return true;
                }
            }
            return false;
        }
    }
}
