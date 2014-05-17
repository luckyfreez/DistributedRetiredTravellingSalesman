/*
 * Flight class
 *
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
        if (ourDate[2] != otherDate[2]) {
            return Integer.parseInt(ourDate[2]) - Integer.parseInt(otherDate[2]);
        } else if (ourDate[0] != otherDate[0]) {
            return Integer.parseInt(ourDate[0]) - Integer.parseInt(otherDate[0]);
        } else {
            return Integer.parseInt(ourDate[1]) - Integer.parseInt(otherDate[1]);
        }
    }
}
