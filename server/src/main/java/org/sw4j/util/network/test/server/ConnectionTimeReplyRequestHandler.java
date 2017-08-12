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
package org.sw4j.util.network.test.server;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ConnectionTimeReplyRequestHandler implements RequestHandler {

    private static final Logger LOG = Logger.getLogger(ConnectionTimeReplyRequestHandler.class.getName());

    private final Integer payloadSize;

    public ConnectionTimeReplyRequestHandler(Integer payloadSize) {
        this.payloadSize = payloadSize;
    }

    @Override
    public StringBuilder handleRequest(StringBuilder request) throws RequestHandlerException {
        return this.handleRequest(request, Instant.now());
    }

    @Override
    public StringBuilder handleRequest(StringBuilder request, Instant received) throws RequestHandlerException {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        StringBuilder reply = new StringBuilder();

        String[] requestParts = request.toString().split("\n");

        LOG.log(Level.FINEST, "Shortened request");

        Instant sent;
        try {
            sent = Instant.from(dtf.parse(requestParts[0]));
            reply.append(requestParts[0]);
        } catch (DateTimeParseException dtpex) {
            LOG.log(Level.WARNING, "Error while parsing the request.", dtpex);
            throw new RequestHandlerException("Error while parsing the request.", dtpex);
        }
        reply.append("\n");

        try {
            reply.append(dtf.format(received));
        } catch (DateTimeException dtex) {
            LOG.log(Level.WARNING, "Error while formating the response.", dtex);
            throw new RequestHandlerException("Error while formating the response.", dtex);
        }
        reply.append("\n");

        StringBuilder log = new StringBuilder();
        log.append(reply);
        Duration duration = Duration.between(sent, received);
        log.append(duration.toString());
        log.append("\n");
        LOG.log(Level.FINE, log.toString());

        if (payloadSize == null) {
            if (requestParts.length > 1) {
                for (int i = 1; i < requestParts.length; i++) {
                    reply.append(requestParts[i]);
                    reply.append("\n");
                }
            }
        } else {
            reply.append("\n");
            byte[] randomPayload = new byte[payloadSize / 4 * 3];
            Random r = new Random();
            r.nextBytes(randomPayload);
            reply.append(Base64.getEncoder().encodeToString(randomPayload));
        }

        return reply;
    }

}
