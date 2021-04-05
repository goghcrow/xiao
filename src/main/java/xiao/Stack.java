package xiao;

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

    static class Pair {
        final Value fst;
        final Value sed;

        Pair(Value fst, Value sed) {
            this.fst = fst;
            this.sed = sed;
        }
    }

    static class PairStack {

        final List<Pair> stack = new ArrayList<>();

        void push(Value fst, Value sed) {
            stack.add(new Pair(fst, sed));
        }

        void pop(Value fst, Value sed) {
            Pair removed = stack.remove(stack.size() - 1);
            assert fst == removed.fst;
            assert sed == removed.sed;
        }

        boolean contains(Value fst, Value sed) {
            for (Pair p : stack) {
                if (p.fst == fst && p.sed == sed) {
                    return true;
                } else if (p.fst == sed && p.sed == fst) {
                    return true;
                }
            }
            return false;
        }
    }
}
