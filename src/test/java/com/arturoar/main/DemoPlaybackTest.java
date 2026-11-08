package com.arturoar.main;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoPlaybackTest {
    @Test
    void eachFramePreservesTheBarsAndTheLastFrameIsSorted() throws IOException {
        List<Integer> expected = IntStream.rangeClosed(1, DemoPlayback.SIZE).boxed().toList();
        for (var algorithm : DemoPlayback.Algorithm.values()) {
            var frames = DemoPlayback.record(algorithm);
            assertEquals(DemoPlayback.initial(), frames.getFirst());
            for (var frame : frames) {
                assertEquals(expected, frame.groups().stream()
                        .flatMap(group -> group.values().stream()).sorted().toList());
            }
            var last = frames.getLast();
            assertTrue(last.finished());
            assertEquals(List.of(new DemoPlayback.Group("Resultado", expected)), last.groups());
            assertTrue(last.writes() >= DemoPlayback.SIZE);
            assertEquals(algorithm == DemoPlayback.Algorithm.RADIX, last.comparisons() == 0);
        }
    }
}
