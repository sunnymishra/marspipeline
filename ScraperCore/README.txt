Kafka default port 9092 
Zookeeper default port 2181
Set ZOOKEEPER_HOME env variable
Start Zookeeper first with command: 
>	zkserver
Start Kafka command: 
>	.\bin\windows\kafka-server-start.bat .\config\server.properties
Stop Kafka command:
> .\bin\windows\kafka-server-stop.bat .\config\server.properties

Producer talks to Kafka server
Consumer talks to Zookeper server

Links: https://dzone.com/articles/running-apache-kafka-on-windows-os