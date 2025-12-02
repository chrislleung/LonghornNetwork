/**
 * Runnable task that simulates a chat message being sent between two
 * {@link UniversityStudent} instances in the Longhorn Network.
 * <p>
 * This class is executed in a separate thread to model concurrent chat
 * activity between multiple pairs of students.
 */
public class ChatThread implements Runnable {
    private final UniversityStudent sender;
    private final UniversityStudent receiver;
    private final String message;

    /**
     * Creates a new {@code ChatThread} representing a single chat message
     * from the given sender to the given receiver.
     *
     * @param sender   the student sending the message
     * @param receiver the student receiving the message
     * @param message  the text of the chat message
     */
    public ChatThread(UniversityStudent sender, UniversityStudent receiver, String message) {
        // Constructor
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
    }

    /**
     * Executes the chat logic when the thread runs.
     * <p>
     * The implementation should safely append the message to both students'
     * chat history or to any shared logging structure used by the application.
     */
    @Override
    public void run() {
        // Method signature only

        String formatted = sender.getName() + ": " +message;

        //addMessageToHistory is synchrnized
        sender.addMessageToHistory(receiver, formatted);
        receiver.addMessageToHistory(sender, formatted);

        System.out.println("Chat [" +sender.getName() +" -> " + receiver.getName() + "]" +message);
    }
}
