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

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public interface DataReporter {

    /**
     * Adds the given data to the result. This method may be called often to update the display of plan data fast.
     *
     * @param data the data to add.
     */
    void addPartialData(List<ClientResult> data);

    /**
     * Aggregate all data added via {@link #addPartialData(java.util.List)} since the last call. This method should not
     * be called to often as it can involve lobger calculations.
     */
    void aggregatePartialData();

}
