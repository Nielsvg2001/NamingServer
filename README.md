#TO DO
Replication:

send logs with file if replicated file is transfered to other node
shutdown: allert all the replicated nodes that the local file is deleted -> die moeten in log kijken en dan kijken of die op een andere plaats gedownlodad is, zoja, pas de log aan, anders mag de file en log verwijderd worden
zorgen shutdown werkt
Abe zijn stuk testen : als node joint, 

Agents:
alles



#tips:
niet touch gebruiken -> lege file aangemaakt, maar dat wordt niet aanvaard door listener



# NamingServer

git clone https://github.com/Nielsvg2001/NamingServer.git
cd NamingServer
mvn package

/* run nameserver*/
mvn spring-boot:run

/* run Client */
mvn exec:java -Dexec.mainClass=Node



bash : runNode.sh

#!/bin/sh
rm -R NamingServer
git clone -b Discovery https://github.com/Nielsvg2001/NamingServer.git
cd NamingServer
mvn package
mvn exec:java -Dexec.mainClass=Node

bash runNaming.sh

#!/bin/sh
rm -R NamingServer
git clone -b Discovery https://github.com/Nielsvg2001/NamingServer.git
cd NamingServer
mvn package
mvn spring-boot:run



