all:
	javac Flight.java
	javac Balas.java
	javac -cp .:lib/* ITACrawler.java 
	javac -cp .:lib/xmlrpc-3.1.3/* CrawlerServer.java 
	javac -cp .:lib/xmlrpc-3.1.3/* MasterServer.java 

slave:
	javac -cp .:lib/xmlrpc-3.1.3/* CrawlerServer.java 

master:
	javac -cp .:lib/xmlrpc-3.1.3/* MasterServer.java 

run_slave_print:
	java -ea -cp .:lib/xmlrpc-3.1.3/*:lib/* CrawlerServer

run_slave_8001:
	java -ea -cp .:lib/xmlrpc-3.1.3/*:lib/* CrawlerServer 8001

run_slave_8002:
	java -ea -cp .:lib/xmlrpc-3.1.3/*:lib/* CrawlerServer 8002

run_master_print:
	java -Xmx4096m -ea -cp .:lib/xmlrpc-3.1.3/*:lib/* MasterServer

run_slave:
	java -ea -cp .:lib/xmlrpc-3.1.3/*:lib/* CrawlerServer > /dev/null &

clean:
	rm -f *.class
