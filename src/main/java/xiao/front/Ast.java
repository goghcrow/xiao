package xiao.front;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiao.Scope;
import xiao.misc.Location;
import xiao.misc.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static xiao.Constant.*;
import static xiao.front.TokenType.Fixity;
import static xiao.misc.Location.None;

/**
 * @author chuxiaofeng
 */
public interface Ast {
    static Id wildcards(@NotNull Location loc) {
        return id(loc, WILDCARDS);
    }

    static Id id(@NotNull Location loc, @NotNull String name) {
        return new Id(loc, name);
    }

    static VectorOf vectorOf(@NotNull Location loc,
                             @NotNull Location oploc,
                             @NotNull Node elem) {
        return new VectorOf(loc, oploc, elem);
    }

    static Operator operator(@NotNull Location loc,
                             @NotNull Fixity fixity,
                             @NotNull IntNum precedence,
                             @NotNull Id operator) {
        return new Operator(loc, fixity, precedence, operator);
    }

    static Import importStmt(Location loc, List<Pair<Id, Id>> aliasMap, Node from) {
        return new Import(loc, aliasMap, from);
    }

    static Module internalModule(String name) {
        return moduleStmt(None, id(None, name), block(None, new ArrayList<>(0)));
    }

    static Module moduleStmt(Location loc, @Nullable Id name, Block block) {
        return new Module(loc, name, block);
    }

    static LitType litType(Node node) {
        return new LitType(node.loc, node);
    }

    static IntNum litIntNum(@NotNull Location loc, @NotNull String content) {
        return Literals.parseIntNum(loc, content);
    }

    static FloatNum litFloatNum(@NotNull Location loc, @NotNull String content) {
        return Literals.parseFloatNum(loc, content);
    }

    static Str litStr(@NotNull Location loc, @NotNull String content) {
        // todo parseStr
        return new Str(loc, content);
    }

    static TupleLiteral litTuple(@NotNull Location loc, @NotNull List<Node> elements) {
        return new TupleLiteral(loc, elements);
    }

    static VectorLiteral litVector(@NotNull Location loc, @NotNull List<Node> elements) {
        return new VectorLiteral(loc, elements);
    }

    static RecordLiteral litRecord(@NotNull Location loc, @NotNull Scope map) {
        return new RecordLiteral(loc, map);
    }

//    static Group group(@NotNull Location loc, @NotNull Node expr) {
//        return new Group(loc, expr);
//    }

    static Block block(@NotNull Location loc, @NotNull List<Node> stmts) {
        return new Block(loc, stmts);
    }

    static Block emptyBlock(@NotNull Location loc) {
        return block(loc, new ArrayList<>());
    }

    static Typedef typedef(@NotNull Location loc,
                           @NotNull Node pattern,
                           @NotNull Node type) {
        return new Typedef(loc, pattern, type, false);
    }

    static Typedef typedefRecursive(@NotNull Location loc,
                           @NotNull Node pattern,
                           @NotNull Node type) {
        return new Typedef(loc, pattern, type, true);
    }

    static Define defineRecursive(@NotNull Location loc,
                                  @NotNull Node pattern,
                                  @NotNull Node init,
                                  boolean mut) {
        return new Define(loc, pattern, null, init, mut, true);
    }

    static Define define(@NotNull Location loc,
                         @NotNull Id name,
                         @Nullable Node type) {
        return new Define(loc, name, type, null, false, false);
    }

    static Define define(@NotNull Location loc,
                         @NotNull Id name,
                         @Nullable Node type,
                         @NotNull Node init,
                         boolean mut) {
        return new Define(loc, name, type, init, mut, false);
    }

    static Define define(@NotNull Location loc,
                         @NotNull Node pattern,
                         @NotNull Node init,
                         boolean mut) {
        return new Define(loc, pattern, null, init, mut, false);
    }

    static Declare declare(@NotNull Location loc, @NotNull Scope props) {
        return new Declare(loc, props);
    }

    static Parameters params(@NotNull Location loc,
                             @NotNull List<Id> names,
                             @NotNull Declare declare) {
        return new Parameters(loc, names, declare);
    }

    static FunDef funDef(@NotNull Location loc,
                         @Nullable Id name,
                         @NotNull Parameters params,
                         @NotNull Block body,
                         boolean arrow) {
        return new FunDef(loc, name, params.names, params.declare, body, arrow);
    }

    static FunDef funDef(@NotNull Location loc,
                         @Nullable Id name,
                         @NotNull List<Id> params,
                         @NotNull Declare declare,
                         @NotNull Block body,
                         boolean arrow) {
        return new FunDef(loc, name, params, declare, body, arrow);
    }

