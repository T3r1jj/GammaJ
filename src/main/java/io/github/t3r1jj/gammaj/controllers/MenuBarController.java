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

import io.github.t3r1jj.gammaj.TrayManager;
import io.github.t3r1jj.gammaj.hotkeys.HotkeyInputEventHandler;
import io.github.t3r1jj.gammaj.hotkeys.HotkeyPollerThread;
import io.github.t3r1jj.gammaj.hotkeys.HotkeysRunner;
import io.github.t3r1jj.gammaj.info.Library;
import io.github.t3r1jj.gammaj.info.ProjectInfo;
import io.github.t3r1jj.gammaj.model.ViewModel;
import java.awt.AWTException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

public class MenuBarController implements Initializable {

    ViewModel viewModel = ViewModel.getInstance();
    private final HostServices hostServices;
    private final TrayManager trayManager;
    private final HotkeysRunner hotkeysRunner;

    private HotkeyInputEventHandler hotkeyInput;

    @FXML
    private CheckMenuItem srgbCheckMenuItem;

    @FXML
    private MenuItem resetMenuItem;

    public MenuBarController(HostServices hostServices, TrayManager trayManager, HotkeysRunner hotkeysRunner) {
        this.hostServices = hostServices;
        this.trayManager = trayManager;
        this.hotkeysRunner = hotkeysRunner;
    }

    public void handleResetAction(ActionEvent event) {
        viewModel.getResetProperty().setValue(!viewModel.getResetProperty().get());
    }

    @FXML
    private void handleSettingsAction(ActionEvent event) {
        Alert settingsAlert = new Alert(Alert.AlertType.CONFIRMATION);
        settingsAlert.setTitle("Settings");
        settingsAlert.setHeaderText(null);
        final TextField hotkeyTextField = new TextField();
        hotkeyTextField.setEditable(false);
        HotkeyPollerThread resetHotkey = viewModel.getHotkeysRunner().getApplicationHotkey();
        hotkeyInput = new HotkeyInputEventHandler(hotkeyTextField);
        hotkeyInput.setHotkey(resetHotkey);
        settingsAlert.getDialogPane().getScene().setOnKeyPressed(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ESCAPE) {
                    event.consume();
                    if (hotkeyTextField.focusedProperty().get()) {
                        hotkeyInput.setHotkey(null);
                    }
                }
            }
        });
        hotkeyTextField.setOnKeyPressed(hotkeyInput);
        Label hotkeyLabel = new Label("Reset global hotkey:  ");
        CheckBox detachDisplaysCheckBox = new CheckBox("Displays detached from whole screen");
        detachDisplaysCheckBox.setSelected(viewModel.getDetachDisplay().get());
        viewModel.getDetachDisplay().bind(detachDisplaysCheckBox.selectedProperty());
        GridPane outPane = new GridPane();
        outPane.setMaxWidth(Double.MAX_VALUE);
        outPane.setMaxHeight(Double.MAX_VALUE);
        GridPane contentPane = new GridPane();
        contentPane.setMaxWidth(Double.MAX_VALUE);
        contentPane.setMaxHeight(Double.MAX_VALUE);
        contentPane.add(hotkeyLabel, 0, 0);
        contentPane.add(hotkeyTextField, 1, 0);
        outPane.add(contentPane, 0, 0);
        outPane.add(new Label(), 0, 1);
        outPane.add(detachDisplaysCheckBox, 0, 2);
        settingsAlert.getDialogPane().contentProperty().set(outPane);
        Optional<ButtonType> result = settingsAlert.showAndWait();
        if (result.get().equals(ButtonType.OK)) {
            handleHotkeyChange(event, resetHotkey);
        }
        hotkeyInput = null;
    }

    private void handleHotkeyChange(ActionEvent event, HotkeyPollerThread resetHotkey) {
        HotkeyPollerThread newHotkey = hotkeyInput.getHotkey();
        if (newHotkey == null) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Hotkey not changed");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Hotkey must not be empty in case of setting or loading entirely black/white profile you would not be able to reset it without system restart.");
            errorAlert.showAndWait();
            handleSettingsAction(event);
        } else if (viewModel.getHotkeysRunner().isRegisteredOnProfile(newHotkey)) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Hotkey not changed");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Hotkey \"" + newHotkey.getDisplayText()
                    + "\" has not been registered because it is already assigned to profile \""
                    + viewModel.getHotkeysRunner().registeredProfileInfo(newHotkey) + "\"");
            errorAlert.showAndWait();
            handleSettingsAction(event);
        } else if (!resetHotkey.equals(newHotkey)) {
            newHotkey.setHotkeyListener(resetHotkey.getHotkeyListener());
            viewModel.getHotkeysRunner().reregisterApplicationHotkey(newHotkey);
        }
    }

    @FXML
    private void handleExitAction(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void handleTraySelectedChange(ActionEvent event) {
        CheckMenuItem trayCheckBox = (CheckMenuItem) event.getSource();
        boolean trayEnabled = trayCheckBox.isSelected();
        try {
            trayManager.enableTray(trayEnabled);
        } catch (IOException | AWTException ex) {
            Logger.getLogger(MenuBarController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handleAboutAction(ActionEvent event) {
        ProjectInfo projectInfo = new ProjectInfo();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText(projectInfo.getAboutHeader());
        alert.setContentText(projectInfo.getAboutContent());
        alert.showAndWait();
    }

    @FXML
    private void handleLicenseAction(ActionEvent event) {
        ProjectInfo projectInfo = new ProjectInfo();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        List<Library> libraries = projectInfo.getLibrariesUsed();
        StringBuilder stringBuilder = new StringBuilder();
        List<ButtonType> buttons = new ArrayList<>();
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        for (Library library : libraries) {
            stringBuilder.append(library.nameLong)
                    .append(" v")
                    .append(library.version)
                    .append(" - ")
                    .append(library.licenseShort);
            ButtonType button = new ButtonType(library.nameShort);
            buttons.add(button);
        }
        buttons.add(okButton);
        alert.setTitle("Licenses");
        alert.setHeaderText("Libraries used");
        alert.setContentText(stringBuilder.toString());
        alert.getButtonTypes().setAll(buttons);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() != okButton) {
            Library pressedLibrary = libraries.get(buttons.indexOf(result.get()));
            showLibraryLicense(pressedLibrary);
        }
    }

    private void showLibraryLicense(Library pressedLibrary) {
        Alert licenseAlert = new Alert(Alert.AlertType.INFORMATION);
        licenseAlert.setTitle(pressedLibrary.nameLong);
        licenseAlert.setHeaderText(null);
        licenseAlert.setContentText(pressedLibrary.licenseLong);
        ButtonType urlButton = new ButtonType("Website");
        licenseAlert.getButtonTypes().addAll(urlButton);
        Optional<ButtonType> licenseResult = licenseAlert.showAndWait();
        if (licenseResult.get().equals(urlButton)) {
            hostServices.showDocument(pressedLibrary.url);
        } else {
            handleLicenseAction(null);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        srgbCheckMenuItem.selectedProperty().bindBidirectional(viewModel.getIsSrgbProperty());
        viewModel.getAssistedAdjustmentProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean isNowAssisted) {
                srgbCheckMenuItem.setDisable(!isNowAssisted);
            }
        });
    }

}
