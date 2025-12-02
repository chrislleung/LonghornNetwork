import java.io.*;
import java.sql.Array;
import java.util.*;

/**
 * Utility class responsible for parsing student data from an input file.
 * <p>
 * The parser reads each student's attributes (name, age, gender, major, GPA,
 * roommate preferences, and previous internships) and constructs corresponding
 * {@link UniversityStudent} objects. These objects are then used to build the
 * {@code StudentGraph} and to run the matching and referral algorithms.
 */
public class DataParser {

    /**
     * Parses a text file describing a set of students and returns a list of
     * {@link UniversityStudent} instances.
     * <p>
     * The file format is defined in {@code input_sample.txt}. Each student block
     * should contain all required attributes, and optional attributes such as
     * roommate preferences or internships may be missing or marked as "None".
     * The method should validate input and handle malformed or missing fields
     * gracefully.
     *
     * @param filename the path to the input file containing student data
     * @return a list of {@link UniversityStudent} objects created from the file
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static List<UniversityStudent> parseStudents(String filename) throws IOException {
        List<UniversityStudent> students = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))){
            String line;
            Map<String, String> studentData = new HashMap<>();


            while ((line = reader.readLine()) != null){
                //blank line -> end of a student's data block
                //process data
                if (line.trim().isEmpty()){
                    UniversityStudent student = createStudentFromMap(studentData);
                    if (student != null){
                        students.add(student);
                    }
                    studentData.clear();
                } else {
                    String[] parts = line.split(":", 2);

                    if (parts.length == 2){
                        studentData.put(parts[0].trim(), parts[1].trim());
                    } else {
                        System.err.println("Skipping malformed line (missing ':'): " +line);
                    }
                }
            }

            if (!studentData.isEmpty()){
                UniversityStudent student = createStudentFromMap(studentData);
                if (student != null){
                    students.add(student);
                }
            }
        } catch (FileNotFoundException e){
            System.err.println("Error: Input file not found: " +filename);
            throw e;
        } catch (IOException e){
            System.err.println("Error reading file: " +filename);
            throw e;
        }

        return students;
    }

    /**
     * Private helper method to convert a map of raw string data into a
     * UniversityStudent object. This is where validation and type conversion happen.
     *
     * @param data A map containing the key-value pairs for a single student.
     * @return A {@link UniversityStudent} object, or {@code null} if the
     * required data is missing or malformed.
     */
    private static UniversityStudent createStudentFromMap(Map<String, String> data) {
        try {
            //get and validate required fields
            String name = data.get("Name");
            String gender = data.get("Gender");
            String major = data.get("Major");

            //check if they exist
            if (name == null || name.isEmpty() || gender == null || gender.isEmpty() || major == null || major.isEmpty() || data.get("Age") == null || data.get("GPA") == null) {
                System.err.println("Skipping students with missing required fields: " + data.get("Name"));
                return null;
            }

            //parse fields that require type conversion
            int age = Integer.parseInt(data.get("Age"));
            double gpa = Double.parseDouble(data.get("GPA"));
            int year = Integer.parseInt(data.get("Year"));

            //parse optional list fields
            List<String> prefs = parseList(data.get("Roommate Preferences"));
            List<String> internships = parseList(data.get("Previous Internships"));

            return new UniversityStudent(name, age, gender, year, major, gpa, prefs, internships);
        } catch (NumberFormatException e){
            System.err.println("Skipping student with invalid number format (Age/Year/GPA): " +data.get("Name"));
            return null;
        } catch (Exception e){
            System.err.println("Skipping student due to an unexpected error: " + data.get("Name") + " | Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Private helper method to parse a comma-separated string into a List<String>.
     * Handles the edge cases of "None", null, or empty strings.
     *
     * @param roommatePreferences The raw string value from the map (e.g., "Alice, Bob" or "None")
     * @return A list of strings. An empty list is returned if the input is
     * null, empty, or "None".
     */
    private static List<String> parseList(String roommatePreferences) {
        List<String> list = new ArrayList<>();

        //handle missing or empty field
        if (roommatePreferences == null || roommatePreferences.trim().isEmpty() || roommatePreferences.trim().equalsIgnoreCase("None")){
            return list;    //return empty list
        }

        //split the string by commas
        String[] items = roommatePreferences.split(",");
        for (String item: items){
            String trimmedItem = item.trim();
            if (!trimmedItem.isEmpty()) {
                list.add(trimmedItem);
            }
        }

        return list;
    }
}
