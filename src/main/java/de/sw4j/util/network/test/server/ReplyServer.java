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
import java.util.logging.Logger;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ReplyServer {

    private static final Logger LOG = Logger.getLogger(ReplyServer.class.getName());

    public static void main(String... args) throws Exception {
        ReplyServer server = new ReplyServer();
        server.start();
    }

    public void start() throws IOException {
        Server server = new ConnectionTimeReplySocketServer(9099);
        while (true) {
            ServerRunnable runnable = server.accept();
            Thread t = new Thread(runnable);
            t.start();
        }
    }

}
