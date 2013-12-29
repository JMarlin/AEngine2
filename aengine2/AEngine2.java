package aengine2;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Joe
 */
public class AEngine2 extends JFrame{
    private Core mainPgm;

        private static String OS = System.getProperty("os.name").toLowerCase();
    
	private AEngine2() {
                mainPgm = new Core();
                add("Center", mainPgm);
	}

	static public void main(String argv[]) {
		AEngine2 app = new AEngine2();
                app.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		app.pack();
                app.setResizable( false );
                app.setLocationRelativeTo( null );
		app.setVisible(true);
                app.createBufferStrategy(2);
	}
        
}
