/*
 * This code takes in the linear programming problem as input and will output a solution using Balas' Additive Algorithm
 * (c) May 2014 by Daniel Seita
 * NOTE: To make things easy, we really should convert this stuff to canonical form BEFORE it gets inputted...
 * TODO I need to test for flight logic!
 */

import java.util.*;

public class Balas {

    // Input to the problem (from the Master server)
    private static int[][] original_constraints;
    private static int[] original_costs;
    private static ArrayList<Flight> flights = new ArrayList<Flight>();


    /**
     * Constructor for Balas, which gets called from the crawler
     */
    public Balas (int[][] original_constraints, int[] original_costs, ArrayList<Flight> flights) {
        this.original_constraints = original_constraints;
        this.original_costs = original_costs;
        this.flights = flights;
    }


    /** 
     * Called by the Master Server to solve the input. Most notably, it (1) makes constraints canonical if necessary, 
     * (2) finds variable_ordering, a HUGELY important array that maintains ordering of variables according to cost,
     * (3) orders the costs and constraints, (4) calls the DFS which searches for solutions, and (5) prints out results. 
     */
    public List<Flight> solve() {
        int num_variables = original_costs.length;
        assert num_variables == original_constraints[0].length - 2 : "The costs and variables don't match in quantity.";

        make_canonical(original_constraints);

        // TODO Might consider making variable_grouping global...
        // E.g., variable_ordering[0] gives INDEX of smallest cost variable (i.e., flight) in the original 'flights' list
        int[] variable_ordering = order_variables(original_costs);
        int[] ordered_costs = order_costs(variable_ordering, original_costs); // Order the costs
        int[][] ordered_constraints = order_constraints(variable_ordering, original_constraints);

        // Ready to actually start Balas' Additive Algorithm! 
        List<Integer> best_path = dfs(ordered_costs, ordered_constraints, variable_ordering);

        // Convert the best path to the best set of flights, and return that to the master.
        System.out.println("Done with the DFS. Total cost: " + dot_product(best_path, ordered_costs) + "\n");
        List<Flight> best_flights = new ArrayList<Flight>();
        for (int i = 0; i < best_path.size(); i++) {
            if (best_path.get(i) == 1) {
                Flight f = flights.get(variable_ordering[i]);
                best_flights.add(f);
            }
        }
        return best_flights;
    } 


    /**
     * Transforms the problem so that it satisfies the requirements for Balas' Algorithm
     * All objective functions are guaranteed to be positive by construction, so no worries
     */
    public static void make_canonical(int[][] constraints) {
        for (int[] equation : constraints) {
            if (equation[equation.length-2] == 0) {
                for (int i = 0; i < equation.length; i++) {
                    equation[i] = -1 * equation[i];
                }
                equation[equation.length-2] = 1;
            }
        }
    }


    /**
     * Concatenates two arrays together, useful as a helper function for ordering the constraints by cost
     */
    public static int[] concatenate_arrays(int[] array1, int[] array2) {
        int a1len = array1.length;
        int a2len = array2.length;
        int[] combined_array = new int[a1len + a2len];
        System.arraycopy(array1, 0, combined_array, 0, a1len);
        System.arraycopy(array2, 0, combined_array, a1len, a2len);
        return combined_array;
    }


    /**
     * Output: an array of the indices of variables ordered based on cost, which is used in rest of code.
     * Uses the Pair class we made since Java doesn't have tuples. There might be a better way to do this...
     */
    public static int[] order_variables(int[] costs) {

        // Gets everything set up
        int num_variables = costs.length;
        int[] ordered = new int[num_variables];
        Pair[] costs_and_index = new Pair[num_variables];
        for (int i = 0; i < num_variables; i++) {
            costs_and_index[i] = new Pair(costs[i], i);
        }

        // Sorts the array of Pairs
        for (int i = 0; i < num_variables; i++) {
            Pair current_element = costs_and_index[i];
            Pair current_min = current_element;
            int target_index = i;
            for (int j = i+1; j < num_variables; j++) {
                Pair new_element = costs_and_index[j];
                if (new_element.less_than(current_min)) {
                    current_min = new_element;
                    target_index = j;
                }
            }
            Pair original_element = costs_and_index[i];
            costs_and_index[i] = current_min;
            costs_and_index[target_index] = original_element;
        }

        // Now use the additional information to get the actual variable ordering
        for (int i = 0; i < num_variables; i++) {
            ordered[i] = costs_and_index[i].second;
        }
        return ordered;
    }


