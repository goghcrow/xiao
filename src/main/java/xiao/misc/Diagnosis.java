package xiao.misc;

/**
 * @author chuxiaofeng
 */
public class Diagnosis {
    public enum Category {
        INFO, WARNING, ERROR
    }

    public final Location loc;
    public final Category category;
    public final String msg;

    public Diagnosis(Location loc, Category category, String msg) {
        this.loc = loc;
        this.category = category;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "Diagnosis: " + category + ": " + msg + " @" + loc;
    }
}
