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

import de.sw4j.util.network.test.common.ClientResult;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
class LiveDataService extends ScheduledService<Void> {

//    private final ExecutorService calculationService = Executors.newCachedThreadPool();
//
    private final LiveDataRunnable liveDataRunnable;

    private final DataReporter dataReporter;

    public LiveDataService(LiveDataRunnable liveDataRunnable, DataReporter dataReporter) {
        this.liveDataRunnable = liveDataRunnable;
        this.dataReporter = dataReporter;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<ClientResult> data = liveDataRunnable.getResultReader().getIntermediateResult();
                dataReporter.setPartialData(data);
                return null;
            }
        };
    }

}