    /**
     * Takes cost matrix and returns an ordered version of it (also used as a helper for order_constraints)
     */
    public static int[] order_costs(int[] ordered_variables, int[] costs) {
        int[] ordered_costs = new int[costs.length];
        for (int i = 0; i < ordered_variables.length; i++) {
            ordered_costs[i] = costs[ordered_variables[i]];
        }
        return ordered_costs;
    }


    /**
     * Orders the constraints based on the appropriate list of variables
     */
    public static int[][] order_constraints(int[] ordered_variables, int[][] constraints) {
        int[][] ordered_constraints = new int[constraints.length][constraints[0].length];
        for (int i = 0; i < constraints.length; i++) {
            int[] current_equation = new int[ordered_variables.length];

            // Copy current constraint to 'current_equation', except the last two values
            for (int j = 0; j < current_equation.length; j++) {
                current_equation[j] = constraints[i][j];
            }
            int[] single_ordered_constraint = order_costs(ordered_variables, current_equation);

            // Now get the last two in and combine arrays
            int[] last_two = new int[] { constraints[i][ordered_variables.length], constraints[i][ordered_variables.length+1] };
            ordered_constraints[i] = concatenate_arrays(single_ordered_constraint, last_two);
        }
        return ordered_constraints;
    }


    /**
     * Helper method for Balas's alg to check for pruning. Returns FALSE if we CANNOT prune, TRUE if we CAN prune.
     */
    public static boolean check_pruning(List<Integer> path, int[] costs, int[][] constraints) {
        int num_variables = costs.length;

        for (int i = 0; i < constraints.length; i++) {
            int lower_bound = constraints[i][constraints[i].length-1];
            int[] current_equation = new int[num_variables];
            for (int j = 0; j < num_variables; j++) {
                current_equation[j] = constraints[i][j];
            }

            // Assume highest-cost scenario future path for infeasibility pruning
            List<Integer> best_case_path = new ArrayList<Integer>(path);
            while (best_case_path.size() < num_variables) {
                if (current_equation[best_case_path.size()] > 0) {
                    best_case_path.add(1);
                } else {
                    best_case_path.add(0);
                }
            }

            // If highest-cost scenario cannot beat lower bound for this constraint, we're done.
            int best_case_bound = dot_product(best_case_path, current_equation);
            if (best_case_bound < lower_bound) {
                return true;
            }
        }
        return false;
    }


    /**
     * Hepler method for Balas' alg to compute dot product, with path of variables and either costs or weights.
     */
    public static int dot_product(List<Integer> full_path, int[] weights) {
        assert full_path.size() == weights.length : "Error: full path is length " + full_path.size() + " but weight vector is of length " + weights.length;
        int total = 0;
        for (int i = 0; i < weights.length; i++) {
            total += full_path.get(i) * weights[i];
        }
        return total;
    }


