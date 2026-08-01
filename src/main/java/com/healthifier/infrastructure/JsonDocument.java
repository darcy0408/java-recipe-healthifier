package com.healthifier.infrastructure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonDocument {
    private JsonDocument() {}

    static Object parse(String source) { return new Parser(source).parse(); }

    static String write(Object value) {
        StringBuilder output = new StringBuilder();
        writeValue(value, output, 0);
        return output.append('\n').toString();
    }

    private static void writeValue(Object value, StringBuilder out, int depth) {
        if (value == null) out.append("null");
        else if (value instanceof String text) quote(text, out);
        else if (value instanceof Boolean || value instanceof Number) out.append(value);
        else if (value instanceof Map<?, ?> map) writeObject(map, out, depth);
        else if (value instanceof List<?> list) writeArray(list, out, depth);
        else throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }

    private static void writeObject(Map<?, ?> map, StringBuilder out, int depth) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) throw new IllegalArgumentException("JSON keys must be strings");
            if (!first) out.append(',');
            newline(out, depth + 1);
            quote(key, out);
            out.append(": ");
            writeValue(entry.getValue(), out, depth + 1);
            first = false;
        }
        if (!map.isEmpty()) newline(out, depth);
        out.append('}');
    }

    private static void writeArray(List<?> list, StringBuilder out, int depth) {
        out.append('[');
        for (int index = 0; index < list.size(); index++) {
            if (index > 0) out.append(',');
            newline(out, depth + 1);
            writeValue(list.get(index), out, depth + 1);
        }
        if (!list.isEmpty()) newline(out, depth);
        out.append(']');
    }

    private static void newline(StringBuilder out, int depth) {
        out.append('\n').append("  ".repeat(depth));
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (character < 0x20) out.append("\\u%04x".formatted((int) character));
                    else out.append(character);
                }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String source;
        private int index;
        private Parser(String source) { this.source = source; }

        private Object parse() {
            skip();
            Object result = value();
            skip();
            if (index != source.length()) fail("Trailing content");
            return result;
        }

        private Object value() {
            skip();
            if (index >= source.length()) return fail("Unexpected end");
            return switch (source.charAt(index)) {
                case '{' -> object(); case '[' -> array(); case '"' -> string();
                case 't' -> literal("true", true); case 'f' -> literal("false", false);
                case 'n' -> literal("null", null); default -> number();
            };
        }

        private Map<String, Object> object() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skip();
            if (take('}')) return result;
            do {
                skip();
                if (index >= source.length() || source.charAt(index) != '"') fail("Expected object key");
                String key = string();
                expect(':');
                result.put(key, value());
                skip();
            } while (take(','));
            expect('}');
            return result;
        }

        private List<Object> array() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skip();
            if (take(']')) return result;
            do { result.add(value()); skip(); } while (take(','));
            expect(']');
            return result;
        }

        private String string() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < source.length()) {
                char character = source.charAt(index++);
                if (character == '"') return result.toString();
                if (character != '\\') { result.append(character); continue; }
                if (index >= source.length()) fail("Incomplete escape");
                switch (source.charAt(index++)) {
                    case '"' -> result.append('"'); case '\\' -> result.append('\\');
                    case '/' -> result.append('/'); case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f'); case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r'); case 't' -> result.append('\t');
                    case 'u' -> result.append(unicode()); default -> fail("Unknown escape");
                }
            }
            return fail("Unterminated string");
        }

        private char unicode() {
            if (index + 4 > source.length()) return fail("Incomplete Unicode escape");
            try {
                char value = (char) Integer.parseInt(source.substring(index, index + 4), 16);
                index += 4;
                return value;
            } catch (NumberFormatException exception) { return fail("Invalid Unicode escape"); }
        }

        private BigDecimal number() {
            int start = index;
            while (index < source.length() && "-+0123456789.eE".indexOf(source.charAt(index)) >= 0) index++;
            if (start == index) return fail("Expected value");
            try { return new BigDecimal(source.substring(start, index)); }
            catch (NumberFormatException exception) { return fail("Invalid number"); }
        }

        private Object literal(String token, Object value) {
            if (!source.startsWith(token, index)) return fail("Invalid literal");
            index += token.length(); return value;
        }

        private void expect(char expected) { skip(); if (!take(expected)) fail("Expected '" + expected + "'"); }
        private boolean take(char expected) {
            if (index < source.length() && source.charAt(index) == expected) { index++; return true; }
            return false;
        }
        private void skip() { while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++; }
        private <T> T fail(String message) { throw new IllegalArgumentException(message + " at position " + index); }
    }
}
