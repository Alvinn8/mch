package ca.bkaw.mch.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringPathTest {
    @Test
    public void root() {
        StringPath root = StringPath.root();
        assertEquals("/", root.toString());
    }

    @Test
    public void resolve() {
        StringPath a = StringPath.of("foo");
        StringPath path = a.resolve("bar");
        assertEquals("foo/bar", path.toString());
    }

    @Test
    public void rootResolve() {
        StringPath a = StringPath.of("/foo");
        StringPath path = a.resolve("bar");
        assertEquals("/foo/bar", path.toString());
    }

    @Test
    public void resolveReplaceRoot() {
        StringPath a = StringPath.of("/foo");
        StringPath path = a.resolve("/bar");
        assertEquals("/bar", path.toString());
    }

    @Test
    public void resolveRelativeDoesNotReplaceRoot() {
        StringPath a = StringPath.of("/foo");
        StringPath path = a.resolve(Util.noLeadingSlash("/bar"));
        assertEquals("/foo/bar", path.toString());
    }

    @Test
    public void rootGetFileName() {
        StringPath path = StringPath.of("foo.txt");
        assertEquals("foo.txt", path.getFileName());
    }

    @Test
    public void subDirGetFileName() {
        StringPath path = StringPath.of("/foo/bar/baz.txt");
        assertEquals("baz.txt", path.getFileName());
    }

    @Test
    public void subDirGetFileNameNoExtension() {
        StringPath path = StringPath.of("/foo/bar/baz");
        assertEquals("baz", path.getFileName());
    }

    @Test
    public void getName012() {
        StringPath path = StringPath.of("foo/bar/baz");
        assertEquals("foo", path.getName(0));
        assertEquals("bar", path.getName(1));
        assertEquals("baz", path.getName(2));
    }

    @Test
    public void getNameLeadingSlash0123() {
        StringPath path = StringPath.of("/foo/bar/baz");
        assertEquals("", path.getName(0));
        assertEquals("foo", path.getName(1));
        assertEquals("bar", path.getName(2));
        assertEquals("baz", path.getName(3));
    }
}
