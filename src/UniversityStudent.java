import java.util.*;

/**
 * Concrete implementation of a {@link Student} in the Longhorn Network.
 * <p>
 * A {@code UniversityStudent} stores all information parsed from the input file,
 * including demographic details, academic profile, roommate preferences,
 * and previous internships. This class also implements the connection-strength
 * calculation used to build the {@code StudentGraph}.
 */
public class UniversityStudent extends Student {
    // TODO: Constructor and additional methods to be implemented
    private UniversityStudent roommate;
    private final Set<UniversityStudent> friends;
    private final Map<UniversityStudent, List<String>> chatHistory;
    /**
     * Creates a new {@code UniversityStudent} with the given attributes.
     * <p>
     * You should implement a constructor that initializes all inherited fields
     * (name, age, gender, year, major, gpa, roommate preferences, and internships)
     * according to the parsed input.
     *
     * @param name                unique name of the student
     * @param age                 age in years
     * @param gender              gender string as provided in the input
     * @param year                academic year (e.g., 1 = freshman)
     * @param major               academic major (e.g., "ECE", "CS")
     * @param gpa                 grade point average
     * @param roommatePreferences ordered list of roommate preferences by name
     * @param previousInternships list of company names where the student interned
     */
    public UniversityStudent(String name, int age, String gender, int year, String major, double gpa, List roommatePreferences, List previousInternships) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.year = year;
        this.major = major;
        this.gpa = gpa;
        this.roommatePreferences = roommatePreferences;
        this.previousInternships = previousInternships;

        this.roommate = null;
        this.friends = Collections.synchronizedSet(new HashSet<>());
        this.chatHistory = Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * Computes the connection strength between this student and another student.
     * The calculation should follow the rules in the lab specification:
     * <ul>
     *     <li>Add 4 if the two students are roommates.</li>
     *     <li>Add 3 for each shared internship.</li>
     *     <li>Add 2 if they share the same major.</li>
     *     <li>Add 1 if they are the same age.</li>
     * </ul>
     *
     * @param other another student in the Longhorn Network
     * @return integer connection strength based on shared attributes
     */
    @Override
    public int calculateConnectionStrength(Student other) {
        // TODO: Implement according to the specification.
        if (!(other instanceof UniversityStudent)){
            return 0;
        }

        UniversityStudent otherStudent = (UniversityStudent) other;

        int strength = 0;

        //+4 if roommates
        if (roommate != null && roommate.equals(otherStudent)){
            strength += 4;
        }

        //+3 for each shared internship
        Set<String> myInternships = new HashSet<>(previousInternships);
        myInternships.remove("None");
        myInternships.remove("");

        for(String internship: otherStudent.getPreviousInternships()){
            if (myInternships.contains(internship)){
                strength += 3;
            }
        }

        //+2 if same major
        if (major.equals(otherStudent.getMajor())){
            strength += 2;
        }

        //+1 if same age
        if (age == otherStudent.age){
            strength ++;
        }

        return strength;
    }

    // Additional helper methods (getters, setters, roommate/friend/chat helpers)
    // should be documented here once implemented.
    public String getName(){ return name; }
    public int getAge(){ return age;}
    public int getYear(){ return year;}

    public String getMajor(){ return major; }

    public List<String> getRoommatePreferences(){
        return roommatePreferences;
    }

    public List<String> getPreviousInternships(){
        return previousInternships;
    }

    public UniversityStudent getRoommate(){
        return roommate;
    }

    public void setRoommate(UniversityStudent s){
        roommate = s;
    }

    /**
     * Adds a friend to this student's friend list.
     * Called by {@link FriendRequestThread}. This is thread-safe.
     * @param friend The student to add as a friend.
     */
    public void addFriend(UniversityStudent friend) {
        this.friends.add(friend);
    }

    /**
     * Adds a chat message to the history between this student and another.
     * Called by {@link ChatThread}. This method is thread-safe.
     * @param other   The student this message is being exchanged with.
     * @param message The chat message to add.
     */
    public synchronized void addMessageToHistory(UniversityStudent other, String message) {
        // computeIfAbsent ensures the list is created atomically and safely.
        this.chatHistory.computeIfAbsent(other, k ->
                Collections.synchronizedList(new ArrayList<>())
        ).add(message);
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.name);
    }

    @Override
    public String toString(){
        return this.name;
    }
}
