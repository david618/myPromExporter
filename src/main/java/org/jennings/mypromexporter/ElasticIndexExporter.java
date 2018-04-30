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
package org.jennings.mypromexporter;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class ElasticIndexExporter {

    public static void main(String[] args) throws Exception {

        int numargs = args.length;
        if (numargs < 1 || numargs > 4) {
            System.err.println("You must provide the Elasticsearch url (e.g. http://coordinator.sats-ds01.l4lb.thisdcos.directory:9200)");           
            System.err.println("Optionally you can also specify the exporter port (Default is 9093)");
            System.err.println("If needed you can provide Elasticsearch username and password.");
            System.err.println("For example: ElasticIndexExporter coordinator.sats-ds01.l4lb.thisdcos.directory 9201 elastic changeme");
            System.exit(1);
        }

        String elasticSearchUrl = args[0];

        Integer port = 9201;
        if (numargs == 2) {
            port = Integer.parseInt(args[1]);
        }
        
        String username = "";
        String password = "";
        if (numargs >= 3) {
            username = args[2];
        }
        if (numargs >= 4) {
            password = args[3];
        }
        

        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);        
        context.addServlet(new ServletHolder(new ElasticIndexExporterMetrics(elasticSearchUrl, username, password)), "/metrics");
        server.start();
        server.join();
    }

}
