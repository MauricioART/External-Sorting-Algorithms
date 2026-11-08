package com.arturoar.main;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class MovingBars extends Pane {
    private static final double TOP = 28;
    private static final double GROUP_GAP = 0.45;
    private static final Color NORMAL = Color.web("#dce5f4");
    private static final Color ACTIVE = Color.web("#ff695d");
    private static final Color COMPLETE = Color.web("#65e1aa");

    private final Line[] grid = new Line[5];
    private final Rectangle[] bars = new Rectangle[DemoPlayback.SIZE];
    private final List<Label> labels = new ArrayList<>();
    private final Timeline motion = new Timeline();
    private DemoPlayback.Frame frame = DemoPlayback.initial();
    private double drawnWidth = -1;
    private double drawnHeight = -1;

    MovingBars() {
        setMinHeight(180);
        setPrefHeight(340);
        setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        for (int i = 0; i < grid.length; i++) {
            Line line = new Line();
            line.setManaged(false);
            line.setStroke(Color.web("#354153"));
            grid[i] = line;
            getChildren().add(line);
        }
        for (int i = 0; i < bars.length; i++) {
            Rectangle bar = new Rectangle();
            bar.setManaged(false);
            bar.setArcWidth(3);
            bar.setArcHeight(3);
            Tooltip.install(bar, new Tooltip(Integer.toString(i + 1)));
            bars[i] = bar;
            getChildren().add(bar);
        }
        // There can be no more nonempty groups than visible values.
        for (int i = 0; i < bars.length; i++) {
            Label label = new Label();
            label.setManaged(false);
            label.setAlignment(Pos.CENTER);
            label.getStyleClass().add("group-label");
            labels.add(label);
            getChildren().add(label);
        }
        show(frame, Duration.ZERO);
    }

    void show(DemoPlayback.Frame next, Duration duration) {
        frame = next;
        setAccessibleText(next.groups().stream()
                .map(group -> group.name() + " " + group.values())
                .collect(Collectors.joining(". ")));
        draw(duration);
    }

    @Override
    protected void layoutChildren() {
        if (drawnWidth != getWidth() || drawnHeight != getHeight()) {
            draw(Duration.ZERO);
        }
    }

    private void draw(Duration duration) {
        motion.stop();
        drawnWidth = getWidth();
        drawnHeight = getHeight();
        double baseline = drawnHeight - 1;
        double chartHeight = Math.max(0, baseline - TOP);
        double step = drawnWidth / (bars.length + (frame.groups().size() - 1) * GROUP_GAP);

        for (int i = 0; i < grid.length; i++) {
            double y = TOP + i * chartHeight / (grid.length - 1);
            grid[i].setStartY(y);
            grid[i].setEndY(y);
            grid[i].setEndX(drawnWidth);
        }

        double slot = 0;
        int groupIndex = 0;
        var keyValues = new ArrayList<KeyValue>();
        for (DemoPlayback.Group group : frame.groups()) {
            Label label = labels.get(groupIndex++);
            label.setText(group.name());
            label.resizeRelocate(slot * step, 0, group.values().size() * step, TOP);
            label.setVisible(true);

            for (int value : group.values()) {
                Rectangle bar = bars[value - 1];
                double height = value * chartHeight / bars.length;
                double x = (slot++ + 0.07) * step;
                double barWidth = 0.86 * step;
                bar.setY(baseline - height);
                bar.setHeight(height);
                bar.setFill(frame.finished() ? COMPLETE : frame.active().contains(value) ? ACTIVE : NORMAL);
                if (duration.equals(Duration.ZERO)) {
                    bar.setX(x);
                    bar.setWidth(barWidth);
                } else {
                    keyValues.add(new KeyValue(bar.xProperty(), x));
                    keyValues.add(new KeyValue(bar.widthProperty(), barWidth));
                }
            }
            slot += GROUP_GAP;
        }
        for (int i = groupIndex; i < labels.size(); i++) {
            labels.get(i).setVisible(false);
        }
        if (!keyValues.isEmpty()) {
            motion.getKeyFrames().setAll(new KeyFrame(duration, keyValues.toArray(KeyValue[]::new)));
            motion.playFromStart();
        }
    }
}
