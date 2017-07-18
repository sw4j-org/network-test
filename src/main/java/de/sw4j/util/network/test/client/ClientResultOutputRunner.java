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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ClientResultOutputRunner implements Runnable, ResultCollector {

    private static final Logger LOG = Logger.getLogger(ClientResultOutputRunner.class.getName());

    private final Writer targetWriter;

    private final BlockingQueue<ClientResult> workingQueue;

    public ClientResultOutputRunner(String outputFileName) throws IOException {
        this(new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFileName, true), "UTF-8")));
    }

    public ClientResultOutputRunner(File outputFile) throws IOException {
        this(new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile, true), "UTF-8")));
    }

    private ClientResultOutputRunner(Writer targetWriter) throws IOException {
        this.targetWriter = targetWriter;
        this.workingQueue = new LinkedBlockingQueue<>();
        this.targetWriter.write("start;connected;server received;first response;completed\n");
    }

    @Override
    public void queueResult(ClientResult result) throws InterruptedException {
        LOG.log(Level.FINER, "Retrieved result.");
        this.workingQueue.put(result);
        LOG.log(Level.FINER, "Queued result.");
    }

    @Override
    public void run() {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        boolean hasMore = true;
        while (hasMore) {
            ClientResult result = null;
            try {
                LOG.log(Level.FINER, "Waiting for result.");
                result = this.workingQueue.take();
            } catch (InterruptedException iex) {
                LOG.log(Level.INFO, "Taking from queue interrupted.", iex);
            }
            LOG.log(Level.FINER, "Received result.");
            if (result == null || result.getStart() == null) {
                LOG.log(Level.FINER, "Processing ends.");
                hasMore = false;
            } else {
                LOG.log(Level.FINER, "Processing result.");
                StringBuilder sb = new StringBuilder();
                sb.append(dtf.format(result.getStart()));
                sb.append(";");
                sb.append(dtf.format(result.getConnected()));
                sb.append(";");
                sb.append(dtf.format(result.getServerReceived()));
                sb.append(";");
                sb.append(dtf.format(result.getFirstResponse()));
                sb.append(";");
                sb.append(dtf.format(result.getCompleted()));
                sb.append("\n");
                try {
                    this.targetWriter.write(sb.toString());
                } catch (IOException ioex) {
                    LOG.log(Level.WARNING, "Problems writing result data set.", ioex);
                }
                try {
                    this.targetWriter.flush();
                } catch (IOException ioex) {
                    LOG.log(Level.WARNING, "Problems flushing result.", ioex);
                }
            }
        }
    }

}
