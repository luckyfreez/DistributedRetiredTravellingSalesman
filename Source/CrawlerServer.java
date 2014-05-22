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

	//private static boolean idle;

    // Starts the server.
	public static void startServer(int port) {
		try {
			PropertyHandlerMapping phm = new PropertyHandlerMapping();
			XmlRpcServer xmlRpcServer;
			WebServer server = new WebServer(port);
			xmlRpcServer = server.getXmlRpcServer();
			phm.addHandler("crawlerServer", CrawlerServer.class);
			xmlRpcServer.setHandlerMapping(phm);
			server.start();
			crawler = new ITACrawler();
		} catch (Exception exception) { System.err.println("CrawlerServer: " + exception); }
	}


  public Object[] checkPrice(String from, String to, String depDate) {
  	return crawler.checkPrice(from, to, depDate);
	}

	/*
	public static boolean isIdle() {
		return idle;
	}
	*/

	public static void main(String[] args) {

		int port = 8001;
		if (args.length == 1) {
      port = Integer.parseInt(args[0]);
    }
 		CrawlerServer.startServer(port);
		//CrawlerServer s = new CrawlerServer();
		//System.out.println(s.checkPrice("BOS", "NYC", "05/29/2014")[0]);
		/*
		try {
  		System.out.println("Price = " + crawler.checkPrice("BOS", "NYC", "05/29/2014")[0]);
  	} catch (Exception e) {
  	}
  	*/
	}
}
