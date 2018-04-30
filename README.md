# myPromExporter
Project to Learn how to Create a Prometheus Exporter


## Run Command Line (Kafka Topic Exporter)

You can run at command line.

```
java -cp target/myPromExporter.jar org.jennings.mypromexporter.KafkaTopicExporter broker.hub-gw01.l4lb.thisdcos.directory:9092
```

## Configure Promethesus 

Added this to the scrape_configs in promethesus.yml.

```
  - job_name: 'a4iot_exporters'
    static_configs:
      - targets: ['p1:9308','p1:9201']  
```

## Query 

Kafka Topic Exporter returns gauge `my_prom_exporter_kafka_topics`.   

A child gauge is added to the gauge fro each topic.

Sample Queries:

```
Raw Value:
my_prom_exporter_kafka_topics

Rate: 
sum by (topic)(irate(my_prom_exporter_kafka_topics[5m]))

Use filter to limit to single topic.

Rate for Topic ext-kafka-a1-planes-json-in
 sum by (topic)(irate(my_prom_exporter_kafka_topics{topic="ext-kafka-a1-planes-json-in"}[5m]))

```

## Create Systemd Service

Create Folder (e.g. ls -l /opt/prometheus/kafka_exporter/)

Put the full jar (myPromExporter-full.jar) in the folder.
Create Environment Variable File in the folder: kafka_exporter_env

Contents of kafka_exporter_env
```
BROKER=broker.hub-gw01.l4lb.thisdcos.directory:9092
```

Create `cat /etc/systemd/system/a4iot_kafka_topic_exporter.service`.

```
[Unit]
Description=a4iot Kafka Topic Explorer
After=network.target

[Service]
Type=simple
User=prometheus
Group=prometheus

EnvironmentFile=/opt/prometheus/a4iot_exporter/kafka_exporter_env
ExecStart=
ExecStart=/bin/java -cp /opt/prometheus/a4iot_exporter/myPromExporter-full.jar org.jennings.mypromexporter.KafkaTopicExporter ${BROKER}

[Install]
WantedBy=multi-user.target
```

You should be able to start and enable the service.

```
systemctl enable kafka_exporter
systemctl start kafka_exporter
```

Now the exporter will run as a service.


## Elastic Index Exporter

This is very similaar to the Kafka Topic Exporter.

### Command Line

```
java -cp target/myPromExporter.jar org.jennings.mypromexporter.ElasticIndexExporter http://coordinator.sats-ds01.l4lb.thisdcos.directory:9200
```

### Query

```
my_prom_exporter_elasticsearch_indices


sum by (index)(irate(my_prom_exporter_elasticsearch_indices[5m]))


```


### systemd Service

```
cat /etc/systemd/system/a4iot_elastic_index_exporter.service
[Unit]
Description=a4iot Elastic Index Explorer
After=network.target

[Service]
Type=simple
User=prometheus
Group=prometheus

EnvironmentFile=/opt/prometheus/a4iot_exporter/elastic_exporter_env
ExecStart=
ExecStart=/bin/java -cp /opt/prometheus/a4iot_exporter/myPromExporter-full.jar org.jennings.mypromexporter.ElasticIndexExporter ${ELASTICURL} ${PORT} ${USERNAME} ${PASSWORD}

[Install]
WantedBy=multi-user.target
```

