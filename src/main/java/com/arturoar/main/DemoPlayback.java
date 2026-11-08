package com.arturoar.main;

import com.arturoar.externalsortingalgorithms.BalanceMergeSort;
import com.arturoar.externalsortingalgorithms.ExternalRadixSort;
import com.arturoar.externalsortingalgorithms.PolyphaseMergeSort;
import com.arturoar.externalsortingalgorithms.SortTrace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class DemoPlayback implements SortTrace {
    private static final List<Integer> INPUT_VALUES = List.of(
            18, 5, 22, 9, 2, 13, 24, 7, 16, 1, 11, 20, 4, 14, 8, 23, 3, 10, 21, 6, 17, 12, 15, 19);
    static final int SIZE = INPUT_VALUES.size();

    enum Algorithm {
        POLYPHASE("Mezcla polifásica"),
        BALANCED("Mezcla equilibrada"),
        RADIX("Radix externo");

        private final String label;

        Algorithm(String label) {
            this.label = label;
        }

        Path sort(File input, int blockSize, SortTrace trace) throws IOException {
            return switch (this) {
                case POLYPHASE -> new PolyphaseMergeSort(input, blockSize).sortWithTrace(trace);
                case BALANCED -> new BalanceMergeSort(input).sortWithTrace(trace);
                case RADIX -> new ExternalRadixSort(input).sortWithTrace(trace);
            };
        }

        @Override
        public String toString() {
            return label;
        }
    }

    record Group(String name, List<Integer> values) {}
    record Frame(String phase, List<Group> groups, List<Integer> active,
                 int comparisons, int writes, boolean finished) {}

    private final List<Frame> frames = new ArrayList<>();
    private final Map<String, List<Integer>> locations = new HashMap<>();
    private String phase = "Entrada desordenada";
    private int comparisons;
    private int writes;

    static Frame initial() {
        return new Frame("Entrada desordenada", List.of(new Group("Entrada", INPUT_VALUES)),
                List.of(), 0, 0, false);
    }

    static List<Frame> record(Algorithm algorithm) throws IOException {
        var playback = new DemoPlayback();
        playback.frames.add(initial());
        playback.locations.put(INPUT, new ArrayList<>(INPUT_VALUES));

        Path directory = Files.createTempDirectory("sort-demo-");
        Path input = directory.resolve("demo.txt");
        Path output = null;
        try {
            Files.writeString(input, String.join(",", INPUT_VALUES.stream().map(Object::toString).toList()));
            output = algorithm.sort(input.toFile(), 4, playback);
            playback.finish();
            return List.copyOf(playback.frames);
        } finally {
            if (output != null) {
                Files.deleteIfExists(output);
            }
            Files.deleteIfExists(input);
            Files.delete(directory);
        }
    }

    @Override
    public void phase(Phase next, int step) {
        phase = switch (next) {
            case CHUNKS -> "Ordenar bloques de " + step + " valores";
            case DISTRIBUTION -> "Distribución inicial · " + step + " bloques vacíos";
            case NATURAL_RUNS -> "Repartir secuencias naturales";
            case MERGE -> "Fusión · pasada " + step;
            case BUCKETS -> "Distribuir por " + digitName(step);
            case COLLECT -> "Recoger cubetas de " + digitName(step);
        };
        capture(phase, List.of());
    }

    @Override
    public void compare(int first, int second) {
        comparisons++;
        capture(phase + " · comparar " + first + " y " + second, List.of(first, second));
    }

    @Override
    public void write(String from, String to, int value) {
        if (!locations.get(from).remove(Integer.valueOf(value))) {
            throw new IllegalStateException("Trace lost value " + value + " in " + from);
        }
        locations.computeIfAbsent(to, key -> new ArrayList<>()).add(value);
        writes++;
        capture(phase + " · " + value + " → " + label(to), List.of(value));
    }

    private void capture(String description, List<Integer> active) {
        List<Group> groups = locations.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .sorted(Comparator.comparingInt(entry -> order(entry.getKey())))
                .map(entry -> new Group(label(entry.getKey()), List.copyOf(entry.getValue())))
                .toList();
        frames.add(new Frame(description, groups, active, comparisons, writes, false));
    }

    private void finish() {
        List<Integer> values = frames.getLast().groups().getFirst().values();
        frames.add(new Frame("Ordenamiento completo", List.of(new Group("Resultado", values)),
                List.of(), comparisons, writes, true));
    }

    private static int order(String name) {
        return switch (name) {
            case INPUT -> 0;
            case "chunks" -> 1;
            default -> name.length() == 1 ? name.charAt(0) - 'A' + 2
                    : Integer.parseInt(name.substring(1)) + 20;
        };
    }

    private static String label(String name) {
        return switch (name) {
            case INPUT -> "Entrada";
            case "chunks" -> "Bloques";
            default -> name;
        };
    }

    private static String digitName(int position) {
        return switch (position) {
            case 1 -> "unidades";
            case 2 -> "decenas";
            default -> "dígito " + position;
        };
    }
}
