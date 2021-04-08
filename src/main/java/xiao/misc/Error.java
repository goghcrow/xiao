package xiao.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.joining;

/**
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public class Error extends RuntimeException {

    @Nullable
    public final Location loc;
    public Error(@Nullable Location loc, String msg) {
        super(msg);
        this.loc = loc;
    }

    @Override
    public String toString() {
        if (loc == null || loc == Location.None) {
            return getMessage();
        } else {
            String codeSpan = loc.input.substring(
                    max(0, loc.idxBegin),
                    min(loc.input.length(), loc.idxEnd)
            );
            return getMessage() + " [" + loc + "] " + "\n" + codeSpan + "\n";
        }
    }

    public static class Mixed extends Error {
        List<Error> errors;
        public Mixed(List<Error> errors) {
            super(errors.isEmpty() ? null : errors.get(0).loc, errors.isEmpty() ? "" : errors.get(0).getMessage());
            this.errors = errors;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Error e : errors) {
                sb.append(e.toString());
                sb.append("\n\n");
            }
            return sb.toString();
        }
    }

    public static class Lexer extends Error {
        public Lexer(Location loc, String msg) {
            super(loc, msg);
        }
    }

    public static class Syntax extends Error {
        public Syntax(Location loc, String msg) {
            super(loc, msg);
        }
    }

    public static class Type extends Error {
        public Type(Location loc, String msg) {
            super(loc, msg);
        }
    }

    public static class DiagnosisSummary extends Type {
        final List<Diagnosis> diagnosisList;

        public DiagnosisSummary(List<Diagnosis> errLst) {
            super(Location.None, msg(errLst));
            this.diagnosisList = errLst;
        }

        static String msg(List<Diagnosis> errLst) {
            return errLst.stream().map(Diagnosis::toString).collect(joining("\n\n"));
        }
    }

    public static class Runtime extends Error {
        public Runtime(Location loc, String msg) {
            super(loc, msg);
        }
    }

    public static Error mixed(@NotNull List<Error> errors) {
        return new Mixed(errors);
    }

    public static Error lexer(@NotNull Location loc, @NotNull String msg) {
        return new Lexer(loc, msg);
    }

    public static Error syntax(@NotNull Location loc, @NotNull String msg) {
        return new Syntax(loc, msg);
    }

    public static Error type(@NotNull Location loc, @NotNull String msg) {
        return new Type(loc, msg);
    }

    public static DiagnosisSummary diagnosisSummary(List<Diagnosis> errLst) {
        return new DiagnosisSummary(errLst);
    }

    public static Error runtime(@NotNull Location loc,@NotNull String msg) {
        return new Runtime(loc, msg);
    }

    public static Error bug(@NotNull String msg) {
        return new Error(Location.None, msg);
    }

    public static Error bug(@NotNull Location loc) {
        return new Error(loc, "🐞");
    }

    public static Error bug(@NotNull Location loc, @NotNull String msg) {
        return new Error(loc, "🐞 " + msg);
    }

    public static Error todo(@NotNull Location loc, @NotNull String msg) {
        return new Error(loc, msg + " " + loc);
    }
}
