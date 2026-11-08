package com.arturoar.externalsortingalgorithms;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Streams signed decimal integers separated by commas or whitespace. */
final class IntegerInput implements AutoCloseable {
    private final BufferedReader reader;
    private long position;

    IntegerInput(Path path) throws IOException {
        reader = Files.newBufferedReader(path);
    }

    Integer next() throws IOException {
        int character = reader.read();
        if (position == 0 && character == '\uFEFF') {
            character = reader.read();
        }
        while (isSeparator(character)) {
            character = reader.read();
        }
        if (character == -1) {
            return null;
        }

        position++;
        boolean negative = character == '-';
        if (negative || character == '+') {
            character = reader.read();
        }
        if (character < '0' || character > '9') {
            throw invalidInteger();
        }

        long magnitude = 0;
        long limit = negative ? -(long) Integer.MIN_VALUE : Integer.MAX_VALUE;
        while (character >= '0' && character <= '9') {
            magnitude = magnitude * 10 + character - '0';
            if (magnitude > limit) {
                throw invalidInteger();
            }
            character = reader.read();
        }
        if (character != -1 && !isSeparator(character)) {
            throw invalidInteger();
        }
        return (int) (negative ? -magnitude : magnitude);
    }

    private static boolean isSeparator(int character) {
        return character == ',' || Character.isWhitespace(character);
    }

    private IOException invalidInteger() {
        return new IOException("Invalid 32-bit integer at position " + position);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
