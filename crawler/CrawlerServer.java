// Import for client
import java.net.URL;
import org.apache.xmlrpc.*;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

// Import for server
import org.apache.xmlrpc.webserver.WebServer;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.XmlRpcException;

public class CrawlerServer {

	private static ITACrawler crawler;

    // Starts the server.
	public static void startServer() {
		try {
			PropertyHandlerMapping phm = new PropertyHandlerMapping();
			XmlRpcServer xmlRpcServer;
			WebServer server = new WebServer(8593);
			xmlRpcServer = server.getXmlRpcServer();
			phm.addHandler("crawlerServer", CrawlerServer.class);
			xmlRpcServer.setHandlerMapping(phm);
			server.start();
			crawler = new ITACrawler();
		} catch (Exception exception) { System.err.println("CrawlerServer: " + exception); }
	}


  public Object[] checkPrice(String from, String to, String depDate) {
  	Object[] result;
		try {
  		result = crawler.checkPrice("BOS", "NYC", "05/29/2014");
  	} catch (Exception e) {
  		result = checkPrice(from, to, depDate);
  	}
  	return result;
	}

	public static void main(String[] args) {

		CrawlerServer.startServer();
		/*
		try {
  		System.out.println("Price = " + crawler.checkPrice("BOS", "NYC", "05/29/2014")[0]);
  	} catch (Exception e) {
  	}
  	*/
	}
}
