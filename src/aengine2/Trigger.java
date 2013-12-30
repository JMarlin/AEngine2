package aengine2;
/**************************************************************************************************
 * Project: AEngine2                                                                              *
 * File:    Trigger.java                                                                          *
 * Author:  Joseph Marlin (j.marlin@outlook.com)                                                  *
 * Description:                                                                                   *
 *    This is an incredibly simple class which simply encapsulates a javascript tag and a system  *  
 * time at which to evaluate it.                                                                  *
 **************************************************************************************************/

public class Trigger {
    public long time;
    public String callback;
    
    public Trigger(long newTime, String newCallback) {
        time = newTime;
        callback = newCallback;
    }
    
}
