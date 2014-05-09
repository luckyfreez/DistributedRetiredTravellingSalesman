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

	private static String catalogServerAddr = "";

	private static ITACrawler crawler;

    // Starts the server.
	public static void startServer() {
		try {
			PropertyHandlerMapping phm = new PropertyHandlerMapping();
			XmlRpcServer xmlRpcServer;
			WebServer server = new WebServer(8593);
			xmlRpcServer = server.getXmlRpcServer();
			phm.addHandler("CrawlerServer", CrawlerServer.class);
			xmlRpcServer.setHandlerMapping(phm);
			server.start();
			crawler = new ITACrawler();
		} catch (Exception exception) { System.err.println("CrawlerServer: " + exception); }
	}



    // Input: item number int. Calls the catalog server to buy a book. Returns false if book is out of stock.
	public boolean buy(int itemNum) {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		XmlRpcClient client = null;
		try {
			config.setServerURL(new URL(catalogServerAddr + ":8592"));
			client = new XmlRpcClient();
			client.setConfig(config);
		} catch (Exception e) { System.err.println("Problem! "+ e); }

		Object[] params = new Object[2];
		params[0] = itemNum;
		params[1] = -1;

		try {
			Boolean result = (Boolean) client.execute("catalogServer.changeBookStockCount", params);
			return result;
		} catch (Exception exception) { System.err.println("CrawlerServer Client: " + exception); }
		return false;
	}

    // Input: CatalogServer hostname. Defaults to local hostname.
	public static void main(String[] args) {
		catalogServerAddr = "http://" + ((args.length > 0) ? args[0] : "localhost");

		CrawlerServer.startServer();
		try {
  		System.out.println("Price = " + crawler.checkPrice("BOS", "NYC", "05/29/2014")[0]);
  	} catch (Exception e) {
  	}
	}
}
