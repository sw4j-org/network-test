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
package de.sw4j.util.network.test.report.xml;

import de.sw4j.util.network.test.common.ClientResult;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ResultReader {

    private final List<ClientResult> finalResults = new LinkedList<>();

    private final AtomicReference<List<ClientResult>> results = new AtomicReference<>(new LinkedList<ClientResult>());

    private final XMLEventReader xmlReader;

    public ResultReader(InputStream inputStream) throws UnsupportedEncodingException, FactoryConfigurationError,
            XMLStreamException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        this.xmlReader = XMLInputFactory.newFactory().createXMLEventReader(reader);
    }

    public synchronized List<ClientResult> getFinalResult() {
        getIntermediateResult();
        return this.finalResults;
    }

    public List<ClientResult> getIntermediateResult() {
        List<ClientResult> intermediateResults = this.results.getAndSet(new LinkedList<>());
        this.finalResults.addAll(intermediateResults);
        return intermediateResults;
    }

    public synchronized void readData() throws XMLStreamException {
        while (this.xmlReader.hasNext()) {
            XMLEvent event = this.xmlReader.nextEvent();
            if (event.isStartElement() && "result".equals(event.asStartElement().getName().getLocalPart())) {
                StartElement resultElement = event.asStartElement();
                ClientResult.Builder clientResultBuilder = new ClientResult.Builder();

                Attribute attribute = resultElement.getAttributeByName(QName.valueOf("start"));
                if (attribute != null) {
                    clientResultBuilder.setStart(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                            attribute.getValue())));
                }

                attribute = resultElement.getAttributeByName(QName.valueOf("connected"));
                if (attribute != null) {
                    clientResultBuilder.setConnected(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                            attribute.getValue())));
                }

                attribute = resultElement.getAttributeByName(QName.valueOf("serverReceived"));
                if (attribute != null) {
                    clientResultBuilder.setServerReceived(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                            attribute.getValue())));
                }

                attribute = resultElement.getAttributeByName(QName.valueOf("firstResponse"));
                if (attribute != null) {
                    clientResultBuilder.setFirstResponse(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                            attribute.getValue())));
                }

                attribute = resultElement.getAttributeByName(QName.valueOf("completed"));
                if (attribute != null) {
                    clientResultBuilder.setCompleted(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                            attribute.getValue())));
                }

                this.results.get().add(clientResultBuilder.build());
            }
        }
    }

}
