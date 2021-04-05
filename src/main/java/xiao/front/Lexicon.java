package xiao.front;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiao.misc.Error;
import xiao.misc.Location;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static xiao.front.Lexicon.Rule;
import static xiao.front.Token.EOF;
import static xiao.front.TokenType.*;

/**
 * Lexical Grammar
 * @author chuxiaofeng
 */
public class Lexicon implements Iterable<Rule> {

    interface Rule {
        @Nullable
        MatchResult tryMatch(String input, int offset);
    }

    static class MatchResult {
        final int offset;
        final TokenType type;
        final boolean keep;

        public MatchResult(int offset, TokenType type, boolean keep) {
            this.offset = offset;
            this.type = type;
            this.keep = keep;
        }
    }

    final Predicate<Character> isIdCompleted = ch ->
            !( (ch >= '0' && ch <= '9')
            || (ch >= 'A' && ch <= 'Z')
            || (ch >= 'a' && ch <= 'z')
            || ch == '_');
    final static String idReg = "[a-zA-Z_][a-zA-Z0-9_]*";
    final static Pattern idPtn = Pattern.compile(idReg);
    final static String symbolReg = "`[^`]+`";
    final static Pattern symbolPtn = Pattern.compile(symbolReg);
    static boolean isSymbol(@NotNull String s) { return symbolPtn.matcher(s).matches(); }
    static String retrieveSymbol(@NotNull String s) { return s.substring(1, s.length() - 1); }
    final static String operators = ":!#$%^&*+./<=>?@\\ˆ|-~";
    // 特殊处理as是因为 as 是个操作符...
    final static String operatorsReg = "(?:[\\Q:!#$%^&*+./<=>?@\\ˆ|-~\\E]+)|(?:`[^`]+`)|(?:" + AS.name + "(?![a-zA-Z0-9_]))";
    final static Pattern operatorsPtn = Pattern.compile(operatorsReg);
    static boolean isOperators(@NotNull String s) { return operatorsPtn.matcher(s).matches(); }

    // 是不是可以把动态声明的操作符直接插入 rules， 而不是单独做一个 operators rules
    final List<Rule> rules = new ArrayList<>();
    final TreeSet<StrRule> operatorsRules = new TreeSet<>();

    public Lexicon() {
        init();
    }

