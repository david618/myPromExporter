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
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.BufferedReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ElasticIndexExporterMetrics extends HttpServlet {

    private CollectorRegistry registry;

    private static final Logger LOG = Logger.getLogger(ElasticIndexExporterMetrics.class);

    Properties props = new Properties();

    String elasticsearchUrl;
    String username;
    String password;
    Gauge g;

    /**
     * Construct a MetricsKafkaExample for the default registry.
     *
     * @param elasticsearchURL
     * @param username
     * @param password
     */
    public ElasticIndexExporterMetrics(String elasticsearchURL, String username, String password) {
        this(CollectorRegistry.defaultRegistry, elasticsearchURL, username, password);
    }

    /**
     * Construct a MetricsKafkaExample for the given registry.
     *
     * @param registry
     * @param elasticsearchURL
     * @param username
     * @param password
     */
    public ElasticIndexExporterMetrics(CollectorRegistry registry, String elasticsearchURL, String username, String password) {
        this.registry = registry;
        this.elasticsearchUrl = elasticsearchURL;
        this.username = username;
        this.password = password;
        g = Gauge.build().name("my_prom_exporter_elasticsearch_indices").help("counts").labelNames("index").register();
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            // Get list of indexes

            // index/type
            SSLContext sslContext = SSLContext.getInstance("SSL");

            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials
                    = new UsernamePasswordCredentials(username, password);
            provider.setCredentials(AuthScope.ANY, credentials);

            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {

                    LOG.debug("getAcceptedIssuers =============");

                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs,
                        String authType) {
                    LOG.debug("checkClientTrusted =============");
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs,
                        String authType) {

                    LOG.debug("checkServerTrusted =============");

                }
            }}, new SecureRandom());

            CloseableHttpClient httpclient = HttpClients
                    .custom()
                    .setDefaultCredentialsProvider(provider)
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            String url = elasticsearchUrl + "/_cat/aliases?format=json";

            HttpGet request = new HttpGet(url);
            CloseableHttpResponse response = httpclient.execute(request);
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            Header contentType = response.getEntity().getContentType();
            int responseCode = response.getStatusLine().getStatusCode();

            String line;
            StringBuilder result = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            JSONArray jsonArray = new JSONArray(result.toString());
            request.abort();
            response.close();

            ArrayList<String> indicesArrayList = new ArrayList<>();

            // Create list of aliases ignore duplicates
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                String alias = json.getString("alias");
                if (indicesArrayList.indexOf(alias) == -1) {
                    indicesArrayList.add(alias);
                }
            }

            Iterator<String> indices = indicesArrayList.iterator();

            while (indices.hasNext()) {
                // Loop through each Index
                String index = indices.next();

                try {
                    LOG.debug(index);

                    // Create Gauge Child and populate with index
                    Gauge.Child g2 = g.labels(index);

                    // Get cnt from index
                    url = elasticsearchUrl + "/" + index + "/_count";

                    request = new HttpGet(url);
                    response = httpclient.execute(request);
                    rd = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent()));

                    result = new StringBuilder();
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject json = new JSONObject(result.toString());
                    request.abort();
                    response.close();

                    Integer cnt = json.getInt("count");

                    g2.set(cnt);

                } catch (Exception e) {
                    LOG.error("ERROR", e);
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

        } catch (IOException | UnsupportedOperationException | KeyManagementException | NoSuchAlgorithmException | JSONException e) {
            LOG.error("ERROR", e);
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
