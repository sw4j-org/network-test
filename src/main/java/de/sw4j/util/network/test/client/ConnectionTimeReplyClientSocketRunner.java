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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ConnectionTimeReplyClientSocketRunner implements Runnable {

    private static final Logger LOG = Logger.getLogger(ConnectionTimeReplyClientSocketRunner.class.getName());

    private boolean hasError = false;

    private final int serverPort;

    private final long threads;

    private int numberCalls;

    private final ResultCollector collector;

    public ConnectionTimeReplyClientSocketRunner(long threads, int serverPort, ResultCollector collector) {
        this.threads = threads;
        this.serverPort = serverPort;
        LOG.log(Level.INFO, new StringBuilder("Number of threads: ").append(this.threads).append("\n").toString());
        this.collector = collector;
    }

    @Override
    public void run() {
        ExecutorService threadPool = Executors.newWorkStealingPool();

        for (long i = 0; i < this.threads; i++) {
            threadPool.execute(() -> {
                communicate();
            });
        }

        numberCalls++;
    }

    private void communicate() {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        Instant start = Instant.now();

        Socket socket;
        try {
            socket = new Socket(InetAddress.getLocalHost(), this.serverPort);
        } catch (IOException ioex) {
            LOG.log(Level.WARNING, "Error while creating socket.", ioex);
            hasError = true;
            return;
        }

        Instant connected = Instant.now();

        char[] response = new char[1024];
        StringBuilder requestSb = new StringBuilder();
        StringBuilder responseSb = new StringBuilder();
        Writer requestWriter;
        Reader responseReader;
        try {
            requestWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            responseReader = new InputStreamReader(socket.getInputStream(), "UTF-8");
        } catch (IOException ioex) {
            LOG.log(Level.WARNING, "Error while getting input or output stream.", ioex);
            this.hasError = true;
            try {
                socket.close();
            } catch (IOException ioex2) {
                LOG.log(Level.INFO, "Ignoring exception during socket close.", ioex2);
            }
            return;
        }

        try {
            requestSb.append(dtf.format(connected));
        } catch (DateTimeException dtex) {
            this.hasError = true;
            LOG.log(Level.WARNING, "Error while formating the request.", dtex);
            return;
        }
        requestSb.append("\n\n.\n");
        try {
            requestWriter.append(requestSb);
            requestWriter.flush();
        } catch (IOException ioex) {
            this.hasError = true;
            LOG.log(Level.WARNING, "Error while writing request.", ioex);
            return;
        }

        Instant firstResponse = null;
        try {
            int byteRead = 0;
            while ((byteRead = responseReader.read(response)) >= 0) {
                if (firstResponse == null) {
                    firstResponse = Instant.now();
                }
                responseSb.append(response, 0, byteRead);
            }
        } catch (IOException ioex) {
            LOG.log(Level.WARNING, "Error while receiving request.", ioex);
            this.hasError = true;
            return;
        }

        Instant received = Instant.now();

        int lineBreak = responseSb.indexOf("\n");
        Instant serverTime;
        try {
            serverTime = Instant.from(dtf.parse(responseSb, new ParsePosition(lineBreak + 1)));
        } catch (DateTimeParseException dtpex) {
            LOG.log(Level.WARNING, "Error while parsing the response.", dtpex);
            this.hasError = true;
            return;
        }

        try {
            responseSb.append(dtf.format(received));
        } catch (DateTimeException dtex) {
            LOG.log(Level.WARNING, "Error while formating received time.", dtex);
            this.hasError = true;
            return;
        }
        responseSb.append("\n");

        Duration connectTime = Duration.between(start, connected);
        responseSb.append("Connect Time: ").append(connectTime.toString()).append("\n");
        Duration serverReceive = Duration.between(start, serverTime);
        responseSb.append("Server Receive: ").append(serverReceive.toString()).append("\n");
        Duration responseTime = Duration.between(start, received);
        responseSb.append("Response Time: ").append(responseTime.toString()).append("\n");
        LOG.log(Level.FINE, responseSb.toString());

        try {
            socket.close();
        } catch (IOException ioex) {
            LOG.log(Level.INFO, "Ignoring exception during socket close.", ioex);
            return;
        }

        try {
            collector.queueResult(new ClientResult(start, connected, serverTime, firstResponse, received));
        } catch (InterruptedException iex) {
            LOG.log(Level.INFO, "Interrupted while publishing result.", iex);
        }
    }

    public boolean hasError() {
        return hasError;
    }

    public int getNumberCalls() {
        return numberCalls;
    }

}
