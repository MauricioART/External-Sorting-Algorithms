package com.arturoar.main;

import com.arturoar.externalsortingalgorithms.SortTrace;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.arturoar.main.DemoPlayback.Algorithm;

public final class SortingApplication extends Application {
    private static final double STEP_MILLIS = 180;
    private static final int FILE_BLOCK_SIZE = 8192;

    private final ComboBox<Algorithm> algorithm = new ComboBox<>();
    private final ComboBox<Speed> speed = new ComboBox<>();
    private final Button play = new Button("Reproducir");
    private final Button previous = new Button("Anterior");
    private final Button next = new Button("Siguiente");
    private final Button reset = new Button("Reiniciar");
    private final Button sortFile = new Button("Ordenar archivo…");
    private final CheckBox sound = new CheckBox("Sonido");
    private final Label phase = new Label();
    private final Label stats = new Label();
    private final Label step = new Label();
    private final Label fileStatus = new Label();
    private final TextField resultPath = new TextField();
    private final ProgressBar progress = new ProgressBar();
    private final MovingBars chart = new MovingBars();
    private final Timeline playback = new Timeline(new KeyFrame(Duration.millis(STEP_MILLIS),
            event -> advance(1, true)));
    private Map<Algorithm, List<DemoPlayback.Frame>> recordings = Map.of();
    private List<DemoPlayback.Frame> frames = List.of(DemoPlayback.initial());
    private int index;
    private TonePlayer tones;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        configureControls(stage);

        var title = new Label("Ordenamiento en movimiento");
        title.getStyleClass().add("title");
        var subtitle = new Label("Demostración · 24 valores · orden ascendente");
        subtitle.getStyleClass().add("muted");
        var header = new VBox(8, title, subtitle, row(new Label("Algoritmo"), algorithm, sortFile));
        header.getStyleClass().add("section");

        var legend = new Label("■  Valores en uso");
        legend.getStyleClass().add("active-legend");
        var display = new VBox(8, phase, stats, chart, progress, row(legend, step));
        display.getStyleClass().add("display");
        VBox.setVgrow(chart, Priority.ALWAYS);

        var footer = new VBox(8, row(play, previous, next, reset, new Label("Velocidad"), speed, sound),
                fileStatus, resultPath);
        footer.getStyleClass().add("section");
        var root = new VBox(header, display, footer);
        root.getStyleClass().add("app");
        VBox.setVgrow(display, Priority.ALWAYS);

