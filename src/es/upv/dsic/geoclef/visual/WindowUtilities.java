package es.upv.dsic.geoclef.visual;
import javax.swing.*;

import es.upv.dsic.geoclef.geography.WorldPoint;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;

/** A few utilities that simplify testing of windows in Swing.
 *  1998 Marty Hall, http://www.apl.jhu.edu/~hall/java/
 */

class ExitListener extends WindowAdapter {
	public void windowClosing(WindowEvent event) {
	    //System.exit(0);
	  }
}
	  
public class WindowUtilities {
	static Vector<JFrame> openFrames = new Vector();
  /** Tell system to use native look and feel, as in previous
   *  releases. Metal (Java) LAF is the default otherwise.
   */

  public static void setNativeLookAndFeel() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch(Exception e) {
      System.out.println("Error setting native LAF: " + e);
    }
  }

  /** A simplified way to see a JPanel or other Container.
   *  Pops up a JFrame with specified Container as the content pane.
   */

  public static JFrame openInJFrame(Container content,
                                    int width,
                                    int height,
                                    String title,
                                    Color bgColor) {
    JFrame frame = new JFrame(title);
    frame.setBackground(bgColor);
    content.setBackground(bgColor);
    content.setForeground(Color.BLACK);
    content.setSize(width, height);
    frame.setSize(width, height);
    frame.setContentPane(content);
    frame.addWindowListener(new ExitListener());
    frame.setVisible( true );
    openFrames.add(frame);
    return(frame);
  }

  /** Uses Color.white as the background color. */

  public static JFrame openInJFrame(Container content,
                                    int width,
                                    int height,
                                    String title) {
    return(openInJFrame(content, width, height, title, Color.white));
  }

  /** Uses Color.white as the background color, and the
   *  name of the Container's class as the JFrame title.
   */

  public static JFrame openInJFrame(Container content,
                                    int width,
                                    int height) {
    return(openInJFrame(content, width, height,
                        content.getClass().getName(),
                        Color.white));
  }
  
  public static void drawOnLastFrame(List<WorldPoint> shapeArea){
	  JFrame frame = openFrames.lastElement();
	  MapPanel content = (MapPanel)frame.getContentPane();
	  content.drawCH(shapeArea);
  }

public static void closeAllFrames() {
	for(int i=0; i< openFrames.size(); i++){
		openFrames.elementAt(i).dispose();
	}
	
}
}