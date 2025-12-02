import java.util.*;

/**
 * Implements the Gale–Shapley stable matching algorithm for assigning
 * roommates within the Longhorn Network.
 * <p>
 * Each {@link UniversityStudent} provides an ordered list of roommate
 * preferences. This class uses those preference lists to compute a stable
 * set of roommate pairings whenever possible.
 */
public class GaleShapley {

    /**
     * Assigns roommates to the given list of students using the
     * Gale–Shapley stable matching algorithm.
     * <p>
     * The method should:
     * <ul>
     *     <li>Treat each student as both a proposer and a receiver.</li>
     *     <li>Use each student's roommate preference list to drive proposals.</li>
     *     <li>Ensure that a proposal is accepted if the receiver is currently
     *         unmatched, or prefers the new proposer to their existing roommate.</li>
     *     <li>Leave students with empty or invalid preference lists unmatched.</li>
     * </ul>
     *
     * @param students list of {@link UniversityStudent} objects to match
     */
    public static void assignRoommates(List<UniversityStudent> students) {
        //*initialization
        //queue of proposers
        Queue<UniversityStudent> freeProposers = new LinkedList<>(students);
        //map of engagements (receiver -> proposer)
        Map<UniversityStudent, UniversityStudent> engagedTo = new HashMap<>();
        //map to track the next person a proposer will propose to (by index)
        Map<UniversityStudent, Integer> nextProposalIndex = new HashMap<>();

        //*preprocessing
        //map for quick lookup
        Map<String, UniversityStudent> nameToStudent = new HashMap<>();
        for(UniversityStudent s: students){
            nameToStudent.put(s.getName(), s);
        }

        //map for quick lookup of preference ranks (receiver -> (proposer -> rank))
        Map<UniversityStudent, Map<UniversityStudent, Integer>> preferenceRanks = new HashMap<>();
        for(UniversityStudent student: students){
            nextProposalIndex.put(student, 0); //each proposal starts at their 0th pref
            Map<UniversityStudent, Integer> ranks = new HashMap<>();

            List<String> prefNames = student.getRoommatePreferences();
            int rank = 0;
            for(String name: prefNames){
                UniversityStudent prefStudent = nameToStudent.get(name);
                if (prefStudent != null){
                    ranks.put(prefStudent, rank++);
                }
            }
            preferenceRanks.put(student, ranks);
        }

        //*main algo loop
        while (!freeProposers.isEmpty()){
            UniversityStudent proposer = freeProposers.poll();

            //get proposer's preference list
            List<String> proposerPrefs = proposer.getRoommatePreferences();
            int proposalIdx = nextProposalIndex.get(proposer);

            //if proposer is out of ppl to propose to, they remain unmatched
            if (proposalIdx >= proposerPrefs.size()){
                continue;
            }

            //get next receiver from pref list
            String receiverName = proposerPrefs.get(proposalIdx);
            UniversityStudent receiver = nameToStudent.get(receiverName);

            //increment index for the next loop
            nextProposalIndex.put(proposer, proposalIdx +1);

            //if name is invalid or not in student list
            if (receiver == null){
                freeProposers.add(proposer);    //add back to queue and try again later
                continue;
            }

            if (!engagedTo.containsKey(receiver)){      //receiver is free
                engagedTo.put(receiver, proposer);
            } else {            //receiver is currently engaged
                UniversityStudent currentPartner = engagedTo.get(receiver);

                //get receiver's pref ranks
                Map<UniversityStudent, Integer> receiverRanks = preferenceRanks.get(receiver);

                //get rank of proposer and current engagement
                int newProposerRank = receiverRanks.getOrDefault(proposer, Integer.MAX_VALUE);
                int currentPartnerRank = receiverRanks.getOrDefault(currentPartner, Integer.MAX_VALUE);

                if (newProposerRank < currentPartnerRank){  //cockblock
                    engagedTo.put(receiver, proposer);
                    freeProposers.add(currentPartner);
                } else {
                    freeProposers.add(proposer);            //new proposer remains free
                }
            }
        }

        //*finalize matches
        Set<UniversityStudent> matchedStudents = new HashSet<>();

        for(Map.Entry<UniversityStudent, UniversityStudent> entry: engagedTo.entrySet()){
            UniversityStudent receiver = entry.getKey();
            UniversityStudent proposer = entry.getValue();

            //make sure each set pairs once using the matchedStudents set
            if (!matchedStudents.contains(receiver) && !matchedStudents.contains(proposer)){
                receiver.setRoommate(proposer);
                proposer.setRoommate(receiver);

                matchedStudents.add(receiver);
                matchedStudents.add(proposer);
            }
        }
    }
}
