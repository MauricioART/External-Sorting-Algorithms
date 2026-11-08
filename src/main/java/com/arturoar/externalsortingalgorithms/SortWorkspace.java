package com.arturoar.externalsortingalgorithms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Owns the temporary files of one sort and publishes the result after it has been written. */
final class SortWorkspace implements AutoCloseable {
    private static final String PREFIX = ".external-sort-";

    private final Path directory;
    private final List<RunTape> tapes = new ArrayList<>();

    SortWorkspace(Path input) throws IOException {
        directory = Files.createTempDirectory(input.toAbsolutePath().getParent(), PREFIX);
    }

    RunTape tape(String name) {
        RunTape tape = new RunTape(directory.resolve("tape-" + tapes.size()), name);
        tapes.add(tape);
        return tape;
    }

    Path publish(String algorithm, RunTape result) throws IOException {
        Path staged = directory.resolve("result.txt");
        try (var writer = Files.newBufferedWriter(staged)) {
            if (result != null) {
                for (long value = result.read(); value != RunTape.END; value = result.read()) {
                    writer.write(Long.toString(value));
                    writer.write(',');
                }
            }
        }
        String id = directory.getFileName().toString().substring(PREFIX.length());
        return Files.move(staged, directory.resolveSibling(algorithm + "-sorted-" + id + ".txt"));
    }

    @Override
    public void close() throws IOException {
        IOException failure = new IOException("Could not remove sorting workspace " + directory);
        for (RunTape tape : tapes) {
            try {
                tape.close();
            } catch (IOException exception) {
                failure.addSuppressed(exception);
            }
        }
        try (var files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                try {
                    Files.delete(file);
                } catch (IOException exception) {
                    failure.addSuppressed(exception);
                }
            }
        } catch (IOException exception) {
            failure.addSuppressed(exception);
        }
        try {
            Files.delete(directory);
        } catch (IOException exception) {
            failure.addSuppressed(exception);
        }
        if (failure.getSuppressed().length != 0) {
            throw failure;
        }
    }
}
