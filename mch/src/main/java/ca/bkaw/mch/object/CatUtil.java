package ca.bkaw.mch.object;

import java.util.Map;

public class CatUtil {
    /**
     * Print a String to Reference20 map as a human-readable string.
     *
     * @param map The map to print.
     * @param str The StringBuilder to add to.
     */
    public static void printMap(Map<String, Reference20> map, StringBuilder str) {
        for (Map.Entry<String, Reference20> entry : map.entrySet()) {
            str.append("    ");
            str.append(entry.getKey());
            str.append(": ");
            str.append(entry.getValue().getSha1().asHex());
            str.append('\n');
        }
    }
}
