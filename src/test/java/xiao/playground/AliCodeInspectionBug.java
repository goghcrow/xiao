package xiao.playground;

import org.jetbrains.annotations.NotNull;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author chuxiaofeng
 */
public class AliCodeInspectionBug {
    static class Entry implements Comparable<Entry> {
        final int i;
        Entry(int i) { this.i = i; }
        @Override public String toString() { return String.valueOf(i); }
        @Override public int compareTo(@NotNull Entry o) { return Integer.compare(i, o.i); }
    }

    public static void main(String[] args) {
        SortedSet<Entry> set = new TreeSet<>();
        set.add(new Entry(42));
        set.add(new Entry(100));
        set.add(new Entry(0));
        System.out.println(set);

        SortedMap<Entry, Integer> map = new TreeMap<>();
        map.put(new Entry(42), 42);
        map.put(new Entry(100), 100);
        map.put(new Entry(0), 0);
        System.out.println(map);
    }
}
