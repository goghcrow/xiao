package xiao;

import org.jetbrains.annotations.NotNull;
import xiao.misc.Error;
import xiao.misc.Helper;
import xiao.misc.Location;

import java.util.Deque;
import java.util.LinkedList;

import static xiao.front.Ast.Call;

/**
 * @author chuxiaofeng
 */
class CallStack {

    static class Frame {
        final Call call;
        final Value callee;
        Object args; // List<Value> | PrimArgs
        final Scope s;

        Frame(@NotNull Call call, @NotNull Value callee, @NotNull Scope s) {
            this.call = call;
            this.callee = callee;
            this.s = s;
        }

        @Override
        public String toString() {
            return call.loc.toString();
        }
    }

    final Deque<Frame> frames = new LinkedList<>();
    int threshold;
    int cnt = 0;

    CallStack(int threshold) {
        this.threshold = threshold;
    }

    void push(@NotNull Frame f) {
        frames.push(f);
        if (++cnt > threshold) {
            Helper.log(toString() + "\n");
            throw Error.runtime(Location.None, "StackOverflow");
        }
    }

    void pop() {
        cnt--;
        frames.pop();
    }

    Frame peek() {
        return frames.peek();
    }

    @Override
    public String toString() {
        return Helper.join(frames, "\n");
    }
}
