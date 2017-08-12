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
package org.sw4j.util.network.test.report.live;

import org.sw4j.util.network.test.common.ClientResult;
import java.util.List;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
class LiveDataService extends ScheduledService<Void> {

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
                dataReporter.addPartialData(data);
                return null;
            }
        };
    }

}
