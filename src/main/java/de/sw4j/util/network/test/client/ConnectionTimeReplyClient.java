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
package de.sw4j.util.network.test.client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ConnectionTimeReplyClient {

    private static final Logger LOG = Logger.getLogger(ConnectionTimeReplyClientSocketRunner.class.getName());

    private static final double[] SERIES = {1.0, 1.6, 2.5, 4.0, 6.3};

    public static void main(String... args) throws Exception {
        ScheduledExecutorService requestExecutorService = Executors.newSingleThreadScheduledExecutor();
        ScheduledExecutorService stopExecutorService = Executors.newSingleThreadScheduledExecutor();
        int startThreads = 10;
        int endThreads = 1000;

        boolean run = true;
        int i = 0;
        while (run) {
            double seriesNumber = i / SERIES.length;
            long threads = Math.round(SERIES[i % SERIES.length] * Math.pow(10, seriesNumber));
            i++;
            if (threads >= startThreads) {
                if (threads <= endThreads) {
                    ConnectionTimeReplyClientSocketRunner runner = new ConnectionTimeReplyClientSocketRunner(threads);
                    final ScheduledFuture future = requestExecutorService.scheduleAtFixedRate(runner, 0, 10, TimeUnit.SECONDS);
                    ScheduledFuture stopFuture = stopExecutorService.schedule(() -> {
                        future.cancel(false);
                    }, 3, TimeUnit.MINUTES);
                    stopFuture.get();
                    LOG.log(Level.INFO, new StringBuilder("Number of calls: ").append(runner.getNumberCalls()).append("\n")
                            .append("Number of connections: ").append(runner.getNumberCalls() * threads).append("\n")
                            .toString());
                } else {
                    run = false;
                }
            }
        }

        requestExecutorService.shutdown();
        stopExecutorService.shutdown();
    }

}
