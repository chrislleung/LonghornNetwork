import java.sql.SQLOutput;
import java.util.*;

/**
 * Represents a weighted undirected graph of UniversityStudent nodes.
 */
public class StudentGraph {
    // You will implement this later.
    public static class Edge {
        public final UniversityStudent neighbor;
        public final int weight;

        public Edge(UniversityStudent neighbor, int weight){
            this.neighbor = neighbor;
            this.weight = weight;
        }

        public UniversityStudent getNeighbor() {
            return neighbor;
        }

        public int getWeight() {
            return weight;
        }

        public String toString(){
            return "(" + neighbor.name + ", " +weight + ")";
        }
    }

    //adjacency list
    private final Map<UniversityStudent, List<Edge>> adjList;

    /**
     * Constructs the StudentGraph.
     * <p>
     * Initializes the graph structure and adds all students as nodes.
     * It then iterates through all unique pairs of students, calculates their
     * connection strength, and adds a weighted, undirected edge between them.
     *
     * @param students The list of all students to add to the graph.
     */
    public StudentGraph(List<UniversityStudent> students){
        adjList = new HashMap<>();
        
        //add all nodes
        for(UniversityStudent student: students){
            adjList.put(student, new ArrayList<>());
        }

        //2 build the graph by adding edges for all unique pairs
        for (int i = 0; i < students.size(); i++) {
            for (int j = i + 1; j < students.size(); j++) {
                UniversityStudent s1 = students.get(i);
                UniversityStudent s2 = students.get(j);

                int strength = s1.calculateConnectionStrength(s2);

                if (strength > 0) {
                    addEdge(s1, s2, strength);
                }
            }
        }
    }

    /**
     * Adds an edge between two students with the given weight.
     * Since the graph is undirected, the edge is added in both directions.
     *
     * @param s1     The first student (node).
     * @param s2     The second student (node).
     * @param weight The connection strength (weight) of the edge.
     */
    public void addEdge(UniversityStudent s1, UniversityStudent s2, int weight){
        if(adjList.containsKey(s1) && adjList.containsKey(s2)){
            adjList.get(s1).add(new Edge(s2, weight));
            adjList.get(s2).add(new Edge(s1, weight));
        }
    }

    /**
     * Returns a list of all edges connected to the specified student.
     *
     * @param student The student (node) to get neighbors for.
     * @return A list of {@link Edge} objects; returns an empty list
     * if the student has no neighbors or is not in the graph.
     */
    public List<Edge> getNeighbors(UniversityStudent student){
        return adjList.getOrDefault(student, new ArrayList<>());
    }

    /**
     * Returns a set of all nodes (students) in the graph.
     *
     * @return A {@link Set} containing all {@link UniversityStudent} nodes.
     */
    public Set<UniversityStudent> getAllNodes(){
        return adjList.keySet();
    }

    public void displayGraph(){
        System.out.println("\nStudent Graph:");
        for (UniversityStudent s: adjList.keySet()){
            System.out.println(s.name +" -> " +adjList.get(s));
        }
        System.out.println();
    }
}
