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
package de.sw4j.util.network.test.report.live;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
class ChartDataService extends ScheduledService<Void> {

    private final DataReporter dataReporter;

    public ChartDataService(DataReporter dataReporter) {
        this.dataReporter = dataReporter;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                dataReporter.aggregatePartialData();
                return null;
            }
        };
    }

}
