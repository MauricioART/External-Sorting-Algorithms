package com.arturoar.externalsortingalgorithms;

/** Receives synchronous notifications from a sort without retaining its input. */
public interface SortTrace {
    String INPUT = "input";
    SortTrace NONE = new SortTrace() {};

    enum Phase { CHUNKS, DISTRIBUTION, NATURAL_RUNS, MERGE, BUCKETS, COLLECT }

    default void phase(Phase phase, int step) {}

    default void compare(int first, int second) {}

    default void write(String from, String to, int value) {}
}
