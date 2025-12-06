import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class LabServer {
    private static List<UniversityStudent> currentStudents = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/load", new LoadHandler());
        server.createContext("/api/match", new MatchHandler());
        server.createContext("/api/referral", new ReferralHandler());
        server.createContext("/api/chat", new ChatHandler());
        server.createContext("/api/friends", new FriendRequestHandler()); // New Endpoint

        server.setExecutor(null);
        System.out.println("Longhorn Network Server is running on port " + port + "...");
        server.start();
    }

    static class LoadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            int caseNum = 1;
            if (query != null && query.contains("case=2")) caseNum = 2;
            if (query != null && query.contains("case=3")) caseNum = 3;

            System.out.println("Loading Test Case " + caseNum);
            switch (caseNum) {
                case 1: currentStudents = Main.generateTestCase1(); break;
                case 2: currentStudents = Main.generateTestCase2(); break;
                case 3: currentStudents = Main.generateTestCase3(); break;
                default: currentStudents = Main.generateTestCase1();
            }
            // Reset state
            for(UniversityStudent s : currentStudents) {
                s.setRoommate(null);
                try {
                    // Clear chat history via reflection
                    Field chatField = UniversityStudent.class.getDeclaredField("chatHistory");
                    chatField.setAccessible(true);
                    ((Map) chatField.get(s)).clear();

                    // Clear friend list via reflection (since friends is final, we clear the set)
                    Field friendField = UniversityStudent.class.getDeclaredField("friends");
                    friendField.setAccessible(true);
                    ((Set) friendField.get(s)).clear();
                } catch (Exception e) { /* Ignore */ }
            }
            sendResponse(t, serializeStudents(currentStudents));
        }
    }

    static class MatchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            GaleShapley.assignRoommates(currentStudents);
            sendResponse(t, serializeStudents(currentStudents));
        }
    }

    static class ReferralHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            String startName = getQueryParam(query, "start");
            String targetCompany = getQueryParam(query, "company");

            UniversityStudent startNode = currentStudents.stream()
                    .filter(s -> s.getName().equals(startName))
                    .findFirst().orElse(null);

            List<String> pathNames = new ArrayList<>();
            if (startNode != null) {
                StudentGraph graph = new StudentGraph(currentStudents);
                ReferralPathFinder finder = new ReferralPathFinder(graph);
                List<UniversityStudent> path = finder.findReferralPath(startNode, targetCompany);
                for (UniversityStudent s : path) pathNames.add(s.getName());
            }

            String jsonResponse = "[" + pathNames.stream()
                    .map(n -> "\"" + n + "\"")
                    .collect(Collectors.joining(",")) + "]";
            sendResponse(t, jsonResponse);
        }
    }

    // New Handler specifically for Friend Requests
    static class FriendRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            System.out.println("Running Friend Request Threads...");
            if (currentStudents.size() >= 2) {
                ExecutorService executor = Executors.newFixedThreadPool(4);
                UniversityStudent s1 = currentStudents.get(0);
                UniversityStudent s2 = currentStudents.get(1);

                // Run Friend Requests
                executor.submit(new FriendRequestThread(s1, s2));
                executor.submit(new FriendRequestThread(s2, s1));

                executor.shutdown();
                try {
                    executor.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            sendResponse(t, serializeStudents(currentStudents));
        }
    }

    static class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            System.out.println("Running Chat Threads...");
            if (currentStudents.size() >= 2) {
                ExecutorService executor = Executors.newFixedThreadPool(4);
                UniversityStudent s1 = currentStudents.get(0);
                UniversityStudent s2 = currentStudents.get(1);

                // Only run chats here (removed friend requests so buttons are distinct)
                executor.submit(new ChatThread(s1, s2, "Hey, are you looking for a roommate?"));
                executor.submit(new ChatThread(s2, s1, "Yes I am! What is your major?"));
                executor.submit(new ChatThread(s1, s2, "I am in " + s1.getMajor() + ". You?"));
                executor.submit(new ChatThread(s2, s1, "I'm in " + s2.getMajor() + ". Let's connect!"));

                executor.shutdown();
                try {
                    executor.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            sendResponse(t, serializeStudents(currentStudents));
        }
    }

    private static void sendResponse(HttpExchange t, String response) throws IOException {
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Content-Type", "application/json");
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static String getQueryParam(String query, String key) {
        if (query == null) return "";
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1 && pair[0].equals(key)) return pair[1];
        }
        return "";
    }

    // --- JSON SERIALIZATION ---

    private static String serializeStudents(List<UniversityStudent> students) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < students.size(); i++) {
            UniversityStudent s = students.get(i);
            json.append("{");
            json.append("\"name\":\"").append(s.getName()).append("\",");
            json.append("\"age\":").append(s.getAge()).append(",");
            json.append("\"major\":\"").append(s.getMajor()).append("\",");
            json.append("\"gpa\":").append(s.gpa).append(",");
            json.append("\"prefs\":").append(listToJson(s.getRoommatePreferences())).append(",");
            json.append("\"internships\":").append(listToJson(s.getPreviousInternships())).append(",");

            String roommateName = (s.getRoommate() != null) ? s.getRoommate().getName() : "null";
            json.append("\"roommateName\":\"").append(roommateName).append("\",");

            // Serialize Friends via reflection
            json.append("\"friends\":").append(serializeFriends(s)).append(",");

            // Serialize Chat History
            json.append("\"chatHistory\":").append(serializeChatHistory(s));

            json.append("}");
            if (i < students.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    private static String serializeFriends(UniversityStudent s) {
        try {
            Field friendField = UniversityStudent.class.getDeclaredField("friends");
            friendField.setAccessible(true);
            Set<UniversityStudent> friends = (Set<UniversityStudent>) friendField.get(s);
            if (friends == null) return "[]";

            List<String> names = friends.stream().map(UniversityStudent::getName).collect(Collectors.toList());
            return listToJson(names);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String serializeChatHistory(UniversityStudent s) {
        try {
            Field chatField = UniversityStudent.class.getDeclaredField("chatHistory");
            chatField.setAccessible(true);
            Map<UniversityStudent, List<String>> history = (Map<UniversityStudent, List<String>>) chatField.get(s);

            if (history == null || history.isEmpty()) return "{}";

            StringBuilder sb = new StringBuilder("{");
            int count = 0;
            for (Map.Entry<UniversityStudent, List<String>> entry : history.entrySet()) {
                if (count > 0) sb.append(",");
                sb.append("\"").append(entry.getKey().getName()).append("\":");
                sb.append(listToJson(entry.getValue()));
                count++;
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String listToJson(List<String> list) {
        if (list == null) return "[]";
        return "[" + list.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + "]";
    }
}