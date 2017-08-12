/*
 * Copyright (C) 2017 Uwe Plonus
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sw4j.util.network.test.report;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

/**
 * FXML Controller class
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ConfigurationController {

    @FXML
    private Spinner<Integer> listenPort;

    @FXML
    private CheckBox enableListener;

    private SpinnerValueFactory<Integer> listenPortFactory;

    public Integer getListenPort() {
        if (this.enableListener.isSelected()) {
            return this.listenPort.getValueFactory().getValue();
        } else {
            return null;
        }
    }

    public void setListenPort(Integer listenPort) {
        if (this.listenPort.getValueFactory() == null) {
            this.listenPortFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 65535);
            this.listenPort.setValueFactory(this.listenPortFactory);
        }
        if (listenPort == null) {
            this.enableListener.setSelected(false);
        } else {
            this.listenPort.getValueFactory().setValue(listenPort);
            this.enableListener.setSelected(true);
        }
    }

}
