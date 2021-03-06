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
package io.github.t3r1jj.gammaj;

import com.sun.javafx.collections.ObservableListWrapper;
import io.github.t3r1jj.gammaj.controllers.SceneController;
import io.github.t3r1jj.gammaj.hotkeys.HotkeyListener;
import io.github.t3r1jj.gammaj.hotkeys.HotkeyPollerThread;
import io.github.t3r1jj.gammaj.hotkeys.HotkeysRunner;
import io.github.t3r1jj.gammaj.hotkeys.ProfileHotkeyListener;
import io.github.t3r1jj.gammaj.model.ColorProfile;
import io.github.t3r1jj.gammaj.model.Display;
import io.github.t3r1jj.gammaj.model.DisplayUtility;
import io.github.t3r1jj.gammaj.model.Gamma;
import io.github.t3r1jj.gammaj.model.MultiDisplay;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.control.Alert;
import javafx.stage.StageStyle;

public class ViewModel {

    private static ViewModel instance;
    private final ListProperty<ColorProfile> loadedProfiles = new SimpleListProperty<>(new ObservableListWrapper<>(new ArrayList<ColorProfile>()));
    private final List<Display> displays = new ArrayList<>();
    private final SetProperty<Gamma.Channel> selectedChannels = new SimpleSetProperty();
    private final HotkeysRunner hotkeysRunner = HotkeysRunner.getInstance();
    private final ObjectProperty<Display> currentDisplay = new SimpleObjectProperty<>();
    private final ObjectProperty<ColorProfile> currentProfile = new SimpleObjectProperty<>();
    private final BooleanProperty assistedAdjustment = new SimpleBooleanProperty(true);
    private final BooleanProperty detachDisplay = new SimpleBooleanProperty(false);
    private final BooleanProperty reset = new SimpleBooleanProperty(true);
    private final BooleanProperty isSrgb = new SimpleBooleanProperty(false);
    private final Configuration configuration = new Configuration();
    private final ResourceBundle resources;

    private ViewModel() {
        this.resources = ResourceBundle.getBundle("bundles/LangBundle");
        configuration.load();
        selectedChannels.set(FXCollections.observableSet(Gamma.Channel.values()));
        loadFileProfiles();
        registerHotkeys();

        DisplayUtility screenUtil = new DisplayUtility();
        final MultiDisplay multiDisplay = screenUtil.getMultiDisplay();
        List<Display> singleDisplays = multiDisplay.getDisplays();
        if (singleDisplays.size() == 1) {
            displays.addAll(singleDisplays);
        } else {
            displays.addAll(singleDisplays);
            displays.add(multiDisplay);
        }
        detachDisplay.addListener((observable, oldValue, nowDetach) -> {
            if (nowDetach) {
                multiDisplay.detachDisplays();
            } else {
                multiDisplay.attachDisplays();
            }
            configuration.setIsDisplaysDetached(nowDetach);
            configuration.save();
        });
        currentDisplay.set(singleDisplays.get(0));
        if (configuration.isDisplaysDetached()) {
            detachDisplay.set(true);
        }
        if (configuration.getLoadCorrespondingProfiles()) {
            loadCorrespondingProfiles();
        }
        assistedAdjustment.set(currentDisplay.get().getColorProfile().getModeIsAssissted());
    }

    // Load starting from whole screen to not let it override single display settings load
    private void loadCorrespondingProfiles() {
        Map<Display, String> correspondingProfiles = configuration.getCorrespondingProfiles(displays);
        for (int i = displays.size() - 1; i >= 0; i--) {
            Display display = displays.get(i);
            String profileName = correspondingProfiles.get(display);
            if ("".equals(profileName)) {
                display.setColorProfile(new ColorProfile(""));
                continue;
            }
            for (ColorProfile profile : loadedProfiles) {
                if (profile.getName().equals(profileName)) {
                    display.setColorProfile(profile.clone(profileName));
                }
            }
        }
    }

    public static ViewModel getInstance() {
        if (instance == null) {
            instance = new ViewModel();
        }
        return instance;
    }

    private void loadFileProfiles() {
        File[] colorProfileProperties = new File(".").listFiles(pathname -> pathname.getAbsolutePath().endsWith(".properties"));

        StringBuilder errorBuilder = new StringBuilder();
        for (File file : colorProfileProperties) {
            ColorProfile colorProfile = new ColorProfile(file);
            try {
                colorProfile.loadProfile();
                loadedProfiles.getValue().add(colorProfile);
            } catch (IOException ex) {
                Logger.getLogger(SceneController.class.getName()).log(Level.SEVERE, null, ex);
                errorBuilder.append("\"").append(colorProfile).append("\", ");
            }
        }
        currentProfile.set(null);
        if (errorBuilder.length() > 0) {
            errorBuilder.delete(errorBuilder.length() - 2, errorBuilder.length());
            errorBuilder.append(".");
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.initStyle(StageStyle.UTILITY);
            errorAlert.setTitle(resources.getString("profile_loading_error"));
            errorAlert.setHeaderText(null);
            errorAlert.setContentText(resources.getString("profile_loading_error_message") + ": " + errorBuilder.toString());
            errorAlert.showAndWait();
        }
    }

    private void registerHotkeys() {
        registerResetHotkey();
        registerProfileHotkeys();
    }

    private void registerResetHotkey() {
        HotkeyPollerThread resetHotkey = configuration.getHotkey();
        resetHotkey.setHotkeyListener(new HotkeyListener() {

            @Override
            public void hotkeyPressed() {
                reset.set(!reset.getValue());
            }

            @Override
            public String toString() {
                return "Reset (App hotkey)";
            }

        });
        hotkeysRunner.reregisterApplicationHotkey(resetHotkey);
    }

    private void registerProfileHotkeys() {
        StringBuilder errorBuilder = new StringBuilder();
        for (ColorProfile colorProfile : loadedProfiles) {
            HotkeyPollerThread loadedHotkey = null;
            try {
                loadedHotkey = colorProfile.getHotkey();
                if (loadedHotkey != null && !hotkeysRunner.isRegistered(loadedHotkey)) {
                    loadedHotkey.setHotkeyListener(new ProfileHotkeyListener(currentProfile, colorProfile));
                    hotkeysRunner.registerHotkey(loadedHotkey);
                } else {
                    colorProfile.setHotkey(null);
                }
            } catch (Exception ex) {
                Logger.getLogger(SceneController.class.getName()).log(Level.SEVERE, null, ex);
                errorBuilder.append("\"").append(colorProfile).append("\", ");
            }
        }
        if (errorBuilder.length() > 0) {
            errorBuilder.delete(errorBuilder.length() - 2, errorBuilder.length());
            errorBuilder.append(".");
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.initStyle(StageStyle.UTILITY);
            errorAlert.setTitle(resources.getString("hotkey_registration_error"));
            errorAlert.setHeaderText(null);
            errorAlert.setContentText(resources.getString("hotkey_registration_error_message") +": " + errorBuilder.toString());
            errorAlert.showAndWait();
        }
    }

    public void saveAndReset() {
        configuration.setCorrespondingProfiles(displays);
        configuration.save();
        if (configuration.isColorResetOnExit()) {
            for (Display display : displays) {
                display.resetGammaRamp();
            }
        }
    }

    public ListProperty<ColorProfile> loadedProfilesProperty() {
        return loadedProfiles;
    }

    public SetProperty<Gamma.Channel> selectedChannelsProperty() {
        return selectedChannels;
    }

    public HotkeysRunner getHotkeysRunner() {
        return hotkeysRunner;
    }

    public List<Display> getDisplays() {
        return displays;
    }

    public ObjectProperty<Display> currentDisplayProperty() {
        return currentDisplay;
    }

    public Display getCurrentDisplay() {
        return currentDisplay.get();
    }

    public void setCurrentDisplay(Display display) {
        this.currentDisplay.set(display);
    }

    public ObjectProperty<ColorProfile> currentProfileProperty() {
        return currentProfile;
    }

    public ColorProfile getCurrentProfile() {
        return currentProfile.get();
    }

    public void setCurrentProfile(ColorProfile colorProfile) {
        this.currentProfile.set(colorProfile);
    }

    public BooleanProperty assistedAdjustmentProperty() {
        return assistedAdjustment;
    }

    public boolean isAssistedAdjustment() {
        return assistedAdjustment.get();
    }

    public void setAssistedAdjustment(boolean isAssistedAdjustment) {
        this.assistedAdjustment.set(isAssistedAdjustment);
    }

    public BooleanProperty detachDisplayProperty() {
        return detachDisplay;
    }

    public boolean isDetachDisplay() {
        return detachDisplay.get();
    }

    public void setDetachDisplay(boolean isDetachDisplay) {
        this.detachDisplay.set(isDetachDisplay);
    }

    public BooleanProperty resetProperty() {
        return reset;
    }

    public boolean isReset() {
        return reset.get();
    }

    public void setReset(boolean isReset) {
        this.reset.set(isReset);
    }

    public BooleanProperty isSrgbProperty() {
        return isSrgb;
    }

    public boolean isSrgb() {
        return isSrgb.get();
    }

    public void setSrgb(boolean isSrgb) {
        this.isSrgb.set(isSrgb);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ObservableList<ColorProfile> getLoadedProfiles() {
        return loadedProfiles.get();
    }

    public ObservableSet<Gamma.Channel> getSelectedChannels() {
        return selectedChannels.get();
    }

}
