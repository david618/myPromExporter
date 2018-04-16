# myPromExporter
Project to Learn how to Create a Prometheus Exporter


## Run Command Line

You can run at command line.

```
java -jar target/myPromExporter.jar broker.hub-gw01.l4lb.thisdcos.directory:9092 &
```

## Configure Promethesus 

Added this to the scrape_configs in promethesus.yml.

```
  - job_name: 'kafka'
    static_configs:
      - targets: ['p1:9093']      
```

## Query 

Kafka Exporter currently returns metrics with the broker prefix.   Turned dashes and periods into underscores.

Sample Queries:

```
Raw Value:
kafka_broker_hub_gw01_l4lb_thisdcos_directory:9092

Rate: 
irate(kafka_broker_hub_gw01_l4lb_thisdcos_directory:9092[5m])

Used label for each topic so you can filter.

Rate for Topic ext-kafka-a1-planes-json-in
irate(kafka_broker_hub_gw01_l4lb_thisdcos_directory:9092{topic="ext-kafka-a1-planes-json-in"}[5m])
```

## Create Systemd Service

Create Folder (e.g. ls -l /opt/prometheus/kafka_exporter/)

Put the full jar (myPromExporter-full.jar) in the folder.
Create Environment Variable File in the folder: kafka_exporter_env

Contents of kafka_exporter_env
```
BROKER=broker.hub-gw01.l4lb.thisdcos.directory:9092
```

Create `/etc/systemd/system/kafka_exporter.service`.

```
[Unit]
Description=Kafka Explorer
After=network.target

[Service]
Type=simple
User=prometheus
Group=prometheus

EnvironmentFile=/opt/prometheus/kafka_exporter/kafka_exporter_env
ExecStart=
ExecStart=/bin/java -jar /opt/prometheus/kafka_exporter/myPromExporter-full.jar ${BROKER}

[Install]
WantedBy=multi-user.target
```

You should be able to start and enable the service.

```
systemctl enable kafka_exporter
systemctl start kafka_exporter
```

Now the exporter will run as a service.

