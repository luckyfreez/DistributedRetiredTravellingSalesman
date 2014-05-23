/*
 * The master server and the workhorse in terms of calling the slaves and getting the constraints set up.
 * TODO I think we need to add more assertions and sanity checks just to make debugging easier (~Daniel)
 * (c) 2014 by Daniel Seita and Lucky Zhang
 */

import java.util.*;
import java.lang.*;
import java.text.*;

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

    public static ArrayList<Flight> flights = new ArrayList<Flight>();      // All possible flights 
    public static HashSet<String> idleSlaveServers = new HashSet<String>(); // For the multiple crawler servers
    public static String crawlerServerAddr = "";                            // Used to connect to crawler

    // Just gets the crawlerServer hostname and starts the master. NEW: now we have multiple servers!
    public static void main(String[] args) {
        crawlerServerAddr = "http://" + ((args.length > 0) ? args[0] : "localhost" + ":8001");
        idleSlaveServers.add(crawlerServerAddr);
        /*
        crawlerServerAddr = "http://" + ((args.length > 0) ? args[0] : "localhost" + ":8002");
        idleSlaveServers.add(crawlerServerAddr);
        crawlerServerAddr = "http://" + ((args.length > 0) ? args[0] : "localhost" + ":8003");
        idleSlaveServers.add(crawlerServerAddr);
        crawlerServerAddr = "http://" + ((args.length > 0) ? args[0] : "localhost" + ":8004");
        idleSlaveServers.add(crawlerServerAddr);
        crawlerServerAddr = "http://" + ((args.length > 0) ? args[0] : "localhost" + ":8005");
        idleSlaveServers.add(crawlerServerAddr);
        crawlerServerAddr = "http://" + ((args.length > 0) ? args[0] : "localhost" + ":8006");
        idleSlaveServers.add(crawlerServerAddr);
        */
        MasterServer.startServer();
    }


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
            System.out.println("Master server has started!");
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


    /**
     * TODO: Add comments here!
     */
    private static class CheckPriceThread implements Runnable {
        private Flight flight;
        private String crawlerServerAddr;
        private HashSet<String> idleSlaveServers;

        public CheckPriceThread(Flight flight, String crawlerServerAddr, HashSet<String> idleSlaveServers) {
            this.flight = flight;
            this.crawlerServerAddr = crawlerServerAddr;
            this.idleSlaveServers = idleSlaveServers;
        }

        // Uses the crawler to check the price of a given flight
        public void run() {

            XmlRpcClient client = getClient(crawlerServerAddr);

            Object[] params = new Object[3];
            params[0] = flight.from;
            params[1] = flight.to;
            params[2] = flight.depDate;

            // Initialize the default price (Returning this in the end indicating that the search fails).
            Object[] result = new Object[] { false, -1, "", "", "" };

            try {
                result = (Object[]) client.execute("crawlerServer.checkPrice", params);
            } catch (Exception e) {
                System.err.println("ERROR: MasterServer cannot get price for flight " +
                    flight.from + " -> " + flight.to + " at " + flight.depDate + "! Exception is " + e);
            }

            if ((Boolean) result[0]) {
                flight.price = Integer.parseInt((String) result[1]);
            } else {
                // TODO Need to get some value here just in case, improve code to equip price failures
                flight.price = 100000; 
            }

            System.out.println("Finished checking. Flight information: " + flight.toString());
            idleSlaveServers.add(crawlerServerAddr);
            return;
        }
    }


    /**
     * This method checks all the flight prices. TODO Change this so that it can distribute across machines.
     * TODO What happens when we have (1) no flight between two cities on same day, or (2) time-out/hanging? 
     * TODO Sometimes checking flight prices will loop around and skip last part ... no idea what's going on.
     */
    public static void checkFlightPrices() {

        // Can we keep track of the number of idle servers?
        int numSlaveServers = idleSlaveServers.size();
        System.out.println("Checking flight prices with " + numSlaveServers + " slave servers.");

        for (Flight f : flights) {
            // Wait till we find an idle slave server.
            // System.out.println("In loop, last flight: " + fPrevious);}
            while (idleSlaveServers.isEmpty()) { try {Thread.sleep(100);} catch (Exception e) {} }
            String crawlerServerAddr = idleSlaveServers.iterator().next();
            idleSlaveServers.remove(crawlerServerAddr);
            Runnable checkPrice = new CheckPriceThread(f, crawlerServerAddr, idleSlaveServers);
            new Thread(checkPrice).start();
        }

        System.out.println("Now done iterating through flights. Current idle slave servers: " + idleSlaveServers.size());
        int previous = idleSlaveServers.size();
        // Important! We can go through the flights but the last one in each thread will be -1
        // So here we'll just do nothing and wait for everything to finish.
        while (idleSlaveServers.size() != numSlaveServers) {
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ie) {}
            System.out.println("Num idle: " + idleSlaveServers.size());
                }
        System.out.println("After while loop with " + idleSlaveServers.size() + " idle servers.");
        return;
    }


    /**
     * Gathers all possible combinations of flights that might be used in integer programming.
     * This only computes the combinations. (The price check happens at checkFlightPrices.)
     */
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


    /**
     * Obtains the cost (much easier than the constraints)
     */
    public static int[] obtainCosts() {
        int[] costs = new int[flights.size()];
        for (int i = 0; i < flights.size(); i++) {
            Flight f = flights.get(i);
            costs[i] = f.price;
        }
        return costs;
    }


    /**
     * This converts the flight information into the int[][] of constraints. A VERY, VERY important method!
     * (1) With n cities and m days, we have n+n+m constraints regarding one flight a day and entering/leaving cities
     * (2) Prevent disjoint cycles. Use power set to generate all possible groupings, then choose those with at least 2 in each
     * (3) Flight logic: we only check any pairs of flights on consecutive days here. (Defer rest to other parts of code.)
     * Note: if you're going to modify constraints, be VERY CAREFUL and CHECK INDICES, etc.
     */
    public static int[][] obtainConstraints(String[] cities, String[] dates) {

        int numCities = cities.length;
        int numDays = dates.length;
        int equationLength = flights.size() + 2;
        int extraLogicalConstraints = (numDays-1) * numCities;

        // (For disjoint cycle constraints) Generates all possible permutations; use helper methods to get power set
        // Then we'll remove all those that have a set of size one or >= numCities-1 (both will be useless)
        List<ArrayList<String>> cityGroups = powerSet(cities);
        for (int i = cityGroups.size()-1; i >= 0; i--) {
            if ((cityGroups.get(i).size() <= 1) || (cityGroups.get(i).size() >= numCities-1)) {
                cityGroups.remove(i);
            }
        }
        System.out.println("Here is our power set of cities with at least 2 and 2 in the subsets: " + cityGroups);
        // Now we know how many constraints to add
        int extraCycleConstraints = cityGroups.size();

        // Variables are going to be listed according to their appearance in 'flights' list
        // Everything is zero by default, and we'll make certain coefficients one later.
        int[][] constraints = new int[numCities + numCities + numDays + extraCycleConstraints + extraLogicalConstraints][equationLength];
       
        // Constraint (1a): each day must have at most one flight.
        for (int d = 0; d < numDays; d++) {
            String date = dates[d];
            int[] dateEquation = new int[equationLength];
            for (int i = 0; i < flights.size(); i++) {
                String flightDate = flights.get(i).depDate;
                if (flightDate.equals(date)) {
                    dateEquation[i] = 1;
                }
            }
            dateEquation[equationLength-2] = 0;
            dateEquation[equationLength-1] = 1;
            constraints[d] = dateEquation;
        }
        int additiveFactor = numDays; // This is key to keep the indexing consistent!

        // Constraint (1b): we must enter each city at least once (i.e., for each 'j'...)
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
        additiveFactor += numCities;

        // Constraint (1c): we must leave each city at least once (i.e., for each 'i'...)
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
        additiveFactor += numCities;

        // Constraint (2): prevent disjoint cycles from appearing, using our 'oneGroup' list from earlier
        // I think this adds twice as many cycle constraints as is necessary, but its if our DFS tree gets smaller
        for (int i = 0; i < extraCycleConstraints; i++) {
            int[] cycleConstraint = new int[equationLength];
            List<String> oneGroup = cityGroups.get(i);           
            for (int x = 0; x < flights.size(); x++) {
                String startCity = flights.get(x).from;
                String endCity = flights.get(x).to;
                if (oneGroup.contains(startCity) && !oneGroup.contains(endCity)) {
                    cycleConstraint[x] = 1;
                }
            }
            cycleConstraint[equationLength-2] = 1;
            cycleConstraint[equationLength-1] = 1;
            constraints[i + additiveFactor] = cycleConstraint;
        }
        additiveFactor += extraCycleConstraints;

        // Constraint (3): For any two flights on BACK TO BACK days, they must enter/leave in a way that makes sense
        // This code generalizes for n cities to n days (and even n+1 days, I believe...)
        int index = 0;
        for (int d = 0; d < numDays-1; d++) { // Do numDays-1 since the last day doesn't matter 
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
                logicalConstraint[equationLength-2] = 0;
                logicalConstraint[equationLength-1] = 1;
                constraints[index + additiveFactor] = logicalConstraint;
                index++;
            }
        }

        // Sanity check (we should put more of these around...)
        if (index + additiveFactor != constraints.length) {
            System.out.println("Something went wrong, we didn't fill in all equations.");
            System.exit(-1);
        }
        return constraints;
    }


    /**
     * Returns the power set from the given set by using a binary counter
     * Example: S = {a,b,c}, P(S) = {[], [c], [b], [b, c], [a], [a, c], [a, b], [a, b, c]}
     */
    public static List<ArrayList<String>> powerSet(String[] set) {

        List<ArrayList<String>> power = new ArrayList<ArrayList<String>>();
        int elements = set.length;
        int powerElements = (int) Math.pow(2,elements);

        // Run a binary counter for the number of power elements
        for (int i = 0; i < powerElements; i++) {
            String binary = intToBinary(i, elements);
            ArrayList<String> innerSet = new ArrayList<String>();
            // Convert each digit in the current binary number to the corresponding element in the given set
            for (int j = 0; j < binary.length(); j++) {
                if (binary.charAt(j) == '1')
                    innerSet.add(set[j]);
            }
            power.add(innerSet);
        }
        return power;
    }


    /**
     * Converts the given integer to a String representing a binary number with the specified number of digits
     * Example: when using 4 digits the binary 1 is 0001
     */
    public static String intToBinary(int binary, int digits) {
        String temp = Integer.toBinaryString(binary);
        int foundDigits = temp.length();
        String returner = temp;
        for (int i = foundDigits; i < digits; i++) {
            returner = "0" + returner;
        }
        return returner;
    } 


    /**
     * Helps to compute times in human-readable format. Takes in time in MILLISECONDS
     */
    public static String computeTime(long time) {
        int seconds = (int) (time / 1000) % 60;
        int minutes = (int) ((time / (1000*60)) % 60);
        int hours   = (int) ((time / (1000*60*60)) % 24);
        return "" + hours + ":" + minutes + ":" + seconds + "";
    }


    /**
     * Helper to see if a java string represents an integer
     */
    public static boolean isInteger(String s) {
        try { 
            Integer.parseInt(s); 
        } catch(NumberFormatException e) { 
            return false; 
        }
        // only got here if we didn't return false
        return true;
    }


    /**
     * Called when the user has submitted an optional argument to have some minimum number of days between flights
     * Exits program if there are not enough days in range to satisfy the user's requests.
     */
     public static void checkCityDateLogic(int numCities, int numDays, int minDaysBtwnFlights) {
         if ( ((minDaysBtwnFlights + 1) * (numCities - 1) + 1)  > numDays ) {
             System.out.println("Error: there are not enough days to satisfy the minimum days between flights criteria.");
             System.exit(-1);
         }
     }


    /**
     * NEW! We'll use the Java Date and Calendar libraries to help us out with parsing dates. First, we check
     * to make sure end date is after the start date. Then find the full date range between the two.
     */
    public static String[] obtainDates(String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        List<String> datesList = new ArrayList<String>();

        try {
            Date date1 = sdf.parse(start);
            Date date2 = sdf.parse(end);
            if (date1.after(date2)) {
                System.out.println("Error: the ending date happens before the starting date.");
                System.exit(-1);
            }
            String date = start;
            datesList.add(date);
            while (!date.equals(end)) {
                Calendar c = Calendar.getInstance();
                c.setTime(sdf.parse(date));
                c.add(Calendar.DATE, 1);
                date = sdf.format(c.getTime());
                datesList.add(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String[] datesArray = new String[datesList.size()];
        datesArray = datesList.toArray(datesArray);
        return datesArray;
    }


    /*
     * Important method! This is what the client will call, and the server uses it to solve the problem
     * It's split into a lot of stages so that it's clear what's going on. And filled with helpful prints.
     * Note that 'inputFromClient' can have an optional number at the end to indicate days between flights.
     * TODO We might consider splitting some of this stuff into their own methods later.
     */
    public String[] startProblem(String inputFromClient) {

        // Parse the input from the client
        String[] input = inputFromClient.split(" ");
        int numInputs = input.length;
        String startDate = input[0];
        String endDate = input[1];
        String[] dates = obtainDates(startDate, endDate);

        // Check for the last optional argument to see if it's an integer
        String lastInput = input[numInputs-1];
        int minDaysBtwnFlights = 0;
        if (isInteger(lastInput)) {
            minDaysBtwnFlights = Integer.parseInt(lastInput);
            numInputs--;
            checkCityDateLogic(numInputs-2, dates.length, minDaysBtwnFlights); // Sanity check
        }
        System.out.println("mindays is " + minDaysBtwnFlights);
        String[] cities = new String[numInputs-2];
        for (int i = 0; i < numInputs-2; i++) {
            cities[i] = input[i+2];
        }

        // Sanity check to the client
        System.out.println("Starting the problem! Here's our input:");
        System.out.println("All dates: " + Arrays.toString(dates));
        System.out.println("All cities: " + Arrays.toString(cities));

        // Now generate all possible flights and check their prices. This will take a while! (time it)
        long startTime = System.currentTimeMillis();
        gatherFlights(dates, cities);
        System.out.println("\nNow checking flight prices...");
        checkFlightPrices();
        String flightCheckTime = computeTime(System.currentTimeMillis() - startTime);
        System.out.println("Flight check time in hours:minutes:seconds -- " + flightCheckTime + ".");
        System.exit(-1);

        // Now set up the IP problem based on the list of flights (need to get inputs)
        System.out.println("\nNow converting to an integer programming problem...");
        int[] costs = obtainCosts();
        int[][] constraints = obtainConstraints(cities, dates);
        System.out.println("Here are the problem inputs:\n");
        System.out.println("The costs: " + Arrays.toString(costs) + ", and the " + constraints.length + " constraints:\n");
        for (int[] eq : constraints) {
            System.out.println(Arrays.toString(eq));
        }

        // Solve the IP problem (also time how long it takes)
        System.out.println("\nNow calling Balas' algorithm...");
        Balas newProblem = new Balas(constraints, costs, flights, minDaysBtwnFlights);
        startTime = System.currentTimeMillis();
        List<Flight> best_flights = newProblem.solve();
        String balasTime = computeTime(System.currentTimeMillis() - startTime);

        // Now output meaningful messages to the user and pass the list back to the client.
        System.out.println("Original problem from client was " + inputFromClient);
        System.out.println("Flights ordered by cost: " + best_flights);
        Collections.sort(best_flights);
        System.out.println("Flights ordered by date: " + best_flights);
        String[] result = new String[best_flights.size()];
        for (int i = 0; i < best_flights.size(); i++) {
            result[i] = best_flights.get(i).toString();
        }
        System.out.println("Flight check time in hours:minutes:seconds -- " + flightCheckTime + ".");
        System.out.println("DFS search time in hours:minutes:seconds -- " + balasTime + ".");
        return result;
    }

}
