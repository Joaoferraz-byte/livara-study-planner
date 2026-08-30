package com.joaoferraz.livara.studyplanner.io;

import com.joaoferraz.livara.studyplanner.domain.Cycle;
import com.joaoferraz.livara.studyplanner.domain.FocusArea;
import com.joaoferraz.livara.studyplanner.domain.ScheduleTemplate;
import com.joaoferraz.livara.studyplanner.domain.StudyBlock;
import com.joaoferraz.livara.studyplanner.domain.WorkflowTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ScheduleStore {
    public ScheduleTemplate load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        Object parsed = new JsonParser(Files.readString(path, StandardCharsets.UTF_8)).parse();
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IOException("Schedule document root must be a JSON object");
        }
        int schemaVersion = integer(root, "schemaVersion");
        String name = string(root, "name");
        Cycle cycle = Cycle.valueOf(string(root, "cycle"));
        WorkflowTemplate workflowTemplate = root.get("workflowTemplate") instanceof String value
                ? WorkflowTemplate.fromId(value)
                : WorkflowTemplate.MARKET_PROGRAMMING;
        int pauseMinutes = integer(root, "pauseMinutes");
        String iconId = root.get("iconId") instanceof String value ? value : "layout-dashboard";
        Object cyclesValue = root.get("cycles");
        if (cyclesValue instanceof Map<?, ?> cycleMap) {
            EnumMap<Cycle, Map<DayOfWeek, List<StudyBlock>>> cycles = new EnumMap<>(Cycle.class);
            for (Cycle value : Cycle.values()) {
                Object rawDays = cycleMap.get(value.name());
                if (!(rawDays instanceof Map<?, ?> dayMap)) {
                    throw new IOException("Each cycle must contain a days object");
                }
                cycles.put(value, parseDays(dayMap));
            }
            return ScheduleTemplate.withCycles(schemaVersion, name, cycle, workflowTemplate, pauseMinutes, iconId, cycles);
        }
        Object daysValue = root.get("days");
        if (!(daysValue instanceof Map<?, ?> dayMap)) {
            throw new IOException("Schedule document must contain a days object");
        }
        return new ScheduleTemplate(schemaVersion, name, cycle, workflowTemplate, pauseMinutes, parseDays(dayMap));
    }

    private static Map<DayOfWeek, List<StudyBlock>> parseDays(Map<?, ?> dayMap) throws IOException {
        EnumMap<DayOfWeek, List<StudyBlock>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            Object blocksValue = dayMap.get(day.name());
            List<StudyBlock> blocks = new ArrayList<>();
            if (blocksValue instanceof List<?> rawBlocks) {
                for (Object rawBlock : rawBlocks) {
                    if (!(rawBlock instanceof Map<?, ?> block)) {
                        throw new IOException("Each study block must be a JSON object");
                    }
                    blocks.add(new StudyBlock(integer(block, "order"), FocusArea.fromId(string(block, "focus")),
                            string(block, "topic"), Duration.ofMinutes(longInteger(block, "durationMinutes")),
                            integer(block, "breakAfterMinutes")));
                }
            }
            days.put(day, List.copyOf(blocks));
        }
        return days;
    }

    public void save(Path path, ScheduleTemplate template) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(template, "template");
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.writeString(temporary, toJson(template) + System.lineSeparator(), StandardCharsets.UTF_8);
        Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public String toJson(ScheduleTemplate template) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        field(json, "schemaVersion", Integer.toString(template.schemaVersion()), false);
        field(json, "name", quote(template.name()), true);
        field(json, "cycle", quote(template.cycle().name()), true);
        field(json, "workflowTemplate", quote(template.workflowTemplate().id()), true);
        field(json, "pauseMinutes", Integer.toString(template.pauseMinutes()), false);
        field(json, "iconId", quote(template.iconId()), true);
        json.append("  \"cycles\": {\n");
        Cycle[] cycles = Cycle.values();
        for (int cycleIndex = 0; cycleIndex < cycles.length; cycleIndex++) {
            Cycle value = cycles[cycleIndex];
            json.append("    ").append(quote(value.name())).append(": {\n");
            appendDays(json, template.blocks(value));
            json.append("    }").append(cycleIndex + 1 == cycles.length ? "\n" : ",\n");
        }
        json.append("  }\n");
        json.append("}");
        return json.toString();
    }

    private static void appendDays(StringBuilder json, Map<DayOfWeek, List<StudyBlock>> days) {
        DayOfWeek[] values = DayOfWeek.values();
        for (int dayIndex = 0; dayIndex < values.length; dayIndex++) {
            DayOfWeek day = values[dayIndex];
            json.append("      ").append(quote(day.name())).append(": [\n");
            List<StudyBlock> blocks = days.getOrDefault(day, List.of());
            for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
                StudyBlock block = blocks.get(blockIndex);
                json.append("        {\n");
                field(json, "order", Integer.toString(block.order()), false, 10);
                field(json, "focus", quote(block.focus().id()), true, 10);
                field(json, "topic", quote(block.topic()), true, 10);
                field(json, "durationMinutes", Long.toString(block.duration().toMinutes()), false, 10);
                json.append("          \"breakAfterMinutes\": ")
                        .append(block.breakAfterMinutes()).append("\n");
                json.append("        }").append(blockIndex + 1 == blocks.size() ? "\n" : ",\n");
            }
            json.append("      ]").append(dayIndex + 1 == values.length ? "\n" : ",\n");
        }
    }

    private static void field(StringBuilder json, String key, String value, boolean quoted) {
        field(json, key, value, quoted, 2);
    }

    private static void field(StringBuilder json, String key, String value, boolean quoted, int spaces) {
        json.append(" ".repeat(spaces)).append(quote(key)).append(": ").append(value).append(",\n");
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c);
            }
        }
        return escaped.append('"').toString();
    }

    private static String string(Map<?, ?> object, String key) throws IOException {
        Object value = object.get(key);
        if (value instanceof String string) {
            return string;
        }
        throw new IOException("Missing string field: " + key);
    }

    private static int integer(Map<?, ?> object, String key) throws IOException {
        return Math.toIntExact(longInteger(object, key));
    }

    private static long longInteger(Map<?, ?> object, String key) throws IOException {
        Object value = object.get(key);
        if (!(value instanceof Number number)) {
            throw new IOException("Missing numeric field: " + key);
        }
        double numeric = number.doubleValue();
        long integral = number.longValue();
        if (!Double.isFinite(numeric) || numeric != integral) {
            throw new IOException("Numeric field must be an integer: " + key);
        }
        return integral;
    }

    private static final class JsonParser {
        private final String source;
        private int index;

        private JsonParser(String source) {
            this.source = source;
        }

        private Object parse() throws IOException {
            skipWhitespace();
            Object value = value();
            skipWhitespace();
            if (index != source.length()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object value() throws IOException {
            skipWhitespace();
            if (index >= source.length()) {
                throw error("Unexpected end of JSON");
            }
            return switch (source.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> stringValue();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> numberValue();
            };
        }

        private Map<String, Object> object() throws IOException {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = stringValue();
                skipWhitespace();
                expect(':');
                result.put(key, value());
                skipWhitespace();
                if (consume('}')) {
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> array() throws IOException {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return result;
            }
            while (true) {
                result.add(value());
                skipWhitespace();
                if (consume(']')) {
                    return result;
                }
                expect(',');
            }
        }

        private String stringValue() throws IOException {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < source.length()) {
                char c = source.charAt(index++);
                if (c == '"') {
                    return result.toString();
                }
                if (c != '\\') {
                    result.append(c);
                    continue;
                }
                if (index >= source.length()) {
                    throw error("Unterminated escape");
                }
                char escape = source.charAt(index++);
                switch (escape) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(unicodeEscape());
                    default -> throw error("Invalid escape: " + escape);
                }
            }
            throw error("Unterminated string");
        }

        private char unicodeEscape() throws IOException {
            if (index + 4 > source.length()) {
                throw error("Incomplete unicode escape");
            }
            String hex = source.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw error("Invalid unicode escape");
            }
        }

        private Number numberValue() throws IOException {
            int start = index;
            if (consume('-')) {
                // sign consumed
            }
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
            if (index < source.length() && source.charAt(index) == '.') {
                index++;
                while (index < source.length() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
            }
            if (index < source.length() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
                index++;
                if (index < source.length() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
                    index++;
                }
                while (index < source.length() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
            }
            if (start == index) {
                throw error("Expected JSON value");
            }
            String value = source.substring(start, index);
            try {
                return value.contains(".") || value.contains("e") || value.contains("E")
                        ? Double.parseDouble(value) : Long.parseLong(value);
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private Object literal(String expected, Object value) throws IOException {
            if (!source.startsWith(expected, index)) {
                throw error("Invalid literal");
            }
            index += expected.length();
            return value;
        }

        private void expect(char expected) throws IOException {
            skipWhitespace();
            if (index >= source.length() || source.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean consume(char expected) {
            if (index < source.length() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private IOException error(String message) {
            return new IOException(message + " at character " + index);
        }
    }
}
