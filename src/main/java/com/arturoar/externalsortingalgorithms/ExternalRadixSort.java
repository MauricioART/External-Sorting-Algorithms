package com.arturoar.externalsortingalgorithms;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/** Stable external radix sort using signed decimal digits from least to most significant. */
public class ExternalRadixSort {
    private final Path input;

    public ExternalRadixSort(File file) {
        input = file.toPath();
    }

    public Path sort() throws IOException {
        return sortWithTrace(SortTrace.NONE);
    }

    public Path sort(int maxDigits) throws IOException {
        return sort(maxDigits, SortTrace.NONE);
    }

    public Path sortWithTrace(SortTrace trace) throws IOException {
        return sort(10, trace);
    }

    private Path sort(int maxDigits, SortTrace trace) throws IOException {
        if (maxDigits < 1 || maxDigits > 10) {
            throw new IllegalArgumentException("Maximum digit count must be between 1 and 10.");
        }
        long limit = 1;
        for (int i = 0; i < maxDigits; i++) {
            limit *= 10;
        }

        try (var workspace = new SortWorkspace(input)) {
            RunTape values = workspace.tape(SortTrace.INPUT);
            long largest = stageInput(values, limit, maxDigits);
            RunTape[] buckets = new RunTape[19];
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = workspace.tape("D" + (i - 9));
            }
            int digit = 0;
            for (long place = 1; place <= largest; place *= 10) {
                trace.phase(SortTrace.Phase.BUCKETS, ++digit);
                distribute(values, buckets, place, trace);
                trace.phase(SortTrace.Phase.COLLECT, digit);
                collect(values, buckets, trace);
            }
            return workspace.publish("radix", values);
        }
    }

    private long stageInput(RunTape values, long limit, int maxDigits) throws IOException {
        values.startWriting();
        long largest = 0;
        try (var source = new IntegerInput(input)) {
            for (Integer value = source.next(); value != null; value = source.next()) {
                largest = Math.max(largest, Math.abs((long) value));
                if (largest >= limit) {
                    throw new IllegalArgumentException("An input value exceeds " + maxDigits + " digits.");
                }
                values.write(value);
            }
        }
        values.endRun();
        values.startReading();
        return largest;
    }

    private static void distribute(RunTape values, RunTape[] buckets, long place, SortTrace trace)
            throws IOException {
        for (RunTape bucket : buckets) {
            bucket.startWriting();
        }
        for (long value = values.read(); value != RunTape.END; value = values.read()) {
            int digit = (int) ((value / place) % 10);
            values.moveTo(buckets[digit + 9], value, trace);
        }
    }

    private static void collect(RunTape values, RunTape[] buckets, SortTrace trace) throws IOException {
        values.startWriting();
        for (RunTape bucket : buckets) {
            bucket.endRun();
            bucket.startReading();
            for (long value = bucket.read(); value != RunTape.END; value = bucket.read()) {
                bucket.moveTo(values, value, trace);
            }
            bucket.close();
        }
        values.endRun();
        values.startReading();
    }
}