        var scene = new Scene(root, 900, 570);
        scene.getStylesheets().add(getClass().getResource("sorting.css").toExternalForm());
        stage.setTitle("Ordenamiento en movimiento");
        stage.setMinWidth(540);
        stage.setMinHeight(540);
        stage.setOnCloseRequest(event -> {
            if (sortFile.isDisabled()) {
                event.consume();
                fileStatus.setText("Espera a que termine el ordenamiento del archivo.");
            }
        });
        stage.setScene(scene);
        stage.show();
        showFrame(false);
        loadDemo();
    }

    private void configureControls(Stage stage) {
        algorithm.getItems().setAll(Algorithm.values());
        algorithm.setValue(Algorithm.POLYPHASE);
        algorithm.setAccessibleText("Algoritmo");
        algorithm.setOnAction(event -> selectRecording());
        speed.getItems().setAll(Speed.values());
        speed.setValue(Speed.NORMAL);
        speed.setAccessibleText("Velocidad");
        speed.setOnAction(event -> playback.setRate(speed.getValue().rate));
        playback.setCycleCount(Animation.INDEFINITE);

        play.getStyleClass().add("play");
        play.setOnAction(event -> togglePlayback());
        previous.setOnAction(event -> stepBy(-1));
        next.setOnAction(event -> stepBy(1));
        reset.setOnAction(event -> selectRecording());
        sound.setOnAction(event -> toggleSound());
        sortFile.setOnAction(event -> chooseFile(stage));
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setAccessibleText("Progreso de la demostración");
        stats.getStyleClass().add("muted");
        step.getStyleClass().add("muted");
        phase.setWrapText(true);
        fileStatus.setWrapText(true);
        resultPath.setEditable(false);
        resultPath.setAccessibleText("Ruta del resultado");
        resultPath.setVisible(false);
        resultPath.managedProperty().bind(resultPath.visibleProperty());
    }

    private void loadDemo() {
        phase.setText("Preparando la demostración…");
        var task = new Task<Map<Algorithm, List<DemoPlayback.Frame>>>() {
            @Override
            protected Map<Algorithm, List<DemoPlayback.Frame>> call() throws Exception {
                var results = new EnumMap<Algorithm, List<DemoPlayback.Frame>>(Algorithm.class);
                for (Algorithm value : Algorithm.values()) {
                    results.put(value, DemoPlayback.record(value));
                }
                return results;
            }
        };
        task.setOnSucceeded(event -> {
            recordings = task.getValue();
            selectRecording();
        });
        task.setOnFailed(event -> phase.setText("No se pudo preparar la demostración. "
                + task.getException().getMessage()));
        Thread.ofVirtual().start(task);
    }

    private void selectRecording() {
        if (recordings.isEmpty()) {
            return;
        }
        playback.stop();
        frames = recordings.get(algorithm.getValue());
        index = 0;
        showFrame(false);
    }

    private void togglePlayback() {
        if (playback.getStatus() == Animation.Status.RUNNING) {
            playback.pause();
        } else {
            if (index == frames.size() - 1) {
                selectRecording();
            }
            playback.play();
        }
        updateButtons();
    }

    private void stepBy(int delta) {
        playback.stop();
        advance(delta, true);
    }

    private void advance(int delta, boolean animate) {
        index = Math.clamp(index + delta, 0, frames.size() - 1);
        if (index == frames.size() - 1) {
            playback.stop();
        }
        showFrame(animate);
    }

    private void showFrame(boolean animate) {
        DemoPlayback.Frame frame = frames.get(index);
        phase.setText(frame.phase());
        String writes = frame.writes() + " escrituras";
        stats.setText(algorithm.getValue() == Algorithm.RADIX ? writes
                : frame.comparisons() + " comparaciones de fusión · " + writes);
        step.setText("Paso " + index + " de " + (frames.size() - 1));
        progress.setProgress(frames.size() == 1 ? 0 : (double) index / (frames.size() - 1));
        Duration motion = animate ? Duration.millis(Math.min(140, STEP_MILLIS * .75 / speed.getValue().rate))
                : Duration.ZERO;
        chart.show(frame, motion);
        if (animate && tones != null && !frame.active().isEmpty()) {
            tones.offer(frame.active().getFirst());
        }
        updateButtons();
    }

    private void updateButtons() {
        play.setText(playback.getStatus() == Animation.Status.RUNNING ? "Pausar" : "Reproducir");
        play.setDisable(frames.size() == 1);
        previous.setDisable(index == 0);
        next.setDisable(index == frames.size() - 1);
        reset.setDisable(index == 0);
    }

    private void toggleSound() {
        if (sound.isSelected()) {
            tones = new TonePlayer(() -> Platform.runLater(() -> {
                sound.setSelected(false);
                sound.setDisable(true);
                sound.setText("Sonido no disponible");
                stopSound();
            }));
        } else {
            stopSound();
        }
    }

    private void stopSound() {
        if (tones != null) {
            tones.close();
            tones = null;
        }
    }

    private void chooseFile(Stage stage) {
        var chooser = new FileChooser();
        chooser.setTitle("Seleccionar archivo de enteros");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos de texto", "*.txt"));
        File input = chooser.showOpenDialog(stage);
        if (input == null) {
            return;
        }

        Algorithm selected = algorithm.getValue();
        sortFile.setDisable(true);
        resultPath.setVisible(false);
        fileStatus.setText("Ordenando " + input.getName() + " con " + selected + "…");
        var task = new Task<Path>() {
            @Override
            protected Path call() throws Exception {
                return selected.sort(input, FILE_BLOCK_SIZE, SortTrace.NONE);
            }
        };
        task.setOnSucceeded(event -> {
            fileStatus.setText("Archivo ordenado");
            resultPath.setText(task.getValue().toString());
            resultPath.setVisible(true);
            sortFile.setDisable(false);
        });
        task.setOnFailed(event -> {
            fileStatus.setText("No se pudo ordenar el archivo. " + task.getException().getMessage());
            sortFile.setDisable(false);
        });
        Thread.ofVirtual().start(task);
    }

    private static FlowPane row(javafx.scene.Node... children) {
        var row = new FlowPane(8, 8, children);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    @Override
    public void stop() {
        playback.stop();
        stopSound();
    }

    private enum Speed {
        SLOW("0.5×", 0.5), NORMAL("1×", 1), FAST("2×", 2), FASTEST("4×", 4);

        private final String label;
        private final double rate;

        Speed(String label, double rate) {
            this.label = label;
            this.rate = rate;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
