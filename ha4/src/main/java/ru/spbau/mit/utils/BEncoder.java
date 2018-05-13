package ru.spbau.mit.utils;

import ru.spbau.mit.data.ClientInfo;
import ru.spbau.mit.data.FileInfo;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BEncoder {
    private String in;
    private StringBuilder sb = new StringBuilder();
    private int id;

    public BEncoder() {

    }

    public BEncoder(String in) {
        this.in = in;
    }

    public void setInput(String in) {
        this.in = in;
        id = 0;
    }

    public void resetOutput() {
        sb.setLength(0);
    }

    public void write(Object o) {
        if (o instanceof Map) {
            sb.append('d');
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                write(e.getKey());
                write(e.getValue());
            }
            sb.append('e');
        } else if (o instanceof FileInfo) {
            sb.append('d');
            write("fileID");
            write(((FileInfo) o).getFileId());
            write("name");
            write(((FileInfo) o).getName());
            write("size");
            write(((FileInfo) o).getSize());
            sb.append('e');
        } else if (o instanceof ClientInfo) {
            sb.append('d');
            write("ip");
            write(((ClientInfo) o).getClientIp());
            write("port");
            write(((ClientInfo) o).getClientPort());
            sb.append('e');
        }
        else if (o instanceof Iterable) {
            sb.append('l');
            writeAll((Iterable) o);
            sb.append('e');
        } else if (o.getClass().isArray()) {
            sb.append('l');
            if (o instanceof Object[]) {
                writeAll((Object[]) o);
            } else {
                int len = Array.getLength(o);
                for (int i = 0; i < len; ++i)
                    write(Array.get(o, i));
            }
            sb.append('e');
        } else if (o instanceof String) {
            String e = (String) o;
            sb.append(e.length()).append(':').append(e);
        } else if (o instanceof Long || o instanceof Integer || o instanceof Byte) {
            sb.append('i').append(o).append('e');
        } else if (o instanceof Boolean) {
            sb.append('i').append((Boolean) o ? '1' : '0').append('e');
        }
    }

    public void writeAll(Object... os) {
        for (Object o : os)
            write(o);
    }

    public void writeAll(Iterable<Object> os) {
        for (Object o : os)
            write(o);
    }

    public ArrayList<Object> readAll() {
        ArrayList<Object> out = new ArrayList<>();
        while (true) {
            Object o = read();
            if (o == null)
                return out;
            out.add(o);
            ++id;
        }
    }

    public Object read() {
        if (id >= in.length())
            return null;
        char type = in.charAt(id);
        ++id;
        if (type == 'i') {
            int out = 0;
            int start = id;
            int limit = id + 22;
            boolean neg = false;
            for (; id <= limit; ++id) {
                char c = in.charAt(id);
                if (id == start && c == '-') {
                    neg = true;
                    continue;
                }
                if (c == 'e')
                    return neg ? -out : out;
                out = out * 10 + (c - 48);
            }
        } else if (type == 'l') {
            ArrayList<Object> out = new ArrayList<>();
            while (true) {
                if (in.charAt(id) == 'e')
                    return out;
                out.add(read());
                ++id;
            }
        } else if (type == 'd') {
            LinkedHashMap<Object, Object> out = new LinkedHashMap<>();
            while (true) {
                if (in.charAt(id) == 'e')
                    return out;
                Object key = read();
                ++id;
                Object value = read();
                out.put(key, value);
                ++id;
            }
        } else if (type >= '0' && type <= '9') {
            int len = type - 48;
            int limit = id + 11;
            for (; id <= limit; ++id) {
                char c = in.charAt(id);
                if (c == ':') {
                    String out = in.substring(id + 1, id + len + 1);
                    id += len;
                    return out;
                }
                len = len * 10 + (c - 48);
            }
        }
        return null;
    }

    public String getString() {
        return sb.toString();
    }
}