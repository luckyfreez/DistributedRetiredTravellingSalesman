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

public class MasterServer {

  private static String crawlerServerAddr = "";
    // Starts the server.
	public static void startServer() {
		try {
			PropertyHandlerMapping phm = new PropertyHandlerMapping();
			XmlRpcServer xmlRpcServer;
			WebServer server = new WebServer(8592);
			xmlRpcServer = server.getXmlRpcServer();
			phm.addHandler("masterServer", MasterServer.class);
			xmlRpcServer.setHandlerMapping(phm);
			server.start();
		} catch (Exception e) {
			System.err.println("ERROR: MasterServer cannot start! " +
  											 "Exception is " + e);
		}
	}


  private static XmlRpcClient getClient(String urlStr) {
    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    XmlRpcClient client = null;
    try {
      config.setServerURL(new URL(urlStr));
      client = new XmlRpcClient();
      client.setConfig(config);
    } catch (Exception e) {
      System.err.println("ERROR: MasterServer cannot talk to slave at " +
                         urlStr + "! Exception is "+ e);
      getClient(urlStr);
    }
    return client;
  }

  /*
  // Not used for now.
  private static boolean checkIsIdle(XmlRpcClient client) {
    checkIsIdle(client, 0);
  }

  private static boolean checkIsIdle(XmlRpcClient client, int attempt) {
    if (attempt >= 5) {
      return false;
    }

    try {
      return (Object[]) client.execute("crawlerServer.isIdle");
    } catch (Exception e) {
      System.err.println("ERROR: MasterServer cannot check if slave at " +
                         client.getClientConfig().getServerUrl() +
                         " is idle! Exception is " + e);
      checkIsIdle(client, attempt + 1);
    }

    return false;
  }
  */

  public static Object[] checkPrice(String from, String to, String depDate) {
    XmlRpcClient client = getClient(crawlerServerAddr + ":8593");

  	Object[] params = new Object[3];
  	params[0] = from;
  	params[1] = to;
  	params[2] = depDate;

  	Object[] result = new Object[5];
  	result[0] = -1;    // Initialize the default price (Returning this in the end indicating that the search fails).

  	try {
  		result = (Object[]) client.execute("crawlerServer.checkPrice", params);
  	} catch (Exception e) {
  		System.err.println("ERROR: MasterServer cannot get price for flight " +
  			                 from + " -> " + to + " at " + depDate +
  			                 "! Exception is " + e);
  	}
  	return result;
  }

    // Input: crawlerServer hostname. Defaults to local hostname.
	public static void main(String[] args) {
		crawlerServerAddr = "http://" + ((args.length > 0) ? args[0] : "localhost");

		MasterServer.startServer();

		System.out.println("Price = " + checkPrice("BOS", "NYC", "05/29/2014")[1]);

	}
}
