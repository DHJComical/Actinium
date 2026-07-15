package com.dhj.actinium.debug;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CoreProfileContextAttributesTest {
    @Test
    void configuresTheOriginalContextInRequiredOrder() {
        RecordingContext attributes = new RecordingContext();

        RecordingContext configured = CoreProfileContextAttributes.configure(
            attributes,
            value -> value.record("core"),
            value -> value.record("forward"),
            value -> value.record("debug")
        );

        assertSame(attributes, configured);
        assertEquals(List.of("core", "forward", "debug"), attributes.operations());
    }

    private static final class RecordingContext {
        private final List<String> operations = new ArrayList<>();

        private void record(String operation) {
            operations.add(operation);
        }

        private List<String> operations() {
            return List.copyOf(operations);
        }
    }
}