    Arguments emptyArgs = args(
            unmodifiableList(emptyList()),
            unmodifiableList(emptyList()),
            unmodifiableMap(new HashMap<>(0))
    );

    static Arguments args(@NotNull List<Node> elements,
                          @NotNull List<Node> positional,
                          @NotNull Map<String, Node> keywords) {
        return new Arguments(elements, positional, keywords);
    }

    static Call call(@NotNull Location loc,
                     @NotNull Location oploc,
                     @NotNull Node callee,
                     @NotNull Arguments args) {
        return new Call(loc, oploc, callee, args);
    }

    static RecordDef recordDef(@NotNull Location loc,
                               @Nullable  Id name,
                               @Nullable List<Id> parents,
                               @NotNull Declare declare) {
        return new RecordDef(loc, name, parents, declare);
    }

    static Assign assign(@NotNull Location loc,
                         @NotNull Location oploc,
                         @NotNull Node lhs,
                         @NotNull Node rhs) {
        return new Assign(loc, oploc, lhs, rhs);
    }

    static Unary unary(@NotNull Location loc,
                       @NotNull Location oploc,
                       @NotNull Id op,
                       @NotNull Node arg,
                       boolean prefix) {
        return new Unary(loc, oploc, op, arg, prefix);
    }

    static Binary binary(@NotNull Location loc,
                         @NotNull Location oploc,
                         @Nullable Fixity fixity,
                         @NotNull Id op,
                         @NotNull Node lhs,
                         @NotNull Node rhs) {
        return new Binary(loc, oploc, fixity, op, lhs, rhs);
    }

    static Ternary ternary(@NotNull Location loc,
                           @NotNull Location oploc,
                           @NotNull Id op,
                           @NotNull Node left,
                           @NotNull Node mid,
                           @NotNull Node right) {
        return new Ternary(loc, oploc, op, left, mid, right);
    }

    static Subscript subscript(@NotNull Location loc, @NotNull Location oploc,
                               @NotNull Node value, @NotNull Node idxKey) {
        return new Subscript(loc, oploc, value, idxKey);
    }

    // objectMember
    static Attribute attribute(@NotNull Location loc,
                               @NotNull Location oploc,
                               @NotNull Node val,
                               @NotNull Id prop) {
        return new Attribute(loc, oploc, val, prop);
    }

    static If ifStmt(@NotNull Location loc,
                     @NotNull Node test,
                     @NotNull Block then,
                     @Nullable If orElse) {
        return new If(loc, test, then, orElse);
    }

    static If ifStmt(@NotNull Location loc,
                     @NotNull Node test,
                     @NotNull Block then,
                     @Nullable Block orElse) {
        return new If(loc, test, then, orElse);
    }

    static While whileStmt(@NotNull Location loc,
                           @NotNull Node test,
                           @NotNull Block body) {
        return new While(loc, test, body);
    }

    static For forStmt(@NotNull Location loc,
                       @NotNull Node init,
                       @NotNull Node test,
                       @NotNull Node update,
                       @NotNull Block body) {
        return new For(loc, init, test, update, body);
    }

    static CaseMatch caseMatch(@NotNull Location loc,
                               @NotNull Node pattern,
                               @Nullable Node guard,
                               @NotNull Block body) {
        return new CaseMatch(loc, pattern, guard, body);
    }

    static MatchPattern match(@NotNull Location loc, @NotNull Node value, @NotNull List<CaseMatch> cases) {
        return new MatchPattern(loc, value, cases);
    }

    static Break breakStmt(@NotNull Location loc) {
        return new Break(loc);
    }

    static Continue continueStmt(@NotNull Location loc) {
        return new Continue(loc);
    }

    static Return returnStmt(@NotNull Location loc) {
        return new Return(loc, null);
    }

    static Return returnStmt(@NotNull Location loc, @NotNull Node expr) {
        return new Return(loc, expr);
    }

    static Throw throwStmt(@NotNull Location loc, @NotNull Node expr) {
        return new Throw(loc, expr);
    }

    static Try tryStmt(@NotNull Location loc,
                       @NotNull Block try1,
                       @Nullable Id error,
                       @Nullable Block catch1,
                       @NotNull Block final1) {
        return new Try(loc, try1, error, catch1, final1);
    }

    static Assert assertStmt(@NotNull Location loc,
                             @NotNull Node expr,
                             @Nullable Node msg) {
        return new Assert(loc, expr, msg);
    }

