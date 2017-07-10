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
package de.sw4j.util.network.test.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ConnectionTimeReplySocketRunner implements Runnable {

    private static final Logger LOG = Logger.getLogger(ConnectionTimeReplySocketRunner.class.getName());

    private final Socket socket;

    public ConnectionTimeReplySocketRunner(Socket socket) {
        if (socket == null) {
            throw new NullPointerException("Socket may not be null.");
        }
        this.socket = socket;
    }

    @Override
    public void run() {
        char[] request = new char[1024];
        StringBuilder reply = new StringBuilder();
        StringBuilder log = new StringBuilder();
        Writer responseWriter;
        Reader requestReader;
        try {
            responseWriter = new OutputStreamWriter(this.socket.getOutputStream(), "UTF-8");
            requestReader = new InputStreamReader(this.socket.getInputStream(), "UTF-8");
        } catch (IOException ioex) {
            LOG.log(Level.WARNING, "Error while getting input or output stream.", ioex);
            try {
                this.socket.close();
            } catch (IOException ioex2) {
                LOG.log(Level.INFO, "Ignoring exception during socket close.", ioex2);
            }
            return;
        }
        try {
            boolean lineRead = false;
            int byteRead = 0;
            while (!lineRead) {
                byteRead = requestReader.read(request);
                reply.append(request, 0, byteRead);
                int newLinePos = reply.indexOf("\n");
                if (newLinePos >= 0) {
                    reply.delete(newLinePos, reply.length());
                    lineRead = true;
                }
            }
        } catch (IOException ioex) {
            LOG.log(Level.WARNING, "Error while receiving request.", ioex);
            return;
        }
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        Instant sent;
        try {
            sent = Instant.from(dtf.parse(reply));
        } catch (DateTimeParseException dtpex) {
            LOG.log(Level.WARNING, "Error while parsing the request.", dtpex);
            return;
        }
        Instant now = Instant.now();
        reply.append("\n");
        try {
            reply.append(dtf.format(now));
        } catch (DateTimeException dtpex) {
            LOG.log(Level.WARNING, "Error while formating the response.", dtpex);
            return;
        }
        log.append(reply);
        log.append("\n");
        Duration duration = Duration.between(sent, now);
        log.append(duration.toString());
        LOG.info(log.toString());
        try {
            responseWriter.write(reply.toString());
            responseWriter.flush();
        } catch (IOException ioex) {
            LOG.log(Level.WARNING, "Error while sending response.", ioex);
            return;
        }
        try {
            this.socket.close();
        } catch (IOException ioex) {
            LOG.log(Level.INFO, "Ignoring exception during socket close.", ioex);
        }
    }

}
