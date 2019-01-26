/**
 * 
 */
package eup;

import java.awt.Color;

import javax.swing.JComponent;

/**
 * @author Ian Shef
 *
 */
public class FeedbackerSkeleton implements Feedbacker {
    
    private void msg(Object ob) {System.out.println(ob) ;} 

    @Override
    public void log(Object ob) {
	msg(ob) ;
    }

    @Override
    public void log(Object ob, int where) {
	msg(ob) ;
    }

    @Override
    public void logBare(Object ob, int where) {
	msg(ob) ;
    }

    @Override
    public void progressAnnounce(boolean enable, String info) {
	System.out.print("Enable is ") ;
	System.out.print(enable) ;
	System.out.print(" with info: ") ;
	msg(info) ;
    }

    @Override
    public void progressAnnounce(boolean enable) {
	System.out.print("Enable is ") ;
	System.out.println(enable) ;
    }

    @Override
    public void progressAnnounce(int percent, String info) {
	System.out.print("Percent is ") ;
	System.out.print(percent) ;
	System.out.print(" with info: ") ;
	msg(info) ;
    }

    @Override
    public JComponent getProgressBar(Color color) {
	return null;
    }

    @Override
    public JComponent getProgressBar() {
	return null;
    }

    @Override
    public JComponent getOperationsLog() {
	return null;
    }
    
}
