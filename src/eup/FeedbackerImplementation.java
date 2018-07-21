/**
 * 
 */
package eup;

import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * @author Ian Shef
 * 
 */
public class FeedbackerImplementation implements Feedbacker {

    // private static final String eol = System.getProperty("line.separator");
    private static final String HTMLEol = "<br />";
    // End of line.
    private static final int PIXELS_WIDTH_CHAR = 8;
    private static final int CHARS_IN_LINE = 80;
    private static final int PIXELS_WIDTH_LINE = PIXELS_WIDTH_CHAR
	    * CHARS_IN_LINE;
    private static final int PIXELS_PER_LINE = 15;
    private static final int LINES_IN_GUI_LOG = 10;
    private static final int PIXELS_VERTICAL_IN_GUI_LOG = PIXELS_PER_LINE
	    * LINES_IN_GUI_LOG;
    private static final Dimension DIMENSION_GUI_LOG = new Dimension(
	    PIXELS_WIDTH_LINE, PIXELS_VERTICAL_IN_GUI_LOG);
    private static final String defaultLogFont = System
	    .getProperty("line.separator")
	    + "<font face=\"Arial,Helvetica,sanserif\" "
	    + "size=\"3\" "
	    + "color=\"black\">";
    private static final String initialLogString = "<html><body>"
	    + "<hr />"
	    + "<p align=\"center\">"
	    + defaultLogFont
	    + "<b>========= "
	    + "&nbsp;&nbsp;&nbsp;&nbsp;"
	    + "&nbsp;&nbsp;&nbsp;&nbsp;"
	    + "OPERATIONS LOG"
	    + "&nbsp;&nbsp;&nbsp;&nbsp;"
	    + "&nbsp;&nbsp;&nbsp;&nbsp;"
	    + "=========</b>"
	    + "</p>"
	    + HTMLEol
	    + "<hr />"
	    + defaultLogFont
	    + Version.getInstance().toString()
	    + HTMLEol
	    + defaultLogFont
	    ;

    private transient OutputStream logStream;
    StringBuilder logString = new StringBuilder(32768).append(initialLogString);

