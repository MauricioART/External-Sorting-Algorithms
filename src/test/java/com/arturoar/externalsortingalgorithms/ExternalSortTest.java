package com.arturoar.externalsortingalgorithms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ExternalSortTest {
    @TempDir
    Path directory;

    @Test
    void preservesAllValuesAndLeavesTheInputUntouched() throws IOException {
        assertSorted("", 2);
        assertSorted("0", 2, 0);
        assertSorted("3,1,2", 2, 1, 2, 3);
        assertSorted("3,2,1", 1, 1, 2, 3);
        assertSorted("\uFEFF-2147483648, -10\r\n-1, -1, 0, +9, 2147483647,",
                3, Integer.MIN_VALUE, -10, -1, -1, 0, 9, Integer.MAX_VALUE);
        assertSorted("2147483647,0,-1,-2147483648,10,-10,1", 2,
                Integer.MIN_VALUE, -10, -1, 0, 1, 10, Integer.MAX_VALUE);
        assertSorted("1,2,3", Integer.MAX_VALUE, 1, 2, 3);
    }

    @Test
    void agreesWithAnInMemorySortAcrossRunCounts() throws IOException {
        var random = new Random(42);
        for (int length : new int[]{4, 5, 6, 8, 9, 13, 14, 22, 34, 2000, 10001}) {
            int[] data = random.ints(length, -1000, 1001).toArray();
            String text = Arrays.stream(data).mapToObj(Integer::toString)
                    .collect(Collectors.joining(","));
            Arrays.sort(data);
            assertSorted(text, length <= 34 ? 1 : 37, data);
        }
    }

    @Test
    void invalidInputAndParametersDoNotPublishPartialResults() throws IOException {
        for (String invalid : List.of("3,1,word", "1,2147483648", "1,-2147483649")) {
            Path input = Files.createTempFile(directory, "invalid", ".txt");
            Files.writeString(input, invalid);

            assertThrows(IOException.class, () -> new PolyphaseMergeSort(input.toFile(), 2).sort());
            assertThrows(IOException.class, () -> new BalanceMergeSort(input.toFile()).sort());
            assertThrows(IOException.class, () -> new ExternalRadixSort(input.toFile()).sort());
            assertEquals(invalid, Files.readString(input));
            Files.delete(input);
        }

        Path input = Files.writeString(directory.resolve("input.txt"), "9,-10,2");
        assertThrows(IllegalArgumentException.class, () -> new PolyphaseMergeSort(input.toFile(), 0));
        assertThrows(IllegalArgumentException.class, () -> new ExternalRadixSort(input.toFile()).sort(0));
        assertThrows(IllegalArgumentException.class, () -> new ExternalRadixSort(input.toFile()).sort(1));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(input), files.toList());
        }
    }

    @Test
    void concurrentRunsPublishDistinctOutputs() throws Exception {
        Path input = Files.writeString(directory.resolve("input.txt"), "3,1,2");
        var sorter = new PolyphaseMergeSort(input.toFile(), 2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(sorter::sort);
            var second = executor.submit(sorter::sort);
            Path firstResult = first.get();
            Path secondResult = second.get();
            assertNotEquals(firstResult, secondResult);
            assertEquals("1,2,3,", Files.readString(firstResult));
            assertEquals("1,2,3,", Files.readString(secondResult));
            assertEquals("3,1,2", Files.readString(input));
        }
    }

    private void assertSorted(String text, int blockSize, int... expected) throws IOException {
        Path input = Files.createTempFile(directory, "input", ".txt");
        Files.writeString(input, text);
        String sorted = Arrays.stream(expected).mapToObj(value -> value + ",")
                .collect(Collectors.joining());
        for (Path result : List.of(
                new PolyphaseMergeSort(input.toFile(), blockSize).sort(),
                new BalanceMergeSort(input.toFile()).sort(),
                new ExternalRadixSort(input.toFile()).sort())) {
            assertNotEquals(input, result);
            assertEquals(sorted, Files.readString(result), result.getFileName().toString());
            assertEquals(text, Files.readString(input));
        }
        try (var files = Files.list(directory)) {
            assertTrue(files.noneMatch(Files::isDirectory));
        }
    }
}
