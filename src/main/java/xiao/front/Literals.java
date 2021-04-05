package xiao.front;

import org.jetbrains.annotations.NotNull;
import xiao.misc.Error;
import xiao.misc.Location;

import static xiao.front.Ast.FloatNum;
import static xiao.front.Ast.IntNum;

/**
 * @author chuxiaofeng
 */
public class Literals {

    public static IntNum parseIntNum(Location loc, @NotNull String str) {
        try {
            return Literals.parseInt1(loc, str);
        } catch (NumberFormatException ignored) {
            throw Error.lexer(loc, "😓整数格式不对: " + str);
        }
    }

    static IntNum parseInt1(Location loc, @NotNull String str) {
        int base, sign;
        if (str.startsWith("+")) {
            sign = 1;
            str = str.substring(1);
        } else if (str.startsWith("-")) {
            sign = -1;
            str = str.substring(1);
        } else {
            sign = 1;
        }

        if (str.startsWith("0b")) {
            base = 2;
            str = str.substring(2);
        } else if (str.startsWith("0x")) {
            base = 16;
            str = str.substring(2);
        } else if (str.startsWith("0o")) {
            base = 8;
            str = str.substring(2);
        } else {
            base = 10;
        }

        long val = Long.parseLong(str, base);
        if (sign == -1) {
            val = -val;
        }
        return new IntNum(loc, str, val, base);
    }

    public static FloatNum parseFloatNum(Location loc, @NotNull String str) {
        try {
            return new FloatNum(loc, str, Double.parseDouble(str));
        } catch (NumberFormatException ignored) {
            throw Error.lexer(loc, "😓浮点数格式不对: " + str);
        }
    }

    // TODO 这里优化下，一团浆糊
    public static String unescapeStr(@NotNull Location loc, String s, char quote) {
        // char quote = s.charAt(0);
        s = s.substring(1, s.length() - 1);

        char[] a = s.toCharArray(), ss = new char[a.length];
        int l = a.length, cnt = 0;

        for (int i = 0; i < l; i++) {
            char c = a[i];
            if (c == quote && i + 1 < l) {
                // """"   ''''
                char n = a[i + 1];
                if (n == quote) {
                    i++;
                    ss[cnt++] = quote;
                } else {
                    ss[cnt++] = c;
                }
            } else if (c == '\\' && i + 1 < l) {
                // \' \" \\ \/ \t \r \n \b \f
                char n = a[i + 1];
                i++;
                if (n == quote) {
                    ss[cnt++] = quote;
                } else {
                    switch (n) {
                        // case quote: ss[cnt++] = quote ;break;
                        case '\\': ss[cnt++] = '\\';break;
                        // case '/': ss[cnt++] = '/';break;
                        case 't': ss[cnt++] = '\t';break;
                        case 'r': ss[cnt++] = '\r';break;
                        case 'n': ss[cnt++] = '\n';break;
                        case 'b': ss[cnt++] = '\b';break;
                        case 'f': ss[cnt++] = '\f';break;
                        case 'u':
                            ss[cnt++] = parseUnicodeEscape(loc, a[i + 1], a[i + 2], a[i + 3], a[i + 4]);
                            i += 4;
                            break;
                        default:
                            i--;
                            ss[cnt++] = c;
                    }
                }
            } else {
                ss[cnt++] = c;
            }
        }
        return new String(ss, 0, cnt);
    }

    static char parseUnicodeEscape(Location loc, char c1, char c2, char c3, char c4) {
        // return (char) Integer.parseInt(String.valueOf(c1) + c2 + c3 + c4, 16);
        int i = parseHexDigit(loc, c1) << 12 | parseHexDigit(loc, c2) << 8 | parseHexDigit(loc, c3) << 4 | parseHexDigit(loc, c4);
        if (Double.isInfinite(i) || Double.isNaN(i)) {
            throw Error.syntax(loc, "有问题的\\u Unicode");
        }
        return (char) i;
    }

    static int parseHexDigit(Location loc, char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'F') {
            return c + 10 - 'A';
        } else if (c >= 'a' && c <= 'f') {
            return c + 10 - 'a';
        }
        throw Error.syntax(loc, "有问题的\\u Unicode");
    }
    
}
