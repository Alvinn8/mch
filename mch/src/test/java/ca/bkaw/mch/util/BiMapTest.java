package ca.bkaw.mch.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BiMapTest {
    @Test
    public void test() {
        BiMap<Integer, String> biMap = new BiMap<>();
        biMap.put(1, "test1");
        biMap.put(2, "test2");

        assertEquals("test1", biMap.get(1));
        assertEquals("test2", biMap.get(2));

        assertEquals(1, biMap.reverse().get("test1"));
        assertEquals(2, biMap.reverse().get("test2"));
    }
}
