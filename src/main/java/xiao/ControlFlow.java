package xiao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiao.misc.Location;

import java.util.List;

import static xiao.CallStack.Frame;

/**
 * @author chuxiaofeng
 */
class ControlFlow extends RuntimeException {

    protected ControlFlow() {
        super(null, null, true, false);
    }

    static Continue Continue() {
        return new Continue();
    }

    static Break Break() {
        return new Break();
    }

    static Return Return(Value val) {
        return new Return(val);
    }

    static Throw Throw(Location loc, List<Frame> stack, Value value) {
        return new Throw(loc, stack, value);
    }

    static class Continue extends ControlFlow {
    }

    static class Break extends ControlFlow {
    }

    static class Return extends ControlFlow {
        @Nullable
        public final Value val;

        private Return(@Nullable Value val) {
            this.val = val;
        }
    }

    static class Throw extends ControlFlow {
        @NotNull
        public final Location loc;
        @NotNull
        public final List<Frame> stack;
        @NotNull
        public final Value value;

        private Throw(@NotNull Location loc, @NotNull List<Frame> stack, @NotNull Value value) {
            this.loc = loc;
            this.stack = stack;
            this.value = value;
        }
    }
}
