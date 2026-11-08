package com.arturoar.externalsortingalgorithms;

import com.arturoar.tools.MergeSort;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/** External polyphase merge using two input tapes and one rotating output tape. */
public class PolyphaseMergeSort {
    private final Path input;
    private final int blockSize;

    public PolyphaseMergeSort(File file, int blockSize) {
        if (blockSize <= 0) {
            throw new IllegalArgumentException("Block size must be positive.");
        }
        this.input = file.toPath();
        this.blockSize = blockSize;
    }

    public Path sort() throws IOException {
        return sortWithTrace(SortTrace.NONE);
    }

    public Path sortWithTrace(SortTrace trace) throws IOException {
        try (var workspace = new SortWorkspace(input)) {
            RunTape staging = workspace.tape("chunks");
            trace.phase(SortTrace.Phase.CHUNKS, blockSize);
            long runs = createRuns(staging, trace);
            if (runs <= 1) {
                return workspace.publish("polyphase", runs == 0 ? null : staging);
            }

            // Consecutive Fibonacci counts let each pass exhaust one input tape.
            long leftCount = 1;
            long rightCount = 1;
            while (leftCount + rightCount < runs) {
                long next = leftCount + rightCount;
                rightCount = leftCount;
                leftCount = next;
            }

            RunTape left = workspace.tape("A");
            RunTape right = workspace.tape("B");
            RunTape output = workspace.tape("C");
            left.startWriting();
            right.startWriting();
            long dummyRuns = leftCount + rightCount - runs;
            trace.phase(SortTrace.Phase.DISTRIBUTION, (int) Math.min(dummyRuns, Integer.MAX_VALUE));
            for (long i = 0; i < leftCount; i++) {
                if (i < dummyRuns) {
                    left.endRun();
                } else {
                    staging.copyRunTo(left, trace);
                }
            }
            for (long i = 0; i < rightCount; i++) {
                staging.copyRunTo(right, trace);
            }
            staging.close();
            left.startReading();
            right.startReading();

            int pass = 0;
            while (true) {
                trace.phase(SortTrace.Phase.MERGE, ++pass);
                output.startWriting();
                for (long i = 0; i < rightCount; i++) {
                    RunTape.merge(left, right, output, trace);
                }
                output.startReading();
                leftCount -= rightCount;
                if (leftCount == 0) {
                    return workspace.publish("polyphase", output);
                }

                RunTape remaining = left;
                left = output;
                output = right;
                right = remaining;
                long remainingCount = leftCount;
                leftCount = rightCount;
                rightCount = remainingCount;
            }
        }
    }

    private long createRuns(RunTape staging, SortTrace trace) throws IOException {
        staging.startWriting();
        MergeSort mergeSort = new MergeSort();
        int[] block = new int[Math.min(blockSize, 4096)];
        int length = 0;
        long runs = 0;

        try (var source = new IntegerInput(input)) {
            for (Integer value = source.next(); value != null; value = source.next()) {
                if (length == block.length) {
                    block = Arrays.copyOf(block, (int) Math.min(blockSize, 2L * block.length));
                }
                block[length++] = value;
                if (length == blockSize) {
                    writeRun(staging, mergeSort, block, length, trace);
                    length = 0;
                    runs++;
                }
            }
        }
        if (length != 0) {
            writeRun(staging, mergeSort, block, length, trace);
            runs++;
        }
        staging.startReading();
        return runs;
    }

    private static void writeRun(RunTape tape, MergeSort sort, int[] block, int length, SortTrace trace)
            throws IOException {
        sort.sort(block, 0, length - 1);
        for (int i = 0; i < length; i++) {
            tape.writeFrom(SortTrace.INPUT, block[i], trace);
        }
        tape.endRun();
    }
}
