import java.util.concurrent.Semaphore;

/**
 * Runnable task that simulates sending a friend request between two
 * {@link UniversityStudent} instances in the Longhorn Network.
 * <p>
 * This class is intended to be executed in a separate thread so that
 * multiple friend requests can happen concurrently.
 */
public class FriendRequestThread implements Runnable {
    private final UniversityStudent sender;
    private final UniversityStudent receiver;
    private static final Semaphore semaphore = new Semaphore(1);

    /**
     * Creates a new {@code FriendRequestThread} for a friend request
     * from the given sender to the given receiver.
     *
     * @param sender   the student sending the friend request
     * @param receiver the student receiving the friend request
     */
    public FriendRequestThread(UniversityStudent sender, UniversityStudent receiver) {
        // Constructor
        this.sender = sender;
        this.receiver = receiver;
    }

    /**
     * Executes the friend request logic when the thread runs.
     * <p>
     * The implementation should be thread-safe and update any shared data
     * structures (e.g., friend lists) in a synchronized manner.
     */
    @Override
    public void run() {
        try{
            semaphore.acquire();
            System.out.println("FriendRequest (Thread-safe): " +sender.name + " sent a friend request to " +receiver.name);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            System.err.println("FriendRquest interrupted: " + e.getMessage());
        } finally{
            semaphore.release();
        }
    }
}
