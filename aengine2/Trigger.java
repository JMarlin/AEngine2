package aengine2;

/**
 *
 * @author Joe
 */
public class Trigger {
    public long time;
    public String callback;
    
    public Trigger(long newTime, String newCallback) {
        time = newTime;
        callback = newCallback;
    }
    
}
