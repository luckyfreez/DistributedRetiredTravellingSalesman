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

// TODO Clean up and document!

public class MasterServer {

    // flights stores all possible flights we might take
    public static ArrayList<Flight> flights = new ArrayList<Flight>();
    private static String crawlerServerAddr = "";


    // Starts the master server.
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


    // Master contacts the slave
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


    // Given a start and ending date, this will return an array of all the dates we have
    public static String[] obtainDates(String start, String end) {

        String[] startParts = start.split("/");
        String[] endParts = end.split("/");
        int startDay = Integer.parseInt(startParts[1]); 
        int startMonth = Integer.parseInt(startParts[0]); 
        int startYear = Integer.parseInt(startParts[2]); 
        int endDay = Integer.parseInt(endParts[1]); 
        int endMonth = Integer.parseInt(endParts[0]); 
        int endYear = Integer.parseInt(endParts[2]); 

        // First, do a bunch of checks.
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

        // Now find all dates (assume Feb is not leap...). TODO Fix for new years and leap years.
        int[] monthDays = new int[] {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        List<String> dates = new ArrayList<String>();
        String currentDate = start;
        dates.add(currentDate);

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
            // System.out.println("New date is: " + currentDate);
            dates.add(currentDate);
        }

        // Finally, convert to array and return.
        String[] allDates = new String[dates.size()];
        return dates.toArray(allDates);
    }


    // Uses the crawler to check the price of a given flight
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


    // Gathers all possible combinations of flights that might be used in integer programming.
    // This only computes the combinations. (The price check happens at checkFlightPrices.)
    public static void gatherFlights(String[] dates, String[] cities) {
        for (String date : dates) {
            for (int x = 0; x < cities.length; x++) {
                for (int y = 0; y < cities.length; y++) {
                    if (x != y) {
                        Flight possibleFlight = new Flight(cities[x], cities[y], date);
                        flights.add(possibleFlight);
                    }
                }
            }
        }
    }


    // This method checks all the flight prices.
    // TODO Change this so that it can distribute across machines.
    public static void checkFlightPrices() {
        for (Flight f : flights) {
            Object[] queryResult = checkPrice(f.from, f.to, f.depDate);
            if ((Boolean) queryResult[0]) {
                f.price = Integer.parseInt((String) queryResult[1]);
            } else {
                f.price = 100000; // Need to get some value here just in case.
            }
            System.out.println(f);
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
    // This is a very important method! Must double check and make sure it is correct!
    public static int[][] obtainConstraints(String[] cities, String[] dates) {

        // If n cities and m days, if we don't worry about cycles, then this means we have n+n+m total constraints (i.e., arrays)
        int numCities = cities.length;
        int numDays = dates.length;
        int equationLength = flights.size() + 2;
        int extraCycleConstraints = 0;
        int extraLogicalConstraints = (numDays-1) * numCities;

        // Okay, let's do cycles. With 4 cities, we have 3 extra cycles to look after
        // E.g., cycles[0] means we have first/second city in one group and must leave out of them to get to other group
        String[][] cycles = new String[3][2];
        if (numCities == 4) {
            extraCycleConstraints = 3;
            cycles[0] = new String[] {cities[0], cities[1]};
            cycles[1] = new String[] {cities[0], cities[2]};
            cycles[2] = new String[] {cities[0], cities[3]};
        }

        // Variables are going to be listed according to their appearance in 'flights' list
        // Everything is zero by default, and we'll make certain coefficients one later. Double check # of equations!
        int[][] constraints = new int[numCities + numCities + numDays + extraCycleConstraints + extraLogicalConstraints][equationLength];
       
        // First set: must make sure only one flight per day NOTE: We set things to be MINUS ONE because we want >=
        for (int d = 0; d < numDays; d++) {
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
        
        // Okay, so I lied. Let's worry about cycles!
        additiveFactor += numCities;
        for (int i = 0; i < extraCycleConstraints; i++) {
            int[] cycleConstraint = new int[equationLength];
            String[] oneGroup = cycles[i];           
            for (int x = 0; x < flights.size(); x++) {
                String startCity = flights.get(x).from;
                String endCity = flights.get(x).to;
                if (Arrays.asList(oneGroup).contains(startCity) && !Arrays.asList(oneGroup).contains(endCity)) {
                    cycleConstraint[x] = 1;
                }
            }
            cycleConstraint[equationLength-2] = 1;
            cycleConstraint[equationLength-1] = 1;
            constraints[i + additiveFactor] = cycleConstraint;
        }

        // Now let's fix the whole back-to-back issue, ASSUMING that we have 4 cities for 4 days (or more generally, n for n days)
        additiveFactor += extraCycleConstraints;
        int index = 0;
        for (int d = 0; d < numDays-1; d++) { // Do numDays-1 since the last day doesn't matter in this sense
            for (int c = 0; c < numCities; c++) {
                String day = dates[d];
                String nextDay = dates[d+1];
                String destCity = cities[c];

                // On day d, went to city c. Now on day d+1, must LEAVE from city c
                int[] logicalConstraint = new int[equationLength];
                for (int x = 0; x < flights.size(); x++) {
                    Flight f = flights.get(x);
                    if (f.depDate.equals(day) && f.to.equals(destCity)) {
                        logicalConstraint[x] = 1;
                    } else if (f.depDate.equals(nextDay) && !f.from.equals(destCity)) {
                        logicalConstraint[x] = 1;
                    }
                }
                logicalConstraint[equationLength-2] = 0; // Because it's LESS THAN or equal...
                logicalConstraint[equationLength-1] = 1;
                constraints[index + additiveFactor] = logicalConstraint;
                index++;
            }
        }

        // Sanity check
        if (index + additiveFactor != constraints.length) {
            System.out.println("Something went wrong, we didn't fill in all equations.");
            System.exit(-1);
        }
        return constraints;
    }


    // Input: crawlerServer hostname. Defaults to local hostname.
    public static void main(String[] args) {

        crawlerServerAddr = "http://" + ((args.length > 0) ? args[0] : "localhost");
        MasterServer.startServer();

        // Input file has start/end dates, followed by each city to travel. All info in its own line.
        String startDate = "06/09/2014";
        String endDate = "06/12/2014";
        String dates[] = obtainDates(startDate, endDate);
        String[] cities = new String[] { "BOS", "SFO", "ORD", "SEA"};
        // String startAndEndCity = "BOS"; // TODO Get a starting city up?

        // Using this information, now we generate all possible flights and check their prices.
        gatherFlights(dates, cities);
        System.out.println("\nNow checking flight prices...");
        checkFlightPrices();

        // Do the integer programming based on the data in the arraylist flights.
        System.out.println("\nNow converting to an integer programming problem...");
        int[] costs = obtainCosts();
        int[][] constraints = obtainConstraints(cities, dates);
        System.out.println("Here are the problem inputs:\n");
        System.out.println("The costs: " + Arrays.toString(costs) + ", and the constraints:\n");
        for (int[] eq : constraints) {
            System.out.println(Arrays.toString(eq));
        }
        
        System.out.println("\nNow calling Balas' algorithm...");
        Balas newProblem = new Balas(constraints, costs, flights);
        newProblem.solve();
        System.out.println("\nDONE!");

    }
}
