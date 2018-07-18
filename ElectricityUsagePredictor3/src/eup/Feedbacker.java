package eup;

import javax.swing.JComponent;

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

    public JComponent getOperationsLog();
    
}