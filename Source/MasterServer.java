import java.util.ArrayList;
import java.util.HashSet;
import java.util.*;

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

    public static ArrayList<Flight> flights = new ArrayList<Flight>();
    public static HashSet<Flight> flightsToCheckPrice = new HashSet<Flight>();
    private static String crawlerServerAddr = "";

    // Starts the server.
    public static void startServer() {
        try {
            PropertyHandlerMapping phm = new PropertyHandlerMapping();
            XmlRpcServer xmlRpcServer;
            WebServer server = new WebServer(8000);
            xmlRpcServer = server.getXmlRpcServer();
            phm.addHandler("masterServer", MasterServer.class);
            xmlRpcServer.setHandlerMapping(phm);
            server.start();
        } catch (Exception e) {
            System.err.println("ERROR: MasterServer cannot start! Exception is " + e);
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
            System.err.println("ERROR: MasterServer cannot talk to slave at " + urlStr + "! Exception is "+ e);
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
        XmlRpcClient client = getClient(crawlerServerAddr + ":8001");

        Object[] params = new Object[3];
        params[0] = from;
        params[1] = to;
        params[2] = depDate;

        // Initialize the default price (Returning this in the end indicating that the search fails).
        Object[] result = new Object[] { false, -1, "", "", "" };

        try {
            result = (Object[]) client.execute("crawlerServer.checkPrice", params);
        } catch (Exception e) {
            System.err.println("ERROR: MasterServer cannot get price for flight " +
                    from + " -> " + to + " at " + depDate + "! Exception is " + e);
        }
        return result;
    }


    /**
     * Gathers all possible combinations of flights that might be used in integer programming.
     * This only computes the combinations. (The price check happens at checkFlightPrices.)
     */
    public static void gatherFlights(String start, String end, String[] cities) {

        // This does a bunch of preliminary checks.
        // TODO - put this stuff in its own method to check the input dates
        String[] startParts = start.split("/");
        String[] endParts = end.split("/");
        int startDay = Integer.parseInt(startParts[1]); 
        int startMonth = Integer.parseInt(startParts[0]); 
        int startYear = Integer.parseInt(startParts[2]); 
        int endDay = Integer.parseInt(endParts[1]); 
        int endMonth = Integer.parseInt(endParts[0]); 
        int endYear = Integer.parseInt(endParts[2]); 

        if (startYear > endYear) {
            System.out.println("Error: start year is " + startYear + " and end year is " + endYear + ".");
            System.exit(-1);
        } else if (startYear == endYear) {
            if (startMonth > endMonth) {
                System.out.println("Error: start month is " + startMonth + " and end month is " + endMonth + ".");
                System.exit(-1);
            } else if (startMonth == endMonth) {
                if (startDay > endDay) {
                    System.out.println("Error: start day is " + startDay + " and end day is " + endDay + ".");
                    System.exit(-1);
                }
            }
        }

        // Now need to find the dates (assume Feb is not leap...)
        // Let's just add stuff to 'dates' and iterate through it.
        // TODO Fix for new years and leap years.
        int[] monthDays = new int[] {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        List<String> dates = new ArrayList<String>();
        String currentDate = start;
        dates.add(currentDate);

        System.out.println("Start date: " + start + ", End date: " + end);

        while (!currentDate.equals(end)) {
            String[] dateParts = currentDate.split("/");
            int day = Integer.parseInt(dateParts[1]);
            int month = Integer.parseInt(dateParts[0]);
            day++;
            // Remember, assume that month is < than 12...
            if (day > monthDays[month-1]) {
                day = 1;
                month++;
            }
            String newDay = Integer.toString(day);
            String newMonth = Integer.toString(month);
            if (newDay.length() == 1) {
                newDay = "0" + newDay;
            }
            if (newMonth.length() == 1) {
                newMonth = "0" + newMonth;
            }
            currentDate = newMonth + "/" + newDay + "/" + dateParts[2];
            System.out.println("New date is: " + currentDate);
            dates.add(currentDate);
        }

        for (String date : dates) {
            for (int x = 0; x < cities.length; x++) {
                for (int y = 0; y < cities.length; y++) {
                    if (x != y) {
                        Flight possibleFlight = new Flight(cities[x], cities[y], date);
                        flights.add(possibleFlight);
                        flightsToCheckPrice.add(possibleFlight);
                    }
                }
            }
        }
    }


    /**
     * This method checks all the flight prices.
     * TODO Change this so that it can distribute across machines.
     */
    public static void checkFlightPrices() {
        for (Flight f : flights) {
            // System.out.println("Checking price for flight " + f);
            Object[] queryResult = checkPrice(f.from, f.to, f.depDate);
            if ((Boolean) queryResult[0]) {
                f.price = Integer.parseInt((String) queryResult[1]);
                // System.out.println("Success.");
            } else {
                // System.out.println("ERROR ERROR! Could not compute flight price.");
            }
        }
    }


    // Obtains the cost (much easier than the constraints)
    public static int[] obtainCosts() {
        int[] costs = new int[flights.size()];
        for (int i = 0; i < flights.size(); i++) {
            Flight f = flights.get(i);
            costs[i] = f.price;
        }
        return costs;
    }


    // This converts the flight information into the int[][] of constraints
    public static int[][] obtainConstraints(String[] cities, String[] dates) {

        // TODO For now, let's NOT worry about the case when we might have disjoint cycles ... i.e., keep it at three cities for now.
        // If n cities and m days, if we don't worry about cycles, then this means we have n+n+m total constraints (i.e., arrays)
        int numCities = cities.length;
        int numDays = dates.length;
        int equationLength = flights.size() + 2;

        // Variables are going to be listed according to their appearance in 'flights' list
        // Everything is zero by default, and we'll make certain coefficients one later
        int[][] constraints = new int[numCities + numCities + numDays][equationLength];
       
        // First set: must make sure only one flight per day NOTE: We set things to be MINUS ONE because we want >=
        for (int d = 0; d < dates.length; d++) {
            String date = dates[d];
            int[] dateEquation = new int[equationLength];
            for (int i = 0; i < flights.size(); i++) {
                String flightDate = flights.get(i).depDate;
                if (flightDate.equals(date)) {
                    dateEquation[i] = -1;
                }
            }
            dateEquation[equationLength-2] = 1; // We want >= 1 so set this to be 1
            dateEquation[equationLength-1] = -1; // Because we set things to be >= 1
            constraints[d] = dateEquation;
        }

        // Second set: must make sure we enter each city at least once (i.e., for each 'j'...)
        // Note here that we start at index of dates.length so that we don't overwrite the older equations
        int additiveFactor = numDays;
        for (int j = 0; j < numCities; j++) {
            int[] cityEquation = new int[equationLength];
            for (int x = 0; x < flights.size(); x++) {
                String endCity = flights.get(x).to;
                if (endCity.equals(cities[j])) {
                    cityEquation[x] = 1;
                }
            }
            cityEquation[equationLength-2] = 1;
            cityEquation[equationLength-1] = 1;
            constraints[j + additiveFactor] = cityEquation;
        }

        // Third set: must make sure we leave each city at least once (i.e., for each 'i'...)
        additiveFactor += numCities;
        for (int i = 0; i < numCities; i++) {
            int[] cityEquation = new int[equationLength];
            for (int x = 0; x < flights.size(); x++) {
                String startCity = flights.get(x).from;
                if (startCity.equals(cities[i])) {
                    cityEquation[x] = 1;
                }
            }
            cityEquation[equationLength-2] = 1;
            cityEquation[equationLength-1] = 1;
            constraints[i + additiveFactor] = cityEquation;
        }

        return constraints;
    }


    // Input: crawlerServer hostname. Defaults to local hostname.
    public static void main(String[] args) {

        crawlerServerAddr = "http://" + ((args.length > 0) ? args[0] : "localhost");
        MasterServer.startServer();

        // Input file has start/end dates, followed by each city to travel. All info in its own line.
        String startDate = "06/03/2014";        
        String endDate = "06/05/2014";        
        String[] cities = new String[] { "NYC", "SFO", "ORD"};

        // Using this information, now we generate all possible flights and check their prices.
        gatherFlights(startDate, endDate, cities);
        System.out.println("Here are all the flights we have now:");
        for (Flight f : flights) {
            System.out.println(f);
        }
        System.out.println("\nNow checking flight prices...\n");
        checkFlightPrices();

        System.out.println("\nFinish checking the flight prices...\n");
        System.out.println("Here are all the flights we have now:");
        for (Flight f : flights) {
            System.out.println(f);
        }

        // Do the integer programming based on the data in the arraylist flights.
        System.out.println("\nNow converting to an integer programming problem...\n");
        int[] costs = obtainCosts();

        // TODO Actually if we had THIS, that would make things A LOT easier! Fix the code up!
        String[] dates = new String[] { "06/03/2014", "06/04/2014", "06/05/2014" }; 
        int[][] constraints = obtainConstraints(cities, dates);

        System.out.println("\nHere are the problem inputs:\n");
        System.out.println(Arrays.toString(costs) + "\n");
        for (int[] eq : constraints) {
            System.out.println(Arrays.toString(eq));
        }
        
        System.out.println("\nNow calling Balas' algorithm...\n");
        Balas newProblem = new Balas(constraints, costs, flights);
        newProblem.solve();

        System.out.println("\nDONE!\n");
        // TODO What if we don't have a flight from city i to j on day t? Will need to work around those cases...
    }
}
