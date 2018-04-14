package org.jennings.mypromexporter;

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
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MetricsKafkaExample extends HttpServlet {

    private CollectorRegistry registry;

    private static final Logger LOG = LogManager.getLogger(MetricsKafkaExample.class);

    // Create Gauge for Each Topic
    HashMap<String, Gauge> topicGauges = new HashMap<>();
    //ArrayList<Gauge> topicGauges = new ArrayList<>();
    //static Gauge g = Gauge.build().name("gauge").help("blah").register();

    Properties props = new Properties();

    /**
     * Construct a MetricsKafkaExample for the default registry.
     *
     * @param brokers
     */
    public MetricsKafkaExample(String brokers) {
        this(CollectorRegistry.defaultRegistry, brokers);
    }

    /**
     * Construct a MetricsKafkaExample for the given registry.
     *
     * @param registry
     * @param brokers
     */
    public MetricsKafkaExample(CollectorRegistry registry, String brokers) {
        this.registry = registry;

        // https://kafka.apache.org/documentation/#consumerconfigs
        props.put("bootstrap.servers", brokers);
        // Should include another parameter for group.id this would allow differenct consumers of same topic
        props.put("group.id", "abc");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", 1000);
        props.put("auto.offset.reset", "earliest");
        props.put("session.timeout.ms", "10000");
        props.put("request.timeout.ms", "11000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

        Random rnd = new Random();

        //g.set(rnd.nextLong());
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        
        if (topicGauges.containsKey("planes")) {
            // Loop through and remove anything that is no longer a topic
            registry.unregister(topicGauges.get("planes"));
            topicGauges.remove("planes");
            
        }

        Iterator<String> tps = consumer.listTopics().keySet().iterator();
        while (tps.hasNext()) {

            String tp = tps.next();
            LOG.debug(tp);
            //System.out.println(tp);

            List<TopicPartition> partitions = consumer.partitionsFor(tp).stream()
                    .map(p -> new TopicPartition(tp, p.partition()))
                    .collect(Collectors.toList());
            consumer.assign(partitions);
            consumer.seekToEnd(Collections.emptySet());
            Map<TopicPartition, Long> endPartitions = partitions.stream()
                    .collect(Collectors.toMap(Function.identity(), consumer::position));
            Iterator itTP = endPartitions.entrySet().iterator();
            long cnt = 0;
            while (itTP.hasNext()) {
                Map.Entry tpart = (Map.Entry) itTP.next();
                cnt += (long) tpart.getValue();
            }

            if (topicGauges.containsKey(tp)) {
                topicGauges.get(tp).set(cnt);

            } else {
                //System.out.println("NEW");
                Gauge g = Gauge.build().name(tp).help("offset").register();

                g.set(cnt);

                topicGauges.put(tp, g);
            }

        }

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
            return new HashSet<>(Arrays.asList(includedParam));
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

}
