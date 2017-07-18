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

import java.time.Instant;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ClientResult {

    private final Instant start;

    private final Instant connected;

    private final Instant serverReceived;

    private final Instant firstResponse;

    private final Instant completed;

    public ClientResult(Instant start, Instant connected, Instant serverReceived, Instant firstResponse,
            Instant completed) {
        this.start = start;
        this.connected = connected;
        this.serverReceived = serverReceived;
        this.firstResponse = firstResponse;
        this.completed = completed;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getConnected() {
        return connected;
    }

    public Instant getServerReceived() {
        return serverReceived;
    }

    public Instant getFirstResponse() {
        return firstResponse;
    }

    public Instant getCompleted() {
        return completed;
    }

}
