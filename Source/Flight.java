/*
 * Flight class
 * Has comparable so that it's easy to order flights based on departure date
 */

import java.util.*;

public class Flight implements Comparable<Flight> {

    public String from;
    public String to;
    public String depDate;
    public int price;

    public Flight(String from, String to, String depDate) {
        this.from = from;
        this.to = to;
        this.depDate = depDate;
        price = -1;
    }

    public int hashCode() {
        return (from + to + depDate).hashCode();
    }

    public String toString() {
        return from + "->" + to + " on " + depDate + ", " + (price != -1 ? "$" + price : "price not known");
    }

    // Order flights based on the departure date. Greater than means it comes later in the ordering.
    public int compareTo(Flight f) {
        String otherDepDate = f.depDate;
        String[] ourDate = depDate.split("/");
        String[] otherDate = otherDepDate.split("/");
        if (!ourDate[2].equals(otherDate[2])) {
            return ourDate[2].compareTo(otherDate[2]);
        } else if (!ourDate[0].equals(otherDate[0])) {
            return ourDate[0].compareTo(otherDate[0]);
        } else {
            return ourDate[1].compareTo(otherDate[1]);
        }
    }

}
