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
package org.sw4j.util.network.test.common;

import java.time.Duration;
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

    private ClientResult(Instant start, Instant connected, Instant serverReceived, Instant firstResponse,
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

    public Duration getConnectTime() {
        return (this.start != null && this.connected != null) ? Duration.between(this.start, this.connected) : null;
    }

    public Instant getServerReceived() {
        return serverReceived;
    }

    public Duration getServerReceivedTime() {
        return (this.start != null && this.serverReceived != null) ?
                Duration.between(this.start, this.serverReceived) : null;
    }

    public Instant getFirstResponse() {
        return firstResponse;
    }

    public Duration getLatency() {
        return (this.start != null && this.firstResponse != null) ?
                Duration.between(this.start, this.firstResponse) : null;
    }

    public Instant getCompleted() {
        return completed;
    }

    public Duration getResponseTime() {
        return (this.start != null && this.completed != null) ? Duration.between(this.start, this.completed) : null;
    }

    public static class Builder {

        private Instant start;

        private Instant connected;

        private Instant serverReceived;

        private Instant firstResponse;

        private Instant completed;

        public Builder setStart(Instant start) {
            this.start = start;
            return this;
        }

        public Builder setConnected(Instant connected) {
            this.connected = connected;
            return this;
        }

        public Builder setServerReceived(Instant serverReceived) {
            this.serverReceived = serverReceived;
            return this;
        }

        public Builder setFirstResponse(Instant firstResponse) {
            this.firstResponse = firstResponse;
            return this;
        }

        public Builder setCompleted(Instant completed) {
            this.completed = completed;
            return this;
        }

        public ClientResult build() {
            return new ClientResult(start, connected, serverReceived, firstResponse, completed);
        }

    }

}
