import java.util.*;

/**
 * Finds referral paths within the Longhorn Network using Dijkstra's algorithm.
 * <p>
 * A referral path is a sequence of students such that the final student in the
 * path has previously interned at a target company. Edges in the underlying
 * {@link StudentGraph} are weighted by connection strength; Dijkstra's algorithm
 * is run on an inverted cost (e.g., 10 - strength) so that stronger connections
 * are treated as shorter paths.
 */
public class ReferralPathFinder {
    private final StudentGraph graph;
    private static final double MAX_STRENGTH = 10;

    /**
     * Private helper class for the Priority Queue in Dijkstra's.
     * Stores a student and the total cost to reach them from the start.
     */
    private static class NodeEntry implements Comparable<NodeEntry>{
        UniversityStudent student;
        double cost;

        NodeEntry(UniversityStudent student, double cost){
            this.student = student;
            this.cost = cost;
        }

        @Override
        public int compareTo(NodeEntry other){
            return Double.compare(this.cost, other.cost);
        }
    }

    /**
     * Constructs a new {@code ReferralPathFinder} backed by the given graph.
     *
     * @param graph the {@link StudentGraph} that encodes connection strengths
     *              between students
     */
    public ReferralPathFinder(StudentGraph graph) {
        // Constructor
        this.graph = graph;
    }



    /**
     * Finds the best referral path from a starting student to any student
     * who has interned at the specified company.
     * <p>
     * This method should:
     * <ul>
     *     <li>Run Dijkstra's algorithm on the {@link StudentGraph}, using
     *         inverted edge weights so that stronger connections appear cheaper.</li>
     *     <li>Stop when a student with the target internship is reached.</li>
     *     <li>Return the list of students along the discovered path
     *         (including the start and target).</li>
     * </ul>
     *
     * @param start         the student from whom the search begins
     * @param targetCompany the name of the company to search internships for
     * @return a list of {@link UniversityStudent} instances representing the
     *         referral path; an empty list or {@code null} may be returned
     *         if no such path exists
     */
    public List<UniversityStudent> findReferralPath(UniversityStudent start, String targetCompany) {
        //*initialization
        PriorityQueue<NodeEntry> pq = new PriorityQueue<>();

        //map of student -> min cost from the start
        Map<UniversityStudent, Double> distance = new HashMap<>();

        //map of student -> student.pi
        Map<UniversityStudent, UniversityStudent> prev = new HashMap<>();

        for(UniversityStudent node: graph.getAllNodes()){
            distance.put(node, Double.POSITIVE_INFINITY);
            prev.put(node, null);
        }

        //start node has cost 0
        distance.put(start, 0.0);
        pq.add(new NodeEntry(start, 0.0));

        //*dijkstras loop
        while (!pq.isEmpty()){
            NodeEntry currentEntry = pq.poll();
            UniversityStudent u = currentEntry.student;
            double uCost = currentEntry.cost;

            //if shorter path already found, skip ts entry
            if (uCost > distance.get(u)){
                continue;
            }

            //goal
            if (u.getPreviousInternships().contains(targetCompany)){
                return path(prev, u);
            }

            //*relax
            for(StudentGraph.Edge edge: graph.getNeighbors(u)){
                UniversityStudent v = edge.getNeighbor();

                //invert weight: cost = 10-strength
                double edgeCost = Math.max(0.1, MAX_STRENGTH - edge.getWeight());   //ensures its always positive

                double newCost = uCost + edgeCost;

                if (newCost < distance.get(v)){
                    distance.put(v, newCost);
                    prev.put(v, u);
                    pq.add(new NodeEntry(v, newCost));
                }
            }
        }
        //if no path was found containing target prev internship
        return new ArrayList<>();
    }

    private List<UniversityStudent> path(Map<UniversityStudent, UniversityStudent> prev, UniversityStudent u){
        LinkedList<UniversityStudent> p = new LinkedList<>();
        UniversityStudent at = u;

        while (at != null){
            p.addFirst(at); //add to front of the list
            at = prev.get(at);
        }
        return p;
    }
}
