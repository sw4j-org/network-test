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
package org.sw4j.util.network.test.client;

import org.sw4j.util.network.test.common.ClientResult;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
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

    private final Integer payloadSize;

    private final String serverHost;

    private final int serverPort;

    private final long threads;

    private int numberCalls;

    private final List<ResultCollector> collectors;

    public ConnectionTimeReplyClientSocketRunner(long threads, String serverHost, int serverPort, Integer payloadSize) {
        this.threads = threads;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.payloadSize = payloadSize;
        LOG.log(Level.INFO, new StringBuilder("Number of threads: ").append(this.threads).append("\n").toString());
        this.collectors = new LinkedList<>();
    }

    public ConnectionTimeReplyClientSocketRunner(long threads, String serverHost, int serverPort, Integer payloadSize,
            ResultCollector collector) {
        this(threads, serverHost, serverPort, payloadSize);
        this.collectors.add(collector);
    }

    public void addCollector(ResultCollector collector) {
        this.collectors.add(collector);
    }

    public void removeCollector(ResultCollector collector) {
        this.collectors.remove(collector);
    }

    private void queueReslts(ClientResult result) throws InterruptedException {
        for(ResultCollector collector: this.collectors) {
            collector.queueResult(result);
        }
    }

    @Override
    public void run() {
        ExecutorService threadPool = Executors.newWorkStealingPool();

        for (long i = 0; i < this.threads; i++) {
            threadPool.execute(() -> {
                communicate();
            });
        }

        threadPool.shutdown();
        numberCalls++;
    }

    private void communicate() {
        ClientResult.Builder clientResultBuilder = new ClientResult.Builder();
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        Instant start = Instant.now();
        clientResultBuilder.setStart(start);

        Socket socket = null;
        Instant connected = null;
        try {
            socket = new Socket(InetAddress.getByName(this.serverHost), this.serverPort);
            connected = Instant.now();
            clientResultBuilder.setConnected(connected);
        } catch (IOException ioex) {
            LOG.log(Level.INFO, "Error while creating socket.", ioex);
        }

        char[] response = new char[1024];
        StringBuilder requestSb = new StringBuilder();
        StringBuilder responseSb = new StringBuilder();
        Writer requestWriter = null;
        Reader responseReader = null;
        if (socket != null) {
            try {
                requestWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
                responseReader = new InputStreamReader(socket.getInputStream(), "UTF-8");
            } catch (IOException ioex) {
                LOG.log(Level.INFO, "Error while getting input or output stream.", ioex);
                try {
                    socket.close();
                } catch (IOException ioex2) {
                    LOG.log(Level.FINE, "Ignoring exception during socket close.", ioex2);
                }
            }
        }

        if (connected != null) {
            try {
                requestSb.append(dtf.format(connected));
            } catch (DateTimeException dtex) {
                LOG.log(Level.FINE, "Error while formating the request.", dtex);
            }
        }

        if (payloadSize != null) {
            requestSb.append("\n");
            byte[] randomPayload = new byte[payloadSize / 4 * 3];
            Random r = new Random();
            r.nextBytes(randomPayload);
            requestSb.append(Base64.getEncoder().encodeToString(randomPayload));
        }

        requestSb.append("\n\n.\n");
        if (requestWriter != null) {
            try {
                requestWriter.append(requestSb);
                requestWriter.flush();
            } catch (IOException ioex) {
                LOG.log(Level.INFO, "Error while writing request.", ioex);
            }
        }

        Instant received = null;
        if (responseReader != null) {
            try {
                Instant firstResponse = null;
                int byteRead;
                while ((byteRead = responseReader.read(response)) >= 0) {
                    if (firstResponse == null) {
                        firstResponse = Instant.now();
                        clientResultBuilder.setFirstResponse(firstResponse);
                    }
                    responseSb.append(response, 0, byteRead);
                }
                received = Instant.now();
                clientResultBuilder.setCompleted(received);
            } catch (IOException ioex) {
                LOG.log(Level.INFO, "Error while receiving request.", ioex);
            }
        }


        String[] responseParts = responseSb.toString().split("\n");
        Instant serverTime = null;
        try {
            if (responseParts.length >= 2) {
                serverTime = Instant.from(dtf.parse(responseParts[1]));
                clientResultBuilder.setServerReceived(serverTime);
            }
        } catch (DateTimeParseException dtpex) {
            LOG.log(Level.INFO, "Error while parsing the response.", dtpex);
        }

        responseSb = new StringBuilder();
        if (connected != null) {
            responseSb.append(dtf.format(connected)).append("\n");
        }
        if (serverTime != null) {
            try {
                responseSb.append(dtf.format(serverTime)).append("\n");
            } catch (DateTimeException dtex) {
                LOG.log(Level.INFO, "Error while formating server time.", dtex);
            }
        }

        if (received != null) {
            try {
                responseSb.append(dtf.format(received));
            } catch (DateTimeException dtex) {
                LOG.log(Level.INFO, "Error while formating received time.", dtex);
            }
        }
        responseSb.append("\n");

        ClientResult result = clientResultBuilder.build();
        if (result.getConnectTime() != null) {
            responseSb.append("Connect Time: ").append(result.getConnectTime().toString()).append("\n");
        }
        if (result.getServerReceivedTime() != null) {
            responseSb.append("Server Receive: ").append(result.getServerReceivedTime().toString()).append("\n");
        }
        if (result.getLatency() != null) {
            responseSb.append("Latency: ").append(result.getLatency().toString()).append("\n");
        }
        if (result.getResponseTime() != null) {
            responseSb.append("Response Time: ").append(result.getResponseTime().toString()).append("\n");
        }
        LOG.log(Level.FINE, responseSb.toString());

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ioex) {
                LOG.log(Level.FINE, "Ignoring exception during socket close.", ioex);
            }
        }

        try {
            queueReslts(result);
        } catch (InterruptedException iex) {
            LOG.log(Level.INFO, "Interrupted while publishing result.", iex);
        }
    }

    public int getNumberCalls() {
        return numberCalls;
    }

}
