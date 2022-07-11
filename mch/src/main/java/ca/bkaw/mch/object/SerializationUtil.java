package ca.bkaw.mch.object;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SerializationUtil {
    private SerializationUtil() {
    }

    /**
     * Write a String to Reference20 map.
     *
     * @param map The map to write.
     * @param stream The stream to write to.
     * @throws IOException If an I/O error occurs.
     * @see #readMap(DataInputStream)
     */
    public static void writeMap(Map<String, Reference20> map, DataOutputStream stream) throws IOException {
        // Write the size
        stream.writeInt(map.size());
        // Sort entries to ensure result is always the same
        List<Map.Entry<String, Reference20>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        // Write entries
        for (Map.Entry<String, Reference20> entry : entries) {
            stream.writeUTF(entry.getKey());
            entry.getValue().write(stream);
        }
    }

    /**
     * Read a String to Reference20 map.
     *
     * @param stream The stream to read from.
     * @return The read map.
     * @throws IOException If an I/O error occurs.
     * @see #writeMap(Map, DataOutputStream)
     */
    public static Map<String, Reference20> readMap(DataInputStream stream) throws IOException {
        int size = stream.readInt();
        Map<String, Reference20> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = stream.readUTF();
            Reference20 value = Reference20.read(stream);
            map.put(key, value);
        }
        return map;
    }
}
