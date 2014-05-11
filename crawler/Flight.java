public class Flight {
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
 		return from + "->" + to + ", " +
 		    (price != -1 ? "$" + price : "price not known");
 	}
}