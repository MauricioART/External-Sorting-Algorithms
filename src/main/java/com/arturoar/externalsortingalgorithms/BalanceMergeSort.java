package com.arturoar.externalsortingalgorithms;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/** External balanced merge that alternates naturally sorted runs across two tapes. */
public class BalanceMergeSort {
    private final Path input;

    public BalanceMergeSort(File file) {
        input = file.toPath();
    }

    public Path sort() throws IOException {
        return sortWithTrace(SortTrace.NONE);
    }

    public Path sortWithTrace(SortTrace trace) throws IOException {
        try (var workspace = new SortWorkspace(input)) {
            RunTape[] sources = {workspace.tape("A"), workspace.tape("B")};
            RunTape[] targets = {workspace.tape("C"), workspace.tape("D")};
            trace.phase(SortTrace.Phase.NATURAL_RUNS, 0);
            long runs = distributeRuns(sources, trace);
            if (runs == 0) {
                return workspace.publish("balanced", null);
            }

            sources[0].startReading();
            sources[1].startReading();
            int pass = 0;
            while (runs > 1) {
                trace.phase(SortTrace.Phase.MERGE, ++pass);
                targets[0].startWriting();
                targets[1].startWriting();
                int target = 0;
                for (long remaining = runs; remaining >= 2; remaining -= 2) {
                    RunTape.merge(sources[0], sources[1], targets[target], trace);
                    target = 1 - target;
                }
                if (runs % 2 != 0) {
                    sources[0].copyRunTo(targets[target], trace);
                }
                runs = runs / 2 + runs % 2;

                RunTape[] consumed = sources;
                sources = targets;
                targets = consumed;
                sources[0].startReading();
                sources[1].startReading();
            }
            return workspace.publish("balanced", sources[0]);
        }
    }

    private long distributeRuns(RunTape[] tapes, SortTrace trace) throws IOException {
        tapes[0].startWriting();
        tapes[1].startWriting();
        long runs = 0;
        int target = 0;
        Integer previous = null;

        try (var source = new IntegerInput(input)) {
            for (Integer value = source.next(); value != null; value = source.next()) {
                if (previous != null && value < previous) {
                    tapes[target].endRun();
                    target = 1 - target;
                    runs++;
                }
                tapes[target].writeFrom(SortTrace.INPUT, value, trace);
                previous = value;
            }
        }
        if (previous != null) {
            tapes[target].endRun();
            runs++;
        }
        return runs;
    }
}
