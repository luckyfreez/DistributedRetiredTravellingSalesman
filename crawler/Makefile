all:
	javac -cp .:lib/* ITACrawler.java 
	javac -cp .:lib/xmlrpc-3.1.3/* CrawlerServer.java 

run_slave_print:
	java -cp .:lib/xmlrpc-3.1.3/*:lib/* CrawlerServer

run_slave:
	java -cp .:lib/xmlrpc-3.1.3/*:lib/* CrawlerServer > /dev/null &

clean:
	rm -f *.class
