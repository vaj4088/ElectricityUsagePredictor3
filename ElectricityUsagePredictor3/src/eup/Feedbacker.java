package eup;

import javax.swing.JComponent ;
import java.awt.Color ;

public interface Feedbacker {

    public static final int TO_GUI = 1;
    public static final int TO_FILE = 2;
    public static final int TO_OUT = 4;
    public static final int FLUSH = 8;

    public void log(final Object ob) ;

    public void log(final Object ob, int where) ;
    
    public void logBare(final Object ob, int where) ;

    public void progressAnnounce(final boolean enable,
	    final String info) ;

    public void progressAnnounce(final boolean enable) ;

    public void progressAnnounce(final int percent, final String info);
    
    public JComponent getProgressBar();
    public JComponent getProgressBar(final Color color) ;

    public JComponent getOperationsLog();
    
    public void activityAnnounce(
                                 final int currentPercent, 
                                 final String info, 
                                 final int maxPercent
                                ) ;
    
}