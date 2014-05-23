/*
 * Flight class. Has comparable so that it's easy to order flights based on departure date
 * (c) 2014 by Daniel Seita (and Lucky Zhang)
 */

import java.util.*;
import java.text.*;

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
        return from + "->" + to + " on " + depDate + ", " + (price != -1 ? "$" + price : "price unknown");
    }

    public int compareTo(Flight f) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Date thisDate = new Date();
        Date otherDate = new Date();
        try {
            thisDate = sdf.parse(depDate);
            otherDate = sdf.parse(f.depDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return thisDate.compareTo(otherDate);
    }

}
