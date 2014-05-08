/*
 * This code takes in the linear programming problem as input and will output a solution ue to Balas' Algorithm
 * (c) May 2014 by Daniel Seita
 * NOTE: To make things easy, we really should convert this stuff to canonical form BEFORE it gets inputted...
 */

import java.util.*;

public class balas {

    // Transforms the problem so that it satisfies the requirements for Balas' Algorithm
    // All objective functions are guaranteed to be positive by construction, so no worries
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


    // Concatenates two arrays together (useful as a helper function)
    public static int[] concatenate_arrays(int[] array1, int[] array2) {
        int a1len = array1.length;
        int a2len = array2.length;
        int[] combined_array = new int[a1len + a2len];
        System.arraycopy(array1, 0, combined_array, 0, a1len);
        System.arraycopy(array2, 0, combined_array, a1len, a2len);
        return combined_array;
    }


    // Outputs an array of the indices of variables ordered based on cost.
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


    // Takes in the cost matrix and returns an ordered version of it (also used as a helper for order_constraints)
    public static int[] order_costs(int[] ordered_variables, int[] costs) {
        int[] ordered_costs = new int[costs.length];
        for (int i = 0; i < ordered_variables.length; i++) {
            ordered_costs[i] = costs[ordered_variables[i]];
        }
        return ordered_costs;
    }


    // Orders the constraints based on the appropriate list of variables
    public static int[][] order_constraints(int[] ordered_variables, int[][] constraints) {
        int[][] ordered_constraints = new int[constraints.length][constraints[0].length];
        assert ordered_constraints.length == ordered_variables.length + 2 : "Something is off with the number of elements here.";
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


    // Helper method for Balas's alg to check for pruning. Returns FALSE if we CANNOT prune, TRUE if we CAN prune.
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


    // Hepler method for Balas' alg to compute dot product, with path of variables and either costs or weights.
    public static int dot_product(List<Integer> full_path, int[] weights) {
        assert full_path.size() == weights.length : "Need equal number of variables in path list and cost vector.";
        int total = 0;
        for (int i = 0; i < weights.length; i++) {
            total += full_path.get(i) * weights[i];
        }
        return total;
    }


    // Helper method for Balas' alg, used to look ahead for solutions by setting "later" variables
    // to be zero. Returns -1 if no solution can be found using this method.
    public static int look_ahead(List<Integer> path, int[] costs, int[][] constraints) {

        // Add zeroes to the path (sometimes we add a single 1) to make it 'full'
        int num_variables = costs.length;
        if (path.get(path.size()-1) == 0) {
            path.add(1);
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


    // This performs Balas' Additive Algorithm.
    public static List<Integer> dfs(int[] costs, int[][] constraints) {

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
        
        while (!st.empty()) {
            Node node = st.pop();
            if (!discovered.contains(node)) {
                discovered.add(node);
                
                System.out.println("Now expanding node number " + expanded_nodes++ + " with path: " + node.path);

                if (node.path.size() < num_variables) {

                    // First, let's try adding in 1 or a 0-1 sequence to see if we can get a solution already.
                    List<Integer> current_path1 = new ArrayList<Integer>(node.path);
                    current_path1.add(1);
                    List<Integer> path1 = new ArrayList<Integer>(current_path1);
                    int cost_child1 = look_ahead(current_path1, costs, constraints);
                    List<Integer> current_path2 = new ArrayList<Integer>(node.path);
                    current_path2.add(0);
                    List<Integer> path2 = new ArrayList<Integer>(current_path2);
                    int cost_child2 = look_ahead(current_path2, costs, constraints);

                    // Check the two cases
                    if (cost_child1 != -1) {
                        if (cost_child1 < best_cost) {
                            System.out.println("(Case 1 of 2). Old best cost: " + best_cost + ", new best cost: " + cost_child1);
                            best_cost = cost_child1;
                            best_path = current_path1;
                        }
                    } else if (cost_child2 != -1) {
                        if (cost_child2 < best_cost) {
                            System.out.println("(Case 2 of 2). Old best cost: " + best_cost + ", new best cost: " + cost_child2);
                            best_cost = cost_child2;
                            best_path = current_path2;
                        } 
                    } else {

                        // Can't find a feasible solution, so see if we can prune. If not, add to DFS stack.
                        System.out.println("Cannot find a feasible solution by looking ahead, so let's check for pruning.");
                        if (!check_pruning(path1, costs, constraints)) {
                            System.out.println("Case 1: cannot prune.");
                            Node child = new Node(node, path1); 
                            st.push(child);
                        }
                        if (!check_pruning(path2, costs, constraints)) {
                            System.out.println("Case 2: cannot prune.");
                            Node child = new Node(node, path2);
                            st.push(child);
                        }
                    }
                }
            }
        }
        return best_path;
    }


    public static void main(String[] args) {
        
        // Constraints matrix. The numbers in the last COLUMN (not row) corresponds to the 'b' vector.
        // Second to last column is the type of equality: 0 means <=, 1 means >=. The algorithm requires >=.
        // Each row here (i.e., sub-array) represents one equality.
        // int[][] constraints = new int[][] { {-2, 6, -3, 4, 1, -2, 1, 2},
                                            // {-5, -3, 1, 3, -2, 1, 1, -2},
                                            // {5, -1, 4, -2, 2, -1, 1, 3} };

        int[][] constraints = new int[][] { { 1, -3,  6, -2, -2,  4, 1,  2},
                                            {-2,  1, -3,  1, -5,  3, 1, -2},
                                            { 2,  4, -1, -1,  5, -2, 1,  3} };
        // int[] costs = new int[] {3,5,6,9,10,10};
        int[] costs = new int[] {10,6,5,10,3,9};
        int num_variables = costs.length;
        assert num_variables == constraints[0].length : "The costs and varaibles don't match in quantity.";

        make_canonical(constraints); // Get constraints in canonical form
        int[] variable_ordering = order_variables(costs); // Find the ordering of variables according to cost
        int[] ordered_costs = order_costs(variable_ordering, costs); // Order the costs
        int[][] ordered_constraints = order_constraints(variable_ordering, constraints);

        // Ready to actually start Balas' Additive Algorithm!
        List<Integer> best_path = dfs(ordered_costs, ordered_constraints);
        System.out.println("\nDone with the DFS. Best path: " + best_path + ", with total cost: " + dot_product(best_path, ordered_costs));
    }


    // Class for nodes that we use for DFS to implement Balas
    private static class Node {
        public final Node parent;
        public final List<Integer> path;

        // For the non-root nodes
        public Node(Node parent, List<Integer> path) {
            this.parent = parent;
            this.path = path;
        }
    }


    // Helps us to organize things by making use of tuples
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
