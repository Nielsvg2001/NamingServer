# NamingServer

git clone https://github.com/Nielsvg2001/NamingServer.git
cd NamingServer
mvn package

/* run nameserver*/
mvn spring-boot:run

/* run Client */
mvn exec:java -Dexec.mainClass=com.example.namingserver.Node
