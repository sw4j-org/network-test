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
package org.sw4j.util.network.test.report.file;

import java.io.File;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.ScrollEvent;

/**
 * FXML Controller class
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class FileReportController {

    private static final Logger LOG = Logger.getLogger(FileReportController.class.getName());

    @FXML
    private LineChart<String, Number> connectTimeChart;

    @FXML
    private LineChart<String, Number> serverTimeChart;

    @FXML
    private LineChart<String, Number> latencyChart;

    @FXML
    private LineChart<String, Number> responseTimeChart;

    @FXML
    private BarChart<String, Number> drops;

    @FXML
    private ProgressIndicator progressIndicator;

    public void loadData(File dataFile) {
        FileReportLoader fileReportLoader = new FileReportLoader(dataFile, connectTimeChart, serverTimeChart,
                latencyChart, responseTimeChart, drops, progressIndicator);
        fileReportLoader.start();
    }

    @FXML
    void mouseScroll(ScrollEvent event) {
        final double SCALE_DELTA = 1.1;
        event.consume();
        if (event.getDeltaY() == 0) {
            return;
        }
        double scaleFactor = (event.getDeltaY() < 0) ? SCALE_DELTA : 1 / SCALE_DELTA;
        NumberAxis yAxis = (NumberAxis)((LineChart)event.getSource()).getYAxis();
        yAxis.setAutoRanging(false);
        double newUpperBound = Math.round(yAxis.getUpperBound() * scaleFactor);
        yAxis.setUpperBound(newUpperBound);
        yAxis.setTickUnit(Math.round(newUpperBound / 10));
    }

}