    void init() {
        // 不是最长匹配, 按优先级匹配, 注意顺序, e.g. 1 - 1, 应该 1,-,1  而不是 1, -1

        // 空白字符 start
        // 多行注释匹配 Stack Overflow
        // 参考: https://stackoverflow.com/questions/7509905/java-lang-stackoverflowerror-while-using-a-regex-to-parse-big-strings
        // 原来实现 "/\\*(?:.|[\\n\\r])*?\\*/" 会 Stack Overflow, 估计是 (?:.|[\\n\\r])* 这个被编译成递归调用...
        // regex("/\\*[\\s\\S]*?\\*/", BLOCK_COMMENT, false);
        rule(new RegRule("/\\*[\\s\\S]*?\\*+/", BLOCK_COMMENT, false));
        rule(new RegRule("//.*", LINE_COMMENT, false));
        rule(new RegRule("[ \\r\\t]+", WHITESPACE, false)); // 不能使用\s+, 要单独处理换行

        str(NEWLINE);           // \n
        str(SEMICOLON);         // ;
        str(COMMA);             // ,

        str(LEFT_PAREN);        // (
        str(RIGHT_PAREN);       // )
        str(LEFT_BRACKET);      // [
        str(RIGHT_BRACKET);     // ]
        str(LEFT_BRACE);        // {
        str(RIGHT_BRACE);       // }

        TokenType[] keywords = {
                PREFIX, INFIXL, INFIXR, INFIXN,
                TYPE, TYPEREC, LET, LETREC, MUT, FUN,
                RECORD, EXTENDS,
                IF, ELSE,
                FOR, DO, WHILE,
                MATCH, CASE,
                BREAK, CONTINUE, RETURN,
                MODULE, IMPORT, FROM, AS,
                ASSERT, DEBUGGER
        };
        for (TokenType t : keywords) {
            keyword(t);
        }

        TokenType[] operators = {
                // => = . 这几个个操作符优先级最高, 且不可以被 override,
                // UNARY_MINUS, // unary_minus 通过 PreParser 处理
                ARROW, ASSIGN, DOT, ARROW_BLOCK
        };
        for (TokenType t : operators) {
            primOperator(t);
        }

        // 自定义操作符 要放在 operators 之前, 注册好 operators 之后 优先级保证高于 operators
        rule((input, offset) -> {
            // 这里应该做成获取 当前作用域 的 operatorsRules
            // 目前做成进入退出作用域自动清理排好序的 operatorRule
            for (StrRule rule : operatorsRules) {
                MatchResult r = rule.tryMatch(input, offset);
                if (r != null) {
                    return r;
                }
            }
            return null;
        });

        // 注意: 这里不会匹配到 ASSIGN(=), ARROW(=>), DOT(.)，因为 KEYWORD 已经注册过
        regex(OPERATORS, operatorsReg);

        regex(NAME, idReg); // identifier

        // 移除数字前的 [+-]?, 因为也没有, 因为木有使用最长路径来匹配, +- 被优先匹配成操作符了
        // 如果优先匹配数字的话, 1-1, 会被分成 1,-1, 需要修一遍 tokseq
        // so, 不支持 + 数字, - 数字 处理成一元操作符
        regex(FLOAT, "(?:0|[1-9][0-9]*)(?:[.][0-9]+)+(?:[eE][-+]?[0-9]+)?");
        regex(FLOAT, "(?:0|[1-9][0-9]*)(?:[.][0-9]+)?(?:[eE][-+]?[0-9]+)+");
        regex(INT, "0b(?:0|1[0-1]*)");
        regex(INT, "0x(?:0|[1-9a-fA-F][0-9a-fA-F]*)");
        regex(INT, "0o(?:0|[1-7][0-7]*)");
        regex(INT, "(?:0|[1-9][0-9]*)");

        // 字符串支持换行
        regex(STRING, "\"((?:[^\"\\\\]*|\\\\[\"\\\\trnbf\\/]|\\\\u[0-9a-fA-F]{4})*)\"");
        regex(STRING, "'((?:[^'\\\\]*|\\\\['\\\\trnbf\\/]|\\\\u[0-9a-fA-F]{4})*)'");
    }

    public void rule(@NotNull Rule rule) {
        rules.add(rule);
    }

    public void keyword(@NotNull TokenType type) {
        // keyword 需要匹配完整单词, 遇到不合法的 char 为止
        rule(new WordsRule(type.name, type, isIdCompleted, true));
    }

    public void primOperator(@NotNull TokenType type) {
        // 遇到不是 op 的 char 为止
        rule(new WordsRule(type.name, type, nxtChar -> operators.indexOf(nxtChar) == -1, true));
    }

    public void str(@NotNull TokenType type) {
        rule(new StrRule(type.name, type, true));
    }

    public void regex(@NotNull TokenType type, @NotNull String pattern) {
        rule(new RegRule(pattern, type, true));
    }

    public Runnable operator(@NotNull TokenType operatorType) {
        boolean idId = idPtn.matcher(operatorType.name).matches();
        StrRule rule;
        if (idId) {
            // 处理像 as 之类操作符
            // 否则 fun assertDepth(d: Int, root: Tree) 中 as会识别成操作符...
            rule = new WordsRule(operatorType.name, operatorType, isIdCompleted, true);
        } else {
            rule = new StrRule(operatorType.name, operatorType, true);
        }
        operatorsRules.add(rule);
        return () -> operatorsRules.remove(rule);
    }

    @NotNull Token match(@NotNull String input, int idxBegin, int rowBegin, int colBegin) {
        if (idxBegin >= input.length()) {
            return EOF;
        }

        for (Rule rule : rules) {
            MatchResult r = rule.tryMatch(input, idxBegin);
            if (r != null) {
                int idxEnd = r.offset;
                String matched = input.substring(idxBegin, idxEnd);
                Location loc = Location.ofStr(input, matched, idxBegin, idxEnd, rowBegin, colBegin);
                return new Token(r.type, matched, loc, r.keep);
            }
        }

        Location loc = Location.ofStr(input, "", idxBegin, input.length(), rowBegin, colBegin);
        throw Error.lexer(loc, "没有匹配到词法规则");
    }

