
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



