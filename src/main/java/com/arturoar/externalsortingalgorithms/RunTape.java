package com.arturoar.externalsortingalgorithms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** A temporary file containing consecutive runs separated by a marker outside the int range. */
final class RunTape implements AutoCloseable {
    static final long END = Long.MAX_VALUE;

    private final Path path;
    private final String name;
    private DataInputStream reader;
    private DataOutputStream writer;

    RunTape(Path path, String name) {
        this.path = path;
        this.name = name;
    }

    void startWriting() throws IOException {
        close();
        writer = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)));
    }

    void startReading() throws IOException {
        close();
        reader = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)));
    }

    long read() throws IOException {
        return reader.readLong();
    }

    void write(long value) throws IOException {
        writer.writeLong(value);
    }

    void endRun() throws IOException {
        write(END);
    }

    void writeFrom(String source, long value, SortTrace trace) throws IOException {
        write(value);
        trace.write(source, name, (int) value);
    }

    void moveTo(RunTape target, long value, SortTrace trace) throws IOException {
        target.writeFrom(name, value, trace);
    }

    void copyRunTo(RunTape target, SortTrace trace) throws IOException {
        for (long value = read(); value != END; value = read()) {
            moveTo(target, value, trace);
        }
        target.endRun();
    }

    static void merge(RunTape left, RunTape right, RunTape target, SortTrace trace) throws IOException {
        long a = left.read();
        long b = right.read();
        while (a != END || b != END) {
            if (a != END && b != END) {
                trace.compare((int) a, (int) b);
            }
            if (a <= b) {
                left.moveTo(target, a, trace);
                a = left.read();
            } else {
                right.moveTo(target, b, trace);
                b = right.read();
            }
        }
        target.endRun();
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            DataInputStream stream = reader;
            reader = null;
            stream.close();
        }
        if (writer != null) {
            DataOutputStream stream = writer;
            writer = null;
            stream.close();
        }
    }
}
