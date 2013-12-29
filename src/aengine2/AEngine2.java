package aengine2;
/**************************************************************************************************
 * Project: AEngine2                                                                              *
 * File:    AEngine2.java                                                                         *
 * Author:  Joseph Marlin (j.marlin@outlook.com)                                                  *
 * Description:                                                                                   *
 *    This class represents the top-level Swing window for the AEngine2 desktop application,      *
 * which creates and appends a Core object, which is the functional rendering loop and canvas     *
 * of the engine, into itself. This allows for the option to add a secondary applet equivalent    *
 * allowing for a JAR which can be launched either in desktop or applet mode in the future.       *
 **************************************************************************************************/

//This is simply a Swing window, so Swing is all we need to reference
import javax.swing.*;

//The main AEngine2 class which encapsulates the window, its properties
//and the application entry point which simply creates a new instance of
//itself and thereby of the engine's Core class.
public class AEngine2 extends JFrame{
    
        //The embedded Core object rendering canvas
        private Core mainPgm;
    
        //Constructor of the application window
	private AEngine2() {
            
                //Construct the Core object, the functional engine itself
                //and insert it into the application window
                mainPgm = new Core();
                add("Center", mainPgm);
                
	}

        //Create the application window which will cause initialization
        //of the engine and begin its execution
	static public void main(String argv[]) {
            
                //Create the main window instance and set it to be unclosable
                //(user will close the application from a menu, though this is not 
                //yet implemented), packed around the Core object, statically sized,
                //centered in the desktop and double-buffered.
		AEngine2 app = new AEngine2();
                app.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		app.pack();
                app.setResizable( false );
                app.setLocationRelativeTo( null );
		app.setVisible(true);
                app.createBufferStrategy(2);
                
	}
        
}