    @NotNull Token matchKeep(@NotNull String input, @Nullable Token prev) {
        Token tok = prev;

        int idxBegin = 0;
        int rowBegin = 1;
        int colBegin = 1;

        while (true) {
            if (tok != null) {
                idxBegin = tok.loc.idxBegin + tok.lexeme.length();
                rowBegin = tok.loc.rowEnd;
                colBegin = tok.loc.colEnd;
            }

            if (idxBegin >= input.length()) {
                return Token.EOF;
            }

            tok = match(input, idxBegin, rowBegin, colBegin);
            if (tok.keep) {
                return tok;
            }
        }
    }

    @NotNull @Override
    public Iterator<Rule> iterator() {
        return rules.iterator();
    }

    static class StrRule implements Rule, Comparable<StrRule> {
        static int cnt = 0;
        final int id = ++cnt;
        final String toMatch;
        final TokenType type;
        final boolean keep;

        StrRule(@NotNull String toMatch, @NotNull TokenType type, boolean keep) {
            this.type = type;
            this.keep = keep;
            this.toMatch = toMatch;
        }

        @Override
        @Nullable
        public MatchResult tryMatch(String input, int offset) {
            if (input.regionMatches(offset, toMatch, 0, toMatch.length())) {
                return new MatchResult(toMatch.length() + offset, type, keep);
            } else {
                return null;
            }
        }

        // 如果一样, 后来加的放到前面, 用 id 来排序, 比如下面的例子, 把 - 在新作用域重载成右结合
        // infixr 7 - 会动态生成一个新的 tokenType, 并且绑定 grammar 规则
        // 所以依赖 之后 - token 的类型为新的 tokenType，所以哪怕相等，后来的也要放到前面
        // {
        //      infixr 7 -
        //      assert 1 - 2 - 3 == 2
        // }
        @Override
        public int compareTo(@NotNull StrRule o) {
            if (toMatch.equals(o.toMatch)) {
                return -Integer.compare(id, o.id);
            } else {
                return -toMatch.compareTo(o.toMatch);
            }
        }

        @Override
        public String toString() {
            return "StrRule{" +
                    "id=" + id +
                    ", toMatch='" + toMatch + '\'' +
                    ", type=" + type +
                    ", keep=" + keep +
                    '}';
        }
    }

    static class RegRule implements Rule {
        final Pattern pat;
        final TokenType type;
        final boolean keep;

        RegRule(@NotNull String pat, @NotNull TokenType type, boolean keep) {
            this.type = type;
            this.keep = keep;
            this.pat = Pattern.compile(pat);
        }

        @Override
        @Nullable
        public MatchResult tryMatch(String input, int offset) {
            java.util.regex.Matcher matcher = pat.matcher(input.substring(offset));
            if (matcher.lookingAt()) {
                return new MatchResult(matcher.end() + offset, type, keep);
            } else {
                return null;
            }
        }
    }

    // 可以用完全用正则表达, 这里做一下简化
    static class WordsRule extends StrRule {
        final Predicate<Character> isCompleted;
        WordsRule(@NotNull String toMatch,
                  @NotNull TokenType type,
                  @NotNull Predicate<Character> isCompleted,
                  boolean keep) {
            super(toMatch, type, keep);
            this.isCompleted = isCompleted;
        }

        @Override
        @Nullable
        public MatchResult tryMatch(String input, int offset) {
            MatchResult r = super.tryMatch(input, offset);
            if (r == null) {
                return null;
            } else {
                int idxEnd = r.offset;
                if (idxEnd == -1 || idxEnd >= input.length()) {
                    return null;
                }
                char nextChar = input.charAt(idxEnd);
                if (isCompleted.test(nextChar)) {
                    return r;
                } else {
                    return null;
                }
            }
        }
    }
}