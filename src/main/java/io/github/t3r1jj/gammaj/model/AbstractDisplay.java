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
package io.github.t3r1jj.gammaj.model;

import io.github.t3r1jj.gammaj.model.temperature.RgbTemperature;
import com.sun.jna.platform.win32.WinDef;
import io.github.t3r1jj.gammaj.model.Gamma.Channel;
import java.util.Objects;

public abstract class AbstractDisplay implements Display {

    protected String name;
    protected WinDef.HDC hdc;
    protected Gamma gammaModel;
    protected ColorProfile colorProfile = new ColorProfile("");

    protected AbstractDisplay() {
    }

    public AbstractDisplay(WinDef.HDC hdc) {
        this.hdc = hdc;
        this.gammaModel = new Gamma(hdc);
    }

    @Override
    public ColorProfile getColorProfile() {
        return colorProfile;
    }

    @Override
    public void setColorProfile(ColorProfile colorProfile) {
        this.colorProfile = colorProfile;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setGamma(Channel channel, double gamma) {
        colorProfile.setGamma(channel, gamma);
        gammaModel.setGamma(channel, gamma);
    }

    @Override
    public void setBrightness(Channel channel, double brightness) {
        colorProfile.setBrightness(channel, brightness);
        gammaModel.setBrightness(channel, brightness);
    }

    @Override
    public void setContrastBilateral(Channel channel, double contrast) {
        colorProfile.setContrastBilateral(channel, contrast);
        gammaModel.setContrastBilateral(channel, contrast);
    }

    @Override
    public void setContrastUnilateral(Channel channel, double contrast) {
        colorProfile.setContrastUnilateral(channel, contrast);
        gammaModel.setContrastUnilateral(channel, contrast);
    }

    @Override
    public void setTemperature(RgbTemperature colorTemperature) {
        colorProfile.setTemperature(colorTemperature);
        gammaModel.setTemperature(colorTemperature);
    }

    @Override
    public void resetGammaRamp() {
        colorProfile.reset();
        gammaModel.resetGammaRamp();
    }

    @Override
    public void invertGammaRamp(Channel channel) {
        gammaModel.invertGammaRamp(channel);
        colorProfile.setInvertedChannels(getInvertedChannels());
    }

    @Override
    public void reinitialize() {
        gammaModel.reinitializeGammaRamp();
    }

    @Override
    public double[][] getNormalizedGammaRamp() {
        return gammaModel.getNormalizedGammaRamp();
    }

    @Override
    public int[][] getGammaRamp() {
        return gammaModel.getGammaRamp();
    }

    @Override
    public boolean[] getInvertedChannels() {
        return gammaModel.getInvertedChannels();
    }

    public void loadModelFromProfile(boolean useRamp) {
        if (useRamp) {
            gammaModel.setGammaRamp(colorProfile.getGammaRamp());
        } else {
            for (Channel channel : Channel.values()) {
                gammaModel.setBrightness(channel, colorProfile.getBrightness(channel));
                gammaModel.setContrastBilateral(channel, colorProfile.getContrastBilateral(channel));
                gammaModel.setContrastUnilateral(channel, colorProfile.getContrastUnilateral(channel));
                gammaModel.setGamma(channel, colorProfile.getGamma(channel));
            }
            gammaModel.setTemperature(colorProfile.getTemperature());
            boolean[] invertedChannels = colorProfile.getInvertedChannels();
            boolean[] invertedChannelsInModel = getInvertedChannels();
            for (Gamma.Channel channel : Gamma.Channel.values()) {
                if (invertedChannels[channel.getIndex()] != invertedChannelsInModel[channel.getIndex()]) {
                    invertGammaRamp(channel);
                }
            }
        }
    }

    @Override
    public void setGammaRampValue(Channel channel, int x, int value) {
        gammaModel.setGammaRampValue(channel, x, value);
    }

    @Override
    public void setDeviceGammaRamp() {
        gammaModel.setDeviceGammaRamp();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 73 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractDisplay other = (AbstractDisplay) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
