package xiao.misc;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * π β β πΊ π π» π€’ π π±
 * @author chuxiaofeng
 */
public interface Helper {

    static void err(String msg) {
        System.err.println(msg);
    }

    static void log(String msg) {
        System.out.println(msg);
    }

    static <T> String join(@NotNull Collection<T> c, @NotNull String sep) {
        return c.stream().map(Object::toString).collect(Collectors.joining(sep));
    }

    @SafeVarargs
    static <T> List<T> lists(T...els) {
        List<T> lst = new ArrayList<>(els.length);
        lst.addAll(Arrays.asList(els));
        return lst;
    }

    static <T extends Throwable> void sneakyThrows(@NotNull Throwable e) throws T {
        //noinspection unchecked
        throw ((T) e);
    }

    static @NotNull String read(@NotNull Path path) {
        try {
            return UTF_8.decode(ByteBuffer.wrap(Files.readAllBytes(path))).toString();
        } catch (IOException e) {
            Helper.sneakyThrows(e);
            return null;
        }
    }

    static @NotNull Path path(String resourcePath) {
        try {
            URL r = Helper.class.getResource(resourcePath);
            if (r == null) {
                throw Error.runtime(Location.None,"ζ¨ζζΎε°ζδ»Ά β " + resourcePath);
            }
            return Paths.get(r.toURI());
        } catch (URISyntaxException e) {
            Helper.sneakyThrows(e);
            return null;
        }
    }
}
