package xiao.misc;

import java.util.ArrayList;
import java.util.List;

import static xiao.misc.Helper.join;

/**
 * assert 实现灵感也来自 Groovy 的 PowerAssert
 * 不过 Groovy 是在编译器改写代码，把表达式展开成最小颗粒度，然后加一堆 if 实现的
 * 这里的实现是执行期间临时 hook 了解释器, 每个 node 的解释都进行求值保存起来
 * 每个node 还特意 加了 oploc, 用来精确定位每个 node assert 值显示列数
 *
 * 【断言渲染方法部分来自 Groovy】
 * @author groovy
 *
 */
public class AssertRender {

    public static String render(Location loc, String src, AssertValueRecord recorder) {
        return new AssertRender(loc, src, recorder).render();
    }

    final String src;

    final AssertValueRecord rec;

    final List<StringBuilder> lines = new ArrayList<>();

    // startColumns.get(i) is the first non-empty column of lines.get(i)
    final List<Integer> startColumns = new ArrayList<>();

    AssertRender(Location loc, String src, AssertValueRecord rec) {
        if (src.contains("\n")) {
            throw Error.runtime(loc, "assert 语句不能换行");
        }
        this.src = src;
        this.rec = rec;
    }

    String render() {
        renderAssertExpr();
        sortValues();
        renderValues();
        return linesToString();
    }

    void renderAssertExpr() {
        lines.add(new StringBuilder(src));
        startColumns.add(0);

        lines.add(new StringBuilder()); // empty line
        startColumns.add(0);
    }

    void sortValues() {
        // it's important to use a stable sort here, otherwise
        // renderValues() will skip the wrong values
        rec.vals.sort((v1, v2) -> v2.col - v1.col);
    }

    void renderValues() {
        List<AssertValueRecord.Val> vals = rec.vals;
        int valSz = vals.size();

        nextValue:
        for (int i = 0; i < valSz; i++) {
            AssertValueRecord.Val value = vals.get(i);
            int startColumn = value.col;
            if (startColumn < 1) {
                continue; // skip values with unknown source position
            }

            // if multiple values are associated with the same column, only
            // render the value which was recorded last (i.e. the value
            // corresponding to the outermost expression)
            AssertValueRecord.Val next = i + 1 < valSz ? vals.get(i + 1) : null;
            if (next != null && next.col == startColumn) {
                continue;
            }

            String str = value.v.toString();
            if (str == null) {
                continue; // null signals the value shouldn't be rendered
            }

            String[] strs = str.split("\r\n|\r|\n");
            int endColumn = strs.length == 1 ?
                    startColumn + str.length() : // exclusive
                    Integer.MAX_VALUE; // multi-line strings are always placed on new lines

            for (int j = 1; j < lines.size(); j++) {
                if (endColumn < startColumns.get(j)) {
                    placeString(lines.get(j), str, startColumn);
                    startColumns.set(j, startColumn);
                    continue nextValue;
                } else {
                    placeString(lines.get(j), "|", startColumn);
                    if (j > 1) // make sure that no values are ever placed on empty line
                    {
                        startColumns.set(j, startColumn + 1); // + 1: no whitespace required between end of value and "|"
                    }
                }
            }

            // value could not be placed on existing lines, so place it on new line(s)
            for (String s : strs) {
                StringBuilder newLine = new StringBuilder();
                lines.add(newLine);
                placeString(newLine, s, startColumn);
                startColumns.add(startColumn);
            }
        }
    }

    String linesToString() {
        return join(lines, "\n");
    }

    static void placeString(StringBuilder line, String str, int column) {
        while (line.length() < column) {
            line.append(' ');
        }
        line.replace(column - 1, column - 1 + str.length(), str);
    }
}
