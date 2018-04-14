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
 */
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class KafkaExporter {

    public static void main(String[] args) throws Exception {

        int numargs = args.length;
        if (numargs < 1) {
            System.err.println("You must provide the broker (e.g. broker.hub-gw01.l4lb.thisdcos.directory:9092)");
            System.err.println("Optionally you can also specify the port to listen on (Default is 9093)");
            System.exit(1);
        }

        String brokers = args[0];

        Integer port = 9093;
        if (numargs == 2) {
            port = Integer.parseInt(args[1]);
        }

        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);        
        context.addServlet(new ServletHolder(new MetricsKafkaExample(brokers)), "/");
        server.start();
        server.join();
    }

}