    static Debugger debugger(@NotNull Location loc, @Nullable Node expr) {
        return new Debugger(loc, expr);
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    class Node {
        public Location loc;
        // operator loc 用于 assert 的位置, 比如 a + b 表达式, oploc 位置在 +
        public Location oploc;
        @Override public String toString() {
            return loc.codeSpan();
        }
    }

    // type[]
    class VectorOf extends Node {
        public final Node elem;
        private VectorOf(Location loc, Location oploc, Node elem) {
            this.elem = elem;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    class Operator extends Node {
        public final Fixity fixity;
        public final IntNum precedence;
        public final Id operator;
        private Operator(Location loc, Fixity fixity, IntNum precedence, Id operator) {
            this.fixity = fixity;
            this.precedence = precedence;
            this.operator = operator;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Module extends Node {
        public final @Nullable Id name;
        public final Block block;

        private Module(Location loc, @Nullable Id name, Block block) {
            this.name = name;
            this.block = block;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Import extends Node {
        public final List<Pair<Id, Id>> aliasMap;
        public final Node from;

        private Import(Location loc, List<Pair<Id, Id>> aliasMap, Node from) {
            this.aliasMap = aliasMap;
            this.from = from;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class LitType extends Node {
        public final Node node;

        private LitType(Location loc, Node node) {
            this.node = node;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class IntNum extends Node {
        public final String content;
        public final long value;
        public final int base;

        public IntNum(Location loc, String content, long value, int base) {
            this.content = content;
            this.value = value;
            this.base = base;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class FloatNum extends Node {
        public final String content;
        public final double value;
        public FloatNum(Location loc, String content, double value) {
            super();
            this.content = content;
            this.value = value;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Str extends Node {
        public final String value;
        private Str(Location loc, String value) {
            this.value = value;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class TupleLiteral extends Node {
        public final List<Node> elements;
        private TupleLiteral(Location loc, List<Node> elements) {
            this.elements = elements;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class VectorLiteral extends Node {
        public final List<Node> elements;
        private VectorLiteral(Location loc, List<Node> elements) {
            this.elements = elements;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class RecordLiteral extends Node {
        // name => [value=>, mut=>]
        public final Scope map;
        private RecordLiteral(Location loc, Scope map) {
            this.map = map;
            this.loc = loc;
            this.oploc = loc;
        }
        public Node node(String name) {
            return ((Node) map.lookupLocalProp(name, KEY_VAL));
        }
        public boolean mut(String name) {
            Object mut = map.lookupLocalProp(name, KEY_MUT);
            if (mut instanceof Boolean) {
                return ((Boolean) mut);
            } else {
                return false;
            }
        }
    }

    // identifier or name
    class Id extends Node {
        public final String name;
        private Id(Location loc, String name) {
            this.name = name;
            this.loc = loc;
            this.oploc = loc;
        }
    }

//    class Group extends Node {
//        public final Node expr;
//        private Group(Location loc, Node expr) {
//            this.expr = expr;
//            this.loc = loc;
//            this.oploc = loc;
//        }
//    }

    class Block extends Node {
        public final List<Node> stmts; // seq
        private Block(Location loc, List<Node> stmts) {
            this.stmts = stmts;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    // VarDef

    class Typedef extends Node {
        public final Node pattern;
        // type 只有 pattern instanceof Id 才有意义
        public final Node type;
        public final boolean rec;
        private Typedef(Location loc, Node pattern, Node type, boolean rec) {
            this.pattern = pattern;
            this.type = type;
            this.rec = rec;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Define extends Node {
        public final Node pattern;
        // type 只有 pattern instanceof Id 才有意义
        public @Nullable final Node type;
        public @Nullable final Node value;
        public final boolean mut;
        public final boolean rec;
        private Define(Location loc, Node pattern, @Nullable Node type, @Nullable Node value, boolean mut, boolean rec) {
            this.pattern = pattern;
            this.type = type;
            this.value = value;
            this.mut = mut;
            this.rec = rec;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Assign extends Node {
        public final Node pattern;
        public final Node value;
        private Assign(Location loc, Location oploc, Node pattern, Node value) {
            this.pattern = pattern;
            this.value = value;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    class Subscript extends Node {
        public final Node value;
        public final Node index;
        private Subscript(Location loc, Location oploc, Node value, Node index) {
            this.value = value;
            this.index = index;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    // ObjectMember | Property
    class Attribute extends Node {
        public final Node value;
        public final Id attr; // prop
        private Attribute(Location loc, Location oploc, Node value, Id attr) {
            this.value = value;
            this.attr = attr;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    class Declare {
        public final Location loc;
        public final Scope props;
        private Declare(Location loc, Scope props) {
            this.loc = loc;
            this.props = props;
        }
    }

    class Parameters {
        public final Location loc;
        public final List<Id> names;
        public final Declare declare;
        private Parameters(Location loc, List<Id> names, Declare declare) {
            this.loc = loc;
            this.names = names;
            this.declare = declare;
        }
    }

    class FunDef extends Node {
        @Nullable public final Id name;
        public final List<Id> params;
        // [ name => [type=>, default=>, mutable=> ...] ]
        public final Declare declare;
        public final Block body;
        // public final boolean arrow;
        // @Nullable public final Id rest; // todo
        private FunDef(Location loc, @Nullable Id name, List<Id> params, Declare declare, Block body, boolean arrow) {
            this.name = name;
            this.params = params;
            this.declare = declare;
            this.body = body;
            // this.arrow = arrow;
            this.loc = loc;
            this.oploc = loc;
        }
        @Override
        public String toString() {
            return loc.codeSpan();
            // return "fun → " + (name == null ? "<anonymous>" : name);
        }
    }

    // elements 是原始节点, positional 与 keywords 二选一
    class Arguments {
        public final List<Node> elements;
        public final List<Node> positional;
        public final Map<String, Node> keywords;
        private Arguments(List<Node> elements, List<Node> positional, Map<String, Node> keywords) {
            this.elements = elements;
            this.positional = positional;
            this.keywords = keywords;
        }
    }

    class Call extends Node {
        public final Node callee;
        public final Arguments args;
        private Call(Location loc, Location oploc, Node callee, Arguments args) {
            this.callee = callee;
            this.args = args;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    class RecordDef extends Node {
        @Nullable public final Id name;
        @Nullable public final List<Id> parents;
        public final Declare declare; // fields
        private RecordDef(Location loc, @Nullable Id name, @Nullable List<Id> parents, Declare declare) {
            this.name = name;
            this.parents = parents;
            this.declare = declare;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class If extends Node {
        public final Node test;
        public final Block then;
        @Nullable public final /*If|Block*/ Node orelse;
        private If(Location loc, Node test, Block then, @Nullable Node orelse) {
            this.test = test;
            this.then = then;
            this.orelse = orelse;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class While extends Node {
        public final Node test;
        public final Block body;
        private While(Location loc, Node test, Block body) {
            this.test = test;
            this.body = body;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class For extends Node {
        public final Node init;
        public final Node test;
        public final Node update;
        public final Block body;
        private For(Location loc, Node init, Node test, Node update, Block body) {
            this.init = init;
            this.test = test;
            this.update = update;
            this.body = body;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class MatchPattern extends Node {
        public final Node value;
        public final List<CaseMatch> cases;
        private MatchPattern(Location loc, Node value, List<CaseMatch> cases) {
            this.value = value;
            this.cases = cases;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class CaseMatch extends Node {
        public final Node pattern;
        @Nullable public final Node guard;
        public final Block body;
        private CaseMatch(Location loc, Node pattern, @Nullable Node guard, Block body) {
            this.pattern = pattern;
            this.guard = guard;
            this.body = body;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Break extends Node {
        private Break(Location loc) {
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Continue extends Node {
        private Continue(Location loc) {
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Return extends Node {
        @Nullable public final Node expr;
        private Return(Location loc, @Nullable Node expr) {
            this.expr = expr;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Throw extends Node {
        public final Node expr;
        private Throw(Location loc, Node expr) {
            this.expr = expr;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Try extends Node {
        public final Block try1;
        @Nullable public final Id error;
        @Nullable public final Block catch1;
        // @Nullable public final Match catch1;
        public final Block final1;
        private Try(Location loc, Block try1, @Nullable Id error, @Nullable Block catch1, Block final1) {
            this.try1 = try1;
            this.error = error;
            this.catch1 = catch1;
            this.final1 = final1;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Unary extends Node {
        public final Id operator;
        public final Node arg;
        public final boolean prefix;
        private Unary(Location loc, Location oploc, Id operator, Node arg, boolean prefix) {
            this.operator = operator;
            this.arg = arg;
            this.prefix = prefix;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    class Binary extends Node {
        @Nullable public final Fixity fixity;
        public final Id operator;
        public final Node lhs;
        public final Node rhs;
        private Binary(Location loc, Location oploc, @Nullable Fixity fixity, Id operator, Node lhs, Node rhs) {
            this.fixity = fixity;
            this.operator = operator;
            this.lhs = lhs;
            this.rhs = rhs;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    class Ternary extends Node {
        public final Id operator;
        public final Node left;
        public final Node mid;
        public final Node right;
        private Ternary(Location loc, Location oploc, Id operator, Node left, Node mid, Node right) {
            this.operator = operator;
            this.left = left;
            this.mid = mid;
            this.right = right;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    class Assert extends Node {
        public final Node expr;
        public final @Nullable Node msg;
        private Assert(Location loc, Node expr, @Nullable Node msg) {
            this.expr = expr;
            this.msg = msg;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class Debugger extends Node {
        public final @Nullable Node expr;
        private Debugger(Location loc, @Nullable Node expr) {
            this.expr = expr;
            this.loc = loc;
            this.oploc = loc;
        }
    }
}