    //
    // Next lines are OK as long as this class is constructed on the
    // EDT (Event Dispatch Thread).
    //
    JTextComponent logText = new JEditorPane("text/html", logString.toString());
    // JTextComponent logText = new JTextArea(10, 120);
    // JTextArea logText = new JTextArea(10, 120);
    JScrollPane log = new JScrollPane(logText,
	    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
	    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    JProgressBar progressText = new JProgressBar(0, 100);
    JProgressBar progress = new JProgressBar(0, 100);

    @Override
    public JComponent getProgressBar() {
	Box progressBox = Box.createVerticalBox();
	progressBox.add(progressText);
	progressBox.add(progress);
	progress.setIndeterminate(false);
	progress.setValue(0) ;
	progressText.setForeground(Color.LIGHT_GRAY) ;
	progressText.setStringPainted(true);
	progressText.setBorderPainted(false);
	progress.setStringPainted(false);
	return progressBox;
    }

    @Override
    public JComponent getOperationsLog() {
	Box logBox = Box.createVerticalBox();
	log.setBorder(BorderFactory.createCompoundBorder(BorderFactory
		.createCompoundBorder(BorderFactory
			.createTitledBorder("Operations Log"), BorderFactory
			.createEmptyBorder(5, 5, 5, 5)), log.getBorder()));
	log.setPreferredSize(DIMENSION_GUI_LOG);
//	logText.setPreferredSize(DIMENSION_GUI_LOG);
//	logBox.setMaximumSize(DIMENSION_GUI_LOG) ;
	logBox.add(log);
	return logBox;
    }

    /*
     * Implementation of Feedbacker
     * =========================================================================
     */

    /*
     * Implementation of Feedbacker, uses the toString() method of any provided
     * Object to produce text on a GUI log and a file log. A line terminator is
     * suffixed.
     * 
     * @see schedule.Feedbacker#log(java.lang.Object)
     */
    @Override
    public void log(final Object ob) {
	log(ob, Feedbacker.TO_GUI + Feedbacker.TO_FILE, true);
    }

    /*
     * Implementation of Feedbacker, uses the toString() method of any provided
     * Object to produce text on a log. The location of the log is determined by
     * the int "where", which is the sum of one or more constants. A line
     * terminator is suffixed.
     * 
     * @see schedule.Feedbacker#log(java.lang.Object)
     */
    @Override
    public void log(final Object ob, final int where) {
	log(ob, where, true);
    }

    /*
     * Implementation of Feedbacker, uses the toString() method of any provided
     * Object to produce text on a log. The location of the log is determined by
     * the int "where", which is the sum of one or more constants. A line
     * terminator is NOT suffixed.
     * 
     * @see schedule.Feedbacker#log(java.lang.Object)
     */
    @Override
    public void logBare(final Object ob, final int where) {
	log(ob, where, false);
    }

    /*
     * Helper for Feedbacker, uses the toString() method of any provided Object
     * to produce text on a log. The location of the log is determined by the
     * int "where", which is the sum of one or more constants. A line terminator
     * is suffixed if terminated is true.
     * 
     * @see schedule.Feedbacker#log(java.lang.Object)
     */
    private void log(final Object ob, final int where, boolean terminated) {
	final String terminatedString = ob.toString()
		+ (terminated ? HTMLEol : "");
	if (0 != (where & Feedbacker.TO_GUI)) {
	    SwingUtilities.invokeLater(new Runnable() {
		@Override
		public void run() {
		    logString.append(terminatedString);
		    logText.setText(logString.toString());
		}
	    });
	}
	if (0 != (where & Feedbacker.TO_OUT)) {
	    if (terminated) {
		System.out.println(ob);
	    } else {
		System.out.print(ob);
	    }
	    if (0 != (where & Feedbacker.FLUSH)) {
		System.out.flush();
	    }

	}
	if (0 != (where & Feedbacker.TO_FILE)) {
	    if (null == this.logStream) {
		Calendar now = Calendar.getInstance();
		long dateTime = now.get(Calendar.YEAR);
		dateTime = (dateTime * 100) + now.get(Calendar.MONTH) + 1;
		dateTime = (dateTime * 100) + now.get(Calendar.DATE);
		dateTime = (dateTime * 100) + now.get(Calendar.HOUR_OF_DAY);
		dateTime = (dateTime * 100) + now.get(Calendar.MINUTE);
		dateTime = (dateTime * 100) + now.get(Calendar.SECOND);
		String name = this.getClass().getSimpleName() + "_log_"
			+ Long.toString(dateTime) + ".html";
		File logFile = new File(name);
		try {
		    this.logStream = new BufferedOutputStream(
			    new FileOutputStream(logFile));
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (SecurityException e) {
		    e.printStackTrace();
		}
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    @Override
		    public void run() {
			shutdown();
		    }
		});
		try {
		    this.logStream.write(initialLogString.getBytes());
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	    byte[] b = terminatedString.getBytes();
	    try {
		this.logStream.write(b);
		if (0 != (where & Feedbacker.FLUSH)) {
		    this.logStream.flush();
		}
	    } catch (IOException e) {
		e.printStackTrace();
	    }

	}
    }

    @Override
    public void progressAnnounce(final boolean enable, final String info) {
	SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		progress.setValue(0);
		progress.setIndeterminate(enable);
		progressText.setString(info);
	    }
	});
    }

    @Override
    public void progressAnnounce(final boolean enable) {
	progressAnnounce(enable, "");
    }

    @Override
    public void progressAnnounce(final int percent, final String info) {
	SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		String processedInfo = info;
		if ((null == info) || (info.isEmpty()))
		    processedInfo = " ";
		progress.setIndeterminate(false);
		progress.setValue(percent);
		progressText.setString(processedInfo);
	    }
	});
    }

    /*
     * End of implementation of Feedbacker =====================================
     */

    void shutdown() {
	if (null != this.logStream) {
	    try {
		logStream.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    public static String getDefaultlogfont() {
	return defaultLogFont;
    }

    @Override
    public String toString() {
	return this.getClass().getName();
    }

    public static void main(String[] args) {
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
	    @SuppressWarnings("finally")
	    @Override
	    public void run() {
		final int where = Feedbacker.TO_GUI + Feedbacker.TO_FILE
			+ Feedbacker.TO_OUT;
		FeedbackerImplementation fb = new FeedbackerImplementation();
		JFrame frame = new JFrame(fb.toString());
		frame.getContentPane().add(fb.getOperationsLog());
		fb.log("Test Line 1", where);
		fb.log("Test Line 2", where);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		PrintStream out0 = System.out;
		PrintStream err0 = System.err;
		System.setOut(new PrintStream(new FeedbackerOutputStream(fb,
			"<font color=\"green\">")));
		System.setErr(new PrintStream(new FeedbackerOutputStream(fb,
			"<font color=\"red\">")));
		System.out.println("This text should be redirected.");
		try {
		    throw new AssertionError(
			    "Intentional Exception, should be redirected.");
		} catch (AssertionError e) {
		    e.printStackTrace() ;
		} finally {
		    System.setOut(out0);
		    System.setErr(err0);
		    System.out
			    .println("This text should appear " + 
				    "normally on console.");
		    throw new AssertionError(
			    "Intentional Exception, should appear normally.");
		}
	    }

	});
    }

}
