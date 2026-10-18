import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Just enough JSON for reading and writing block models: objects keep their key order. */
final class Json {
    private final String text;
    private int at;

    private Json(String text) {
        this.text = text;
    }

    static Object parse(String text) {
        Json json = new Json(text);
        Object value = json.value();
        json.space();
        if (json.at != text.length()) {
            throw new IllegalArgumentException("trailing text at " + json.at);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    static List<Object> array(Object value) {
        return (List<Object>) value;
    }

    static double number(Object value, double fallback) {
        return value instanceof Number n ? n.doubleValue() : fallback;
    }

    private Object value() {
        this.space();
        char c = this.text.charAt(this.at);
        switch (c) {
            case '{' -> {
                this.at++;
                Map<String, Object> map = new LinkedHashMap<>();
                this.space();
                if (this.text.charAt(this.at) == '}') {
                    this.at++;
                    return map;
                }
                while (true) {
                    this.space();
                    String key = this.string();
                    this.space();
                    this.expect(':');
                    map.put(key, this.value());
                    this.space();
                    if (this.text.charAt(this.at) == ',') {
                        this.at++;
                        continue;
                    }
                    this.expect('}');
                    return map;
                }
            }
            case '[' -> {
                this.at++;
                List<Object> list = new ArrayList<>();
                this.space();
                if (this.text.charAt(this.at) == ']') {
                    this.at++;
                    return list;
                }
                while (true) {
                    list.add(this.value());
                    this.space();
                    if (this.text.charAt(this.at) == ',') {
                        this.at++;
                        continue;
                    }
                    this.expect(']');
                    return list;
                }
            }
            case '"' -> {
                return this.string();
            }
            default -> {
                if (this.text.startsWith("true", this.at)) {
                    this.at += 4;
                    return Boolean.TRUE;
                }
                if (this.text.startsWith("false", this.at)) {
                    this.at += 5;
                    return Boolean.FALSE;
                }
                if (this.text.startsWith("null", this.at)) {
                    this.at += 4;
                    return null;
                }
                int start = this.at;
                while (this.at < this.text.length() && "+-0123456789.eE".indexOf(this.text.charAt(this.at)) >= 0) {
                    this.at++;
                }
                return Double.parseDouble(this.text.substring(start, this.at));
            }
        }
    }

    private String string() {
        this.expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = this.text.charAt(this.at++);
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\') {
                char e = this.text.charAt(this.at++);
                switch (e) {
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    case 'r' -> out.append('\r');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'u' -> {
                        out.append((char) Integer.parseInt(this.text.substring(this.at, this.at + 4), 16));
                        this.at += 4;
                    }
                    default -> out.append(e);
                }
            } else {
                out.append(c);
            }
        }
    }

    private void expect(char c) {
        if (this.text.charAt(this.at) != c) {
            throw new IllegalArgumentException("expected " + c + " at " + this.at);
        }
        this.at++;
    }

    private void space() {
        while (this.at < this.text.length() && Character.isWhitespace(this.text.charAt(this.at))) {
            this.at++;
        }
    }

    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, 0);
        return out.append('\n').toString();
    }

    private static void write(Object value, StringBuilder out, int indent) {
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                out.append("{}");
                return;
            }
            out.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.append("  ".repeat(indent + 1));
                quote(String.valueOf(e.getKey()), out);
                out.append(": ");
                write(e.getValue(), out, indent + 1);
                out.append(++i < map.size() ? ",\n" : "\n");
            }
            out.append("  ".repeat(indent)).append('}');
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                out.append("[]");
                return;
            }
            out.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                out.append("  ".repeat(indent + 1));
                write(list.get(i), out, indent + 1);
                out.append(i + 1 < list.size() ? ",\n" : "\n");
            }
            out.append("  ".repeat(indent)).append(']');
        } else if (value instanceof String s) {
            quote(s, out);
        } else if (value instanceof Double d) {
            out.append(d == Math.rint(d) && Math.abs(d) < 1e15 ? String.valueOf(d.longValue()) : String.format(Locale.ROOT, "%s", d));
        } else {
            out.append(value);
        }
    }

    private static void quote(String s, StringBuilder out) {
        out.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                default -> out.append(c);
            }
        }
        out.append('"');
    }
}
