package br.com.mpet.util;

import java.util.*;
import java.util.stream.Collectors;

public class Json {
    public static String obj(Map<String, Object> map) {
        return "{" + map.entrySet().stream()
                .map(e -> quote(e.getKey()) + ":" + value(e.getValue()))
                .collect(Collectors.joining(",")) + "}";
    }

    public static String arr(List<?> list) {
        return "[" + list.stream().map(Json::value).collect(Collectors.joining(",")) + "]";
    }

    public static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @SuppressWarnings("unchecked")
    public static String value(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return quote((String) v);
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        if (v instanceof Map) return obj((Map<String, Object>) v);
        if (v instanceof List) return arr((List<?>) v);
        return quote(v.toString());
    }
}
