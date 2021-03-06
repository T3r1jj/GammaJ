/* 
 * Copyright 2016 Damian Terlecki.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.t3r1jj.gammaj.controllers;

import io.github.t3r1jj.gammaj.hotkeys.HotkeyPollerThread;
import io.github.t3r1jj.gammaj.model.ColorProfile;
import io.github.t3r1jj.gammaj.model.Gamma;
import io.github.t3r1jj.gammaj.model.Gamma.Channel;
import io.github.t3r1jj.gammaj.ViewModel;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.converter.IntegerStringConverter;

public class ManualTabController extends AbstractTabController {

    private IntegerProperty[][] gammaRampProperties;
    private int lastXIndex;
    private int lastValue;

    @FXML
    private TableView tableView;

    public ManualTabController(ViewModel viewModel) {
        super(viewModel);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        super.initialize(url, rb);
        canvas.setCursor(Cursor.CROSSHAIR);
        initializeTable();
        addCanvasHandlers();
        if (!viewModel.assistedAdjustmentProperty().get()) {
            viewModel.setCurrentProfile(viewModel.getCurrentDisplay().getColorProfile());
        }
        viewModel.assistedAdjustmentProperty().addListener((observable, oldValue, nowAssisted) -> {
            if (!nowAssisted) {
                if (isCurrentProfileDefault() || isCurrentDisplayProfileAssisted()) {
                    resetProfile();
                }
                loadLocalProfile();
                loadRampViewModel();
                updateRgbRadioButtons();
                drawGammaRamp();
                addTabListeners();
            } else {
                removeTabListeners();
            }
        });
        drawGammaRamp();
    }

    @Override
    protected void handleLoadLocalProfile() {
        if (isCurrentDisplayProfileAssisted()) {
            viewModel.assistedAdjustmentProperty().set(true);
        } else {
            loadLocalProfile();
        }
    }

    private void loadLocalProfile() {
        loadingProfile = true;
        ColorProfile colorProfile = viewModel.getCurrentDisplay().getColorProfile();
        HotkeyPollerThread hotkey = colorProfile.getHotkey();
        hotkeyInput.setHotkey(hotkey);
        viewModel.getCurrentDisplay().loadModelFromProfile(true);
        viewModel.getCurrentDisplay().setDeviceGammaRamp();
        loadRampViewModel();
        drawGammaRamp();
        loadingProfile = false;
    }

    private boolean isCurrentDisplayProfileAssisted() {
        return !isCurrentProfileDefault() && viewModel.getCurrentDisplay().getColorProfile().getModeIsAssissted();
    }

    @Override
    protected void resetColorAdjustment() {
        loadRampViewModel();
    }

    @Override
    protected void bindTabListeners() {
        if (!viewModel.assistedAdjustmentProperty().get()) {
            addTabListeners();
        }

    }

    private void addCanvasHandlers() {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            double eventX = event.getX();
            if (eventX < 0) {
                eventX = 0;
            } else if (eventX > canvas.getWidth()) {
                eventX = canvas.getWidth();
            }
            double eventY = event.getY();
            if (eventY < 0) {
                eventY = 0;
            } else if (eventY > canvas.getHeight()) {
                eventY = canvas.getHeight();
            }
            lastXIndex = (int) ((eventX / canvas.getWidth()) * (Gamma.CHANNEL_VALUES_COUNT - 1));
            lastValue = (int) (((canvas.getHeight() - eventY) / canvas.getHeight()) * Gamma.MAX_WORD);
            handleCanvasEvent(event);
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleCanvasEvent);

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            handleCanvasEvent(event);
            resetProfile();
        });
    }

    private void handleCanvasEvent(MouseEvent event) {
        double eventX = event.getX();
        if (eventX < 0) {
            eventX = 0;
        } else if (eventX > canvas.getWidth()) {
            eventX = canvas.getWidth();
        }
        double eventY = event.getY();
        if (eventY < 0) {
            eventY = 0;
        } else if (eventY > canvas.getHeight()) {
            eventY = canvas.getHeight();
        }
        int x = (int) ((eventX / canvas.getWidth()) * (Gamma.CHANNEL_VALUES_COUNT - 1));
        int value = (int) (((canvas.getHeight() - eventY) / canvas.getHeight()) * Gamma.MAX_WORD);

        int dx = Math.abs(x - lastXIndex);
        int dy = Math.abs(value - lastValue);

        int sx = (lastXIndex < x) ? 1 : -1;
        int sy = (lastValue < value) ? 1 : -1;

        int error = dx - dy;

        while (true) {

            for (Channel channel : viewModel.selectedChannelsProperty()) {
                viewModel.getCurrentDisplay().setGammaRampValue(channel, lastXIndex, lastValue);
                gammaRampProperties[channel.getIndex()][lastXIndex].set(lastValue);
            }

            if (lastXIndex == x && lastValue == value) {
                break;
            }

            int doubleError = error + error;

            if (doubleError > -dy) {
                error = error - dy;
                lastXIndex = lastXIndex + sx;
            }

            if (doubleError < dx) {
                error = error + dx;
                lastValue = lastValue + sy;
            }
        }

        viewModel.getCurrentDisplay().setDeviceGammaRamp();
        drawGammaRamp();
    }

    @Override
    protected void handleResetButtonAction(ActionEvent event) {
        super.handleResetButtonAction(event);
        loadRampViewModel();
    }

    private void initializeTable() {
        int[][] gammaRamp = viewModel.getCurrentDisplay().getGammaRamp();
        gammaRampProperties = new SimpleIntegerProperty[gammaRamp.length][gammaRamp[0].length];
        for (int y = 0; y < gammaRamp.length; y++) {
            for (int x = 0; x < gammaRamp[y].length; x++) {
                gammaRampProperties[y][x] = new SimpleIntegerProperty(gammaRamp[y][x]);
            }
        }

        TableColumn<Integer, String> firstTableColumn = new TableColumn<>(resources.getString("channel_index"));
        firstTableColumn.getStyleClass().add("my-header-column");
        firstTableColumn.sortableProperty().set(false);
        firstTableColumn.setCellValueFactory(param -> {
            switch (param.getValue()) {
                case 0:
                    return new ReadOnlyStringWrapper(resources.getString("red"));
                case 1:
                    return new ReadOnlyStringWrapper(resources.getString("green"));
                case 2:
                    return new ReadOnlyStringWrapper(resources.getString("blue"));
                default:
                    return new ReadOnlyStringWrapper(resources.getString("invalid"));
            }
        });
        tableView.getColumns().add(firstTableColumn);

        for (int i = 0; i < Gamma.CHANNEL_VALUES_COUNT; i++) {
            TableColumn<Integer, Integer> column = new TableColumn<>(String.valueOf(i));
            column.sortableProperty().set(false);
            final int columnIndex = i;
            column.setCellValueFactory(param -> gammaRampProperties[param.getValue()][columnIndex].asObject());
            column.setCellFactory(TextFieldTableCell.<Integer, Integer>forTableColumn(new IntegerStringConverter()));
            column.setOnEditCommit(event -> {
                resetProfile();
                if (event.getNewValue() < 0) {
                    gammaRampProperties[event.getRowValue()][columnIndex].set(0);
                } else if (event.getNewValue() > Gamma.MAX_WORD) {
                    gammaRampProperties[event.getRowValue()][columnIndex].set(Gamma.MAX_WORD);
                } else {
                    gammaRampProperties[event.getRowValue()][columnIndex].set(event.getNewValue());
                }
                viewModel.getCurrentDisplay().setGammaRampValue(Channel.getChannel(event.getRowValue()), columnIndex, event.getNewValue());
                viewModel.getCurrentDisplay().setDeviceGammaRamp();
                drawGammaRamp();
            });
            column.setEditable(true);
            column.setPrefWidth(60);
            tableView.getColumns().add(column);
        }
        tableView.getItems().addAll(Arrays.asList(0, 1, 2));
        tableView.setEditable(true);
        tableView.setFixedCellSize(25);
        tableView.prefHeightProperty().bind(Bindings.size(tableView.getItems()).multiply(tableView.getFixedCellSize()).add(40));
    }

    private void loadRampViewModel() {
        int[][] gammaRamp = viewModel.getCurrentDisplay().getGammaRamp();
        for (int y = 0; y < gammaRamp.length; y++) {
            for (int x = 0; x < gammaRamp[y].length; x++) {
                gammaRampProperties[y][x].set(gammaRamp[y][x]);
            }
        }
    }

    @Override
    protected void handleInvertButtonAction(ActionEvent event) {
        if (!loadingProfile) {
            resetProfile();
            for (Gamma.Channel channel : viewModel.selectedChannelsProperty()) {
                for (int x = 0; x < Gamma.CHANNEL_VALUES_COUNT; x++) {
                    gammaRampProperties[channel.getIndex()][x].set(Gamma.MAX_WORD - gammaRampProperties[channel.getIndex()][x].get());
                    viewModel.getCurrentDisplay().setGammaRampValue(channel, x, gammaRampProperties[channel.getIndex()][x].get());
                }
            }
            viewModel.getCurrentDisplay().setDeviceGammaRamp();
            drawGammaRamp();
        }
    }

    @Override
    protected void saveModeSettings(ColorProfile newColorProfile) {
        newColorProfile.setModeIsAssissted(false);
    }

    @Override
    protected void resetProfile() {
        super.resetProfile();
        viewModel.getCurrentDisplay().getColorProfile().setGammaRamp(viewModel.getCurrentDisplay().getGammaRamp());
    }

}
