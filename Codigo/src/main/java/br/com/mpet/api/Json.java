package br.com.mpet.api;

import java.util.*;

// Tiny JSON builder for simple objects (string/int/boolean) to avoid external deps.
public class Json {
    public static String obj(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(','); first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            sb.append(value(e.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    public static String arr(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(items.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    public static String quote(String s) { return '"' + escape(s) + '"'; }

    public static String value(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        if (v instanceof String) return quote((String)v);
        return quote(String.valueOf(v));
    }

    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}

