/*
 * The point of this class is that, given a starting and ending date,
 * we can find all the intermediate dates in between. We also use this
 * for ordering a series of flights by departure time
 * (c) 2014 by Daniel Seita
 */

import java.util.*;

public class Dates {

    private static String start;
    private static String end;

    // The masterServer will create one of these.
    public Dates(String start, String end) {
        this.start = start;
        this.end = end;
    }


    // Given a start and ending date, this will return an array of all the dates we have
    public static String[] obtainDates() {
        performChecks();
        List<String> dates = findDates(); 
        String[] allDates = new String[dates.size()];
        return dates.toArray(allDates);
    }


    // Does a reasonable amount of checking to make sure the start date is before the end date (and years are same)
    public static void performChecks() {

        String[] startParts = start.split("/");
        String[] endParts = end.split("/");
        int startDay = Integer.parseInt(startParts[1]); 
        int startMonth = Integer.parseInt(startParts[0]); 
        int startYear = Integer.parseInt(startParts[2]); 
        int endDay = Integer.parseInt(endParts[1]); 
        int endMonth = Integer.parseInt(endParts[0]); 
        int endYear = Integer.parseInt(endParts[2]); 

        if (startYear < endYear) {
            System.out.println("For simplicity, please keep the years the same.");
            System.exit(-1);
        } else if (startYear > endYear) {
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
    }


    // Now find all dates within a given range. Note that we assume Feb is NOT leap, and that
    // the years are the same (for now) ... TODO Fix that!
    public static List<String> findDates() {

        int[] monthDays = new int[] {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        List<String> dates = new ArrayList<String>();
        String currentDate = start;
        dates.add(currentDate);

        // Repeatedly increment the date by one day until we get to the ending date
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
            dates.add(currentDate);
        }
        return dates;
    }

}
