package com.arturoar.main;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import java.util.concurrent.ArrayBlockingQueue;

/** Plays short notes away from the JavaFX thread. */
final class TonePlayer implements AutoCloseable {
    private static final int SAMPLE_RATE = 22050;
    private static final int SAMPLES = SAMPLE_RATE / 28;
    private final ArrayBlockingQueue<Integer> pending = new ArrayBlockingQueue<>(1);
    private final Thread worker;

    TonePlayer(Runnable unavailable) {
        worker = Thread.ofVirtual().start(() -> play(unavailable));
    }

    void offer(int value) {
        pending.poll();
        pending.offer(value);
    }

    private void play(Runnable unavailable) {
        var format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (var line = AudioSystem.getSourceDataLine(format)) {
            line.open(format, SAMPLES * 4);
            line.start();
            while (!Thread.currentThread().isInterrupted()) {
                byte[] samples = note(pending.take());
                line.write(samples, 0, samples.length);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException exception) {
            unavailable.run();
        }
    }

    private static byte[] note(int value) {
        double frequency = 130 * Math.pow(2, value / 13.0);
        byte[] bytes = new byte[SAMPLES * 2];
        for (int i = 0; i < SAMPLES; i++) {
            double envelope = Math.min(i / 50.0, 1.0 - (double) i / SAMPLES);
            short sample = (short) (1300 * envelope * Math.sin(2 * Math.PI * frequency * i / SAMPLE_RATE));
            bytes[i * 2] = (byte) sample;
            bytes[i * 2 + 1] = (byte) (sample >> 8);
        }
        return bytes;
    }

    @Override
    public void close() {
        worker.interrupt();
    }
}
