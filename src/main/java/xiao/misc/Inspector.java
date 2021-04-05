package xiao.misc;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chuxiaofeng
 */
class Inspector {

    public static String inspect(@NotNull Location loc, @NotNull String msg) {
        if (loc == Location.None) {
            return msg;
        }

        String[] lines = loc.input.split("\\r?\\n");
        StringBuilder buf = new StringBuilder();
        int max = 3;

        int lineNumWidth = (Math.max(loc.rowEnd + max, lines.length) + "").length();
        for (int i = max; i > 0; i--) {
            if (loc.rowBegin > i) {
                buf.append(inspectLine(lines, loc.rowBegin - i, lineNumWidth));
            }
        }
        if (loc.rowBegin == loc.rowEnd || (loc.rowBegin + 1 == loc.rowEnd && loc.colEnd == 1)) {
            buf.append(highlightLine(msg, lines, loc.rowBegin, loc.colBegin, loc.colEnd, lineNumWidth));
        } else {
            for (int i = loc.rowBegin; i < loc.rowEnd + 1; i++) {
                Matcher m = NON_SPACE.matcher(lines[i - 1]);
                buf.append(highlightLine(i == loc.rowEnd ? msg : null, lines, i, m.find() ? m.start() + 1 : 1, lines[i - 1].length() + 1, lineNumWidth));
            }
        }
        for (int i = 1; i < max + 1; i++) {
            if (loc.rowEnd + i <= lines.length) {
                buf.append(inspectLine(lines, loc.rowEnd + i, lineNumWidth));
            }
        }
        return buf.toString();
    }

    static String repeatSP(int n) {
        return new String(new char[n]).replace("\0", " ");
    }

    static String inspectLine(String[] lines, int rowBegin, int lineNumWidth) {
//            return String.format("\n%1$" + lineNumWidth + "s | %s", rowBegin, lines[rowBegin - 1]);
        String prefixSp = repeatSP(lineNumWidth - (rowBegin + "").length());
        return "\n" + prefixSp + rowBegin + " | " + lines[rowBegin - 1];
    }

    static String highlightLine(String msg, String[] lines, int rowBegin, int colBegin, int colEnd, int lineNumWidth) {
        String prefixSp = repeatSP(lineNumWidth - (rowBegin + "").length());
        String fstPrefix = prefixSp + rowBegin + " | ";
        String prefix = repeatSP((prefixSp + rowBegin).length()) + " | ";
        String nSpaces = repeatSP(colBegin - 1);
        prefix += nSpaces;

        StringBuilder buf = new StringBuilder("\n");
        buf.append(fstPrefix);

        String line = lines[rowBegin - 1];
        buf.append(line);
        if (buf.charAt(buf.length() - 1) != '\n') {
            buf.append("\n");
        }

        buf.append(prefix);
        for (int i = 0; i < colEnd - colBegin; i++) {
            buf.append("^");
        }
        // buf.append("\n");

        // buf.append(prefix);
        if (msg != null) {
            buf.append(" ");
            buf.append(msg);
        }

        return buf.toString();
    }

    static Pattern NON_SPACE = Pattern.compile("\\S");

}