    /**
     * Helper method for Balas' alg, used to look ahead for (best-case) solutions by setting "later" variables
     * to be zero. Returns -1 if no solution can be found using this method.
     */
    public static int look_ahead(List<Integer> path, int[] costs, int[][] constraints) {

        // Add zeroes to the path (in one case, add a single 1) to make it 'full' if it's shorter than costs...
        int num_variables = costs.length;
        if (path.size() < num_variables) {
            if (path.get(path.size()-1) == 0) {
                path.add(1);
            }
        }
        for (int i = path.size(); i < num_variables; i++) {
            path.add(0);
        }

        // Now check for feasibility
        for (int i = 0; i < constraints.length; i++) {
            int lower_bound = constraints[i][constraints[i].length-1];
            int[] current_equation = new int[num_variables];
            for (int j = 0; j < num_variables; j++) {
                current_equation[j] = constraints[i][j];
            }

            if (dot_product(path, current_equation) < lower_bound) {
                return -1;
            }
        }

        // If we got to here, it's a valid solution
        return dot_product(path, costs);
    }


    /**
     * Check flight logic. Given a path that we looked-ahead and found a solution, we need to check flight logic as a last step.
     * I don't think this is possible to determine until we have found a complete solution, either in constraints or with other code
     * EX: we don't want flights (in order): DEN to SEA, CHI to DEN, and SEA to CHI, because landing in SEA means leaving from SEA next
     * Returns FALSE if there is a logical discontinuity, and TRUE if it's OK and we can accept it as a solution to the full problem
     * Last note: 'path' may end in a 0, which implies it's case 2 where we need to add another flight at the end
     */
    public static boolean check_flight_logic(List<Integer> path, int[] variable_ordering) {
        // System.out.println("Inside flight logic with path " + path);
        List<Flight> current_flights = new ArrayList<Flight>(); // Current flights listed in order of cost
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i) == 1) {
                int flight_index = variable_ordering[i];
                // current_flights.add(flights.get(i)); // For some reason, I had this here!
                current_flights.add(flights.get(flight_index));
            }
        }
        if (path.get(path.size()-1) == 0 && variable_ordering.length > path.size()) {
            int flight_index = variable_ordering[path.size()];
            current_flights.add(flights.get(flight_index));
        }

        // Next, order the flights by date. Thank goodness Flight class implements comparable!! (Good thinking, huh ;-) ?)
        Collections.sort(current_flights);
        // System.out.println("Current ordered flights: " + current_flights);

        // Now we have a list of complete flights in order. Let's check their logic (don't need to check last)
        for (int i = 0; i < current_flights.size()-1; i++) {
            Flight first_flight = current_flights.get(i);
            Flight second_flight = current_flights.get(i+1);
            String first_destination = first_flight.to;
            String second_arrival = second_flight.from;
            if (!first_destination.equals(second_arrival)) {
                return false;
            }
        }
        return true;
    }


    /**
     * The ultimate method here! This performs Balas' Additive Algorithm by running a smart DFS with look-aheads and pruning
     * It's the iterative version of dfs. With lots of nodes, it might run out of memory so be sure to allocate plenty to java.
     */
    public static List<Integer> dfs(int[] costs, int[][] constraints, int[] variable_ordering) {

        // Getting things set up and assume worst-case scenario cost
        int best_cost = 0;
        List<Integer> best_path = new ArrayList<Integer>();
        for (int i : costs) {
            best_cost += i;
        }
        int num_variables = costs.length;
        int expanded_nodes = 0;

        // Now go through the DFS process
        System.out.println("\nNow doing the DFS...\n");
        Set<Node> discovered = new HashSet<Node>();
        List<Integer> current_path = new ArrayList<Integer>();
        Node root = new Node(null, current_path);
        Stack<Node> st = new Stack<Node>();
        st.push(root);
        int stackElements = 1;
        
        while (!st.empty()) {
            // System.out.println("Stack size before popping this one: " + stackElements);
            Node node = st.pop();
            stackElements--;

            if (!discovered.contains(node)) {
                discovered.add(node);

                // Periodically print out progress to reassure the viewer
                expanded_nodes++; 
                // if (expanded_nodes % 10000 == 0) {
                System.out.println("Node #" + expanded_nodes + "; Path: " + node.path);
                //}

                if (node.path.size() < num_variables) {

                    // First, let's try adding in 1 or a 0-1 sequence to see if we can get a solution already.
                    List<Integer> current_path1 = new ArrayList<Integer>(node.path);
                    current_path1.add(1);
                    List<Integer> path1 = new ArrayList<Integer>(current_path1); // Just a copy
                    int cost_child1 = look_ahead(current_path1, costs, constraints);
                    List<Integer> current_path2 = new ArrayList<Integer>(node.path);
                    current_path2.add(0);
                    List<Integer> path2 = new ArrayList<Integer>(current_path2); // Another copy
                    int cost_child2 = look_ahead(current_path2, costs, constraints);

                    // Check the two cases. In both cases, if we have found a solution, must also CHECK FLIGHT LOGIC 
                    // Note: if it is feasible and logically correct BUT if cost is worse, we don't continue this way in DFS

                    // For each of the two cases, first check if it's feasible. Then check if it's better than best cost.

                    // Case/Path 1
                    if (cost_child1 != -1) {
                        // Only worth checking of cost is better than the best one because we're assuming best-case costs
                        if (cost_child1 < best_cost) {
                            // If this is a candidate, then check to make sure that flight is logically consistent.
                            // If not, we MAY add in a future flight later in between to make things work, so that's why we have a special case
                            System.out.println("Now in case 1, checking flight logic...");
                            if (check_flight_logic(path1, variable_ordering)) {
                                System.out.println("Case 1 update with node #" + expanded_nodes + "; path: " + node.path);
                                System.out.println("new cost: " + cost_child1 + "; new path: " + current_path1);
                                best_cost = cost_child1;
                                best_path = current_path1;
                            } else {
                                Node child = new Node(node, path1); 
                                st.push(child);
                                stackElements++;
                            }
                        }
                    } else {
                        // Can't find feasible solution (or perhaps got suboptimal solution), so see if we can prune. If not, add to DFS stack. 
                        if (!check_pruning(path1, costs, constraints)) {
                            Node child = new Node(node, path1); 
                            st.push(child);
                            stackElements++;
                        }
                    }

                    // Case/Path 2
                    if (cost_child2 != -1) {
                        if (cost_child2 < best_cost) {
                            // Now check if this candidate works
                            System.out.println("Now in case 2, checking flight logic...");
                            if (check_flight_logic(path2, variable_ordering)) {
                                System.out.println("Case 2 update with node #" + expanded_nodes + "; path: " + node.path);
                                System.out.println("new cost: " + cost_child2 + "; new path: " + current_path2);
                                best_cost = cost_child2;
                                best_path = current_path2;
                            } else {
                                Node child = new Node(node, path2); 
                                st.push(child);
                                stackElements++;
                            }
                        }
                    } else {
                        if (!check_pruning(path2, costs, constraints)) {
                            Node child = new Node(node, path2);
                            st.push(child);
                            stackElements++;
                        }
                    }
                }
            }
        }
        System.out.println("Total nodes analyzed: " + expanded_nodes);
        System.out.println("Best solution: " + best_path);
        return best_path;
    }


    /**
     * Class for nodes that we use for DFS to implement Balas
     * Fairly straightforward; keeps the path which is the assignment of 0s and 1s to flights
     */
    private static class Node {
        public final Node parent;
        public final List<Integer> path;

        // For the non-root nodes
        public Node(Node parent, List<Integer> path) {
            this.parent = parent;
            this.path = path;
        }
    }


    /**
     * Helps us to organize things by making use of (2-element) tuples, since Java doesn't have a built-in tuple class
     * I made this to help me create 'variable_grouping', a HUGELY important array throughout the algorithm
     */
    private static class Pair {
        public final int first;
        public final int second;

        public Pair(int first, int second) {
            this.first = first;
            this.second = second;
        }

        public String toString() {
            return "[" + this.first + ", " + this.second + "]";
        }

        public boolean less_than(Pair pair) {
            return this.first < pair.first;
        }
    }

}
