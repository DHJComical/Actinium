package com.dhj.actinium.debug.flight;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Decodes a binary flight recording to JSON using only the JDK runtime.
 */
public final class GlFlightRecordingDecoder {
    private GlFlightRecordingDecoder() {
    }

    /**
     * Reads one recording path and writes its structured timeline to standard output.
     *
     * @param arguments exactly one binary recording path
     * @throws IOException when the recording cannot be decoded or output cannot be written
     */
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Usage: GlFlightRecordingDecoder <recording.bin>");
        }
        GlFlightTimeline timeline = GlFlightRecordingReader.read(Path.of(arguments[0]));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        writeJson(timeline, writer);
        writer.flush();
    }

    /**
     * Writes a timeline as JSON without depending on the game's JSON libraries.
     *
     * @param timeline structured flight recording
     * @param writer destination writer
     * @throws IOException when output fails
     */
    public static void writeJson(GlFlightTimeline timeline, Writer writer) throws IOException {
        writer.write("{\n  \"path\": \"");
        writeEscaped(writer, timeline.path().toString());
        writer.write("\",\n  \"formatVersion\": ");
        writer.write(Integer.toString(timeline.formatVersion()));
        writer.write(",\n  \"mode\": \"");
        writer.write(timeline.mode().name());
        writer.write("\",\n  \"cleanClose\": ");
        writer.write(Boolean.toString(timeline.cleanClose()));
        writer.write(",\n  \"argumentEncoding\": \"twos-complement-hex-64\"");
        writer.write(",\n  \"processId\": ");
        writeDecimalLong(writer, timeline.processId());
        writer.write(",\n  \"startEpochMillis\": ");
        writeDecimalLong(writer, timeline.startEpochMillis());
        writer.write(",\n  \"startMonotonicNanos\": ");
        writeDecimalLong(writer, timeline.startMonotonicNanos());
        writer.write(",\n  \"events\": [");
        for (int index = 0; index < timeline.events().size(); index++) {
            GlFlightEvent event = timeline.events().get(index);
            writer.write(index == 0 ? "\n" : ",\n");
            writeEvent(writer, event);
        }
        if (!timeline.events().isEmpty()) {
            writer.write('\n');
        }
        writer.write("  ]\n}\n");
    }

    private static void writeEvent(Writer writer, GlFlightEvent event) throws IOException {
        writer.write("    {\"sequence\": ");
        writeDecimalLong(writer, event.sequence());
        writer.write(", \"epochMillis\": ");
        writeDecimalLong(writer, event.epochMillis());
        writer.write(", \"monotonicNanos\": ");
        writeDecimalLong(writer, event.monotonicNanos());
        writer.write(", \"frame\": ");
        writeDecimalLong(writer, event.frame());
        writer.write(", \"threadId\": ");
        writeDecimalLong(writer, event.threadId());
        writer.write(", \"kind\": \"");
        writer.write(event.kind().name());
        writer.write("\", \"phase\": \"");
        writer.write(event.phase().name());
        writer.write("\", \"stage\": \"");
        writer.write(event.stage().name());
        writer.write("\", \"arguments\": [");
        writeHexLong(writer, event.argument0());
        writer.write(", ");
        writeHexLong(writer, event.argument1());
        writer.write(", ");
        writeHexLong(writer, event.argument2());
        writer.write(", ");
        writeHexLong(writer, event.argument3());
        writer.write("]}");
    }

    private static void writeDecimalLong(Writer writer, long value) throws IOException {
        writer.write('"');
        writer.write(Long.toString(value));
        writer.write('"');
    }

    private static void writeHexLong(Writer writer, long value) throws IOException {
        String hexadecimal = Long.toHexString(value);
        writer.write('"');
        for (int padding = hexadecimal.length(); padding < 16; padding++) {
            writer.write('0');
        }
        writer.write(hexadecimal);
        writer.write('"');
    }

    private static void writeEscaped(Writer writer, String value) throws IOException {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> writer.write("\\\"");
                case '\\' -> writer.write("\\\\");
                case '\b' -> writer.write("\\b");
                case '\f' -> writer.write("\\f");
                case '\n' -> writer.write("\\n");
                case '\r' -> writer.write("\\r");
                case '\t' -> writer.write("\\t");
                default -> {
                    if (character < 0x20) {
                        writer.write("\\u");
                        writer.write(Character.forDigit(character >>> 12, 16));
                        writer.write(Character.forDigit(character >>> 8 & 0xF, 16));
                        writer.write(Character.forDigit(character >>> 4 & 0xF, 16));
                        writer.write(Character.forDigit(character & 0xF, 16));
                    } else {
                        writer.write(character);
                    }
                }
            }
        }
    }
}
