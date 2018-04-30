/*
 * (C) Copyright 2017 David Jennings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     David Jennings
 *
 * Extended from https://github.com/prometheus/client_java
 */

package org.jennings.mypromexporter;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import io.prometheus.client.exporter.common.TextFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ExampleExporterMetrics extends HttpServlet {

    private CollectorRegistry registry;

    static Gauge g = Gauge.build().name("gauge").help("blah").register();
    static Counter c = Counter.build().name("counter").help("meh").register();
    static Summary s = Summary.build().name("summary").help("meh").register();
    static Histogram h = Histogram.build().name("histogram").help("meh").register();
    static Gauge l = Gauge.build().name("labels").help("blah").labelNames("l").register();

    /**
     * Construct a MetricsServlet for the default registry.
     */
    public ExampleExporterMetrics() {
        this(CollectorRegistry.defaultRegistry);
    }

    /**
     * Construct a MetricsServlet for the given registry.
     */
    public ExampleExporterMetrics(CollectorRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

        Random rnd = new Random();
        
        g.set(rnd.nextLong());
        c.inc(2);
       
        s.observe(3);
        h.observe(rnd.nextDouble());
        l.labels("foo").inc(5);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(TextFormat.CONTENT_TYPE_004);

        Writer writer = resp.getWriter();
        try {
            TextFormat.write004(writer, registry.filteredMetricFamilySamples(parse(req)));
            writer.flush();
        } finally {
            writer.close();
        }
    }

    private Set<String> parse(HttpServletRequest req) {
        String[] includedParam = req.getParameterValues("name");
        if (includedParam == null) {
            return Collections.emptySet();
        } else {
            return new HashSet<String>(Arrays.asList(includedParam));
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

}
