package eup;
/**
 * 
 */


import java.awt.GraphicsConfiguration;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
/**
 * The visual environment for the Electricity Usage Predictor. 
 * Construction (instantiation)
 * MUST take place on the EDT (Event Dispatch Thread) because Swing objects are
 * created during construction.
 * 
 * @author Ian Shef
 * 
 */
public class ElectricityUsagePredictor extends JFrame  {

    /**
     * 
     */
    private static final boolean addDebugButton = false;
    private static final boolean disableOutRedirection = false;
    private static final boolean disableErrRedirection = false;

    private static final long serialVersionUID = 1L;
//    private static final String LEFT_OUTLINE_ARROW = "\u21e6";
//    private static final String UP_OUTLINE_ARROW = "\u21e7";
//    private static final String RIGHT_OUTLINE_ARROW = "\u21e8";
//    private static final String DOWN_OUTLINE_ARROW = "\u21e9";

    JDatePickerImpl datePickerCurrentBillDate ;
    JDatePickerImpl datePickerCurrentDate     ;
    JDatePickerImpl datePickerNextBillDate    ;

    Feedbacker fb;

    /**
     * 
     */
    public ElectricityUsagePredictor() {
	super();
	setupGUI();
    }

    /**
     * @param title
     */
    public ElectricityUsagePredictor(String title) {
	super(title);
	setupGUI();
    }

    /**
     * @param gc
     */
    public ElectricityUsagePredictor(GraphicsConfiguration gc) {
	super(gc);
	setupGUI();
    }

    /**
     * @param title
     * @param gc
     */
    public ElectricityUsagePredictor(String title, GraphicsConfiguration gc) {
	super(title, gc);
	setupGUI();
    }

    private final void setupGUI() {
	addWidgets();
	connectInternalListeners();
	logJavaConfiguration();
    }

    private void logJavaConfiguration() {
	for (String key : System.getProperties().stringPropertyNames()) {
	    StringBuilder sb = new StringBuilder("Property: ");
	    sb.append(key);
	    sb.append("=");
	    sb.append(System.getProperty(key));
	    fb.log(sb, Feedbacker.TO_FILE);
	}
    }

    /**
     * Must be called while on Event Dispatch Thread (EDT).
     */
    private final void addWidgets() {
	fb = new FeedbackerImplementation();
	@SuppressWarnings("unused")
	PrintStream out0 = System.out; // Saving original System.out ;
	@SuppressWarnings("unused")
	PrintStream err0 = System.err; // Saving original System.err ;
	if (!disableOutRedirection) {
	    System.setOut(new PrintStream(new FeedbackerOutputStream(fb,
		    "<font color=\"green\">")));
	}
	if (!disableErrRedirection) {
	    System.setErr(new PrintStream(new FeedbackerOutputStream(fb,
		    "<font color=\"red\">")));
	}

	setDefaultCloseOperation(EXIT_ON_CLOSE);

	try {
	    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	} catch (ClassNotFoundException e) {
	    // Live with existing Look and Feel.
	    fb.log("ClassNotFoundException while setting Look and Feel.");
	} catch (InstantiationException e) {
	    // Live with existing Look and Feel.
	    fb.log("InstantiationException while setting Look and Feel.");
	} catch (IllegalAccessException e) {
	    // Live with existing Look and Feel.
	    fb.log("IllegalAccessException while setting Look and Feel.");
	} catch (UnsupportedLookAndFeelException e) {
	    // Live with existing Look and Feel.
	    fb.log("UnsupportedLookAndFeelException"
		    + " while setting Look and Feel.");
	}
	
	//  Create three horizontal boxes, one above the other above the other.
	//  The upper box will be used for date pickers.
	//  (The middle box could be used for selecting meter readings.)
	//  The bottom box will be used for the operations log and
	//    will get set up first.
	//
	//
	//  Pennywise Power used the "Start Read" reading on a "Date",
	//  (from smartmetertexas.com)
	//  truncated to an integer.
	//
	Box vbox  = Box.createVerticalBox() ;
	Box hbox1 = Box.createHorizontalBox() ;
	vbox.add(hbox1) ;
//	Box hbox2 = Box.createHorizontalBox() ;
//	vbox.add(hbox2) ;
	Box hbox3 = Box.createHorizontalBox() ;
	vbox.add(hbox3) ;

	hbox3.add(fb.getOperationsLog()) ;

	Properties p = new Properties();
	
	UtilDateModel modelCurrentBillDate = new UtilDateModel(
		Date.from(LocalDate.of(2018, Month.JUNE, 16).
			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;
	JDatePanelImpl datePanelCurrentBillDate = 
		new JDatePanelImpl(modelCurrentBillDate, p);
	datePickerCurrentBillDate = 
		new JDatePickerImpl(datePanelCurrentBillDate,
		new DateLabelFormatter());
	datePickerCurrentBillDate.setBorder(BorderFactory.
		createCompoundBorder(BorderFactory.
			createCompoundBorder(BorderFactory.
				createTitledBorder("Date of Most Recent Bill"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)),
				datePickerCurrentBillDate.getBorder()));
	hbox1.add(datePickerCurrentBillDate) ;
	
	UtilDateModel modelCurrentDate = new UtilDateModel(
		Date.from(LocalDate.now().
			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;
	JDatePanelImpl datePanelCurrentDate = 
		new JDatePanelImpl(modelCurrentDate, p);
	datePickerCurrentDate = 
		new JDatePickerImpl(datePanelCurrentDate,
		new DateLabelFormatter());
	datePickerCurrentDate.setBorder(BorderFactory.
		createCompoundBorder(BorderFactory.
			createCompoundBorder(BorderFactory.
				createTitledBorder(
					"Current Date (rarely changed)"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)),
				datePickerCurrentDate.getBorder()));
	hbox1.add(datePickerCurrentDate) ;
	
	UtilDateModel modelNextBillDate = new UtilDateModel(
		Date.from(LocalDate.of(2018, Month.JULY, 16).
			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;
	JDatePanelImpl datePanelNextBillDate = 
		new JDatePanelImpl(modelNextBillDate, p);
	datePickerNextBillDate = 
		new JDatePickerImpl(datePanelNextBillDate,
		new DateLabelFormatter());
	datePickerNextBillDate.setBorder(BorderFactory.
		createCompoundBorder(BorderFactory.
			createCompoundBorder(BorderFactory.
				createTitledBorder(
					"Expected Date of Next Bill"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)),
				datePickerNextBillDate.getBorder()));
	hbox1.add(datePickerNextBillDate) ;

	add(vbox) ;
	pack();
	fb.log("Making visible.", Feedbacker.TO_FILE + 
		Feedbacker.TO_GUI);
	setVisible(true);
    }

    /**
     * Must be called while executing on Event Dispatch Thread (EDT).
     */
    private void connectInternalListeners() {
	fb.log("GUI controller setup commencing.", Feedbacker.TO_FILE);
	connectInternalListeners1();
	fb.log("GUI controller setup completed.", Feedbacker.TO_FILE);
    }

    void connectInternalListeners1() {

	if (addDebugButton) {
	    JButton debugButton = new JButton("debug");
	    debugButton.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
		    fb.log("=====            END            =====\n",
			    Feedbacker.TO_OUT);
		}
	    });
	}

    } // end of connectListeners1().


    @Override
    public String toString() {
	return this.getClass().getName();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
	//
	// ElectricityUsagePredictor extends JFrame and 
	// thus must be set up via the
	// EDT (Event Dispatch Thread) !
	//
	// See
	// http://weblogs.java.net/blog/alexfromsun/archive/2006/01/
	// debugging_swing_2.html
	// for how to do this with a RunnableFuture that can provide a
	// result back to the caller.
	//
	java.util.concurrent.atomic.AtomicReference<ElectricityUsagePredictor>
	gui2 = new 
	java.util.concurrent.atomic.AtomicReference
	<ElectricityUsagePredictor>() ;
	try {
	    javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
		@Override
		public void run() {
		    ElectricityUsagePredictor gui =   new 
			    ElectricityUsagePredictor(
			    "Electricity Usage Predictor");
		    gui.fb.log("Finished setting up GUI.");
		    gui2.set(gui) ;
		}
	    });
	} catch (InterruptedException e) {
	    e.printStackTrace();
	    // Restore the interrupted status
	    Thread.currentThread().interrupt();
	    // Now allow the main thread to exit.
	} catch (InvocationTargetException e) {
	    e.printStackTrace();
	    // Now allow the main thread to exit.
	}
	Predictor predictor = new Predictor.Builder().
		currentBillDate(LocalDate.of(2018, Month.JULY, 10)).
		currentBillMeterReading(24512).
		currentDate(LocalDate.of(2018, Month.JULY, 28)).
		currentMeterReading(25189).
		nextBillDate(LocalDate.of(2018, Month.AUGUST, 9)).
//		currentBillDate(LocalDate.of(gui2.datePickerCurrentBillDate.getModel().getYear(), gui2.datePickerCurrentBillDate.getModel().getMonth(), gui2.datePickerCurrentBillDate.getModel().getDay())).
		build() ;
	/*
	 
 Date	  Delta Days	Start Read	Usage
07/10/18		24512	
07/28/18	18	25189	          677
08/09/18	30	25640	         1128


What is the best way to convert a java.util.Date object to 
the new JDK 8/JSR-310 java.time.LocalDate?

Date input = new Date();
LocalDate date = input.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

	 */
	System.out.println() ;
	System.out.print("Current Bill Date: ") ;
	System.out.println(predictor.getDateBillCurrent()) ;
	System.out.print("Current Bill Meter Reading: ") ;
	System.out.println(predictor.getMeterReadingBillCurrent()) ;
	System.out.println() ;
	System.out.print("Current      Date   : ") ;
	System.out.println(predictor.getDateCurrent()) ;
	System.out.print("Current     Meter Reading : ") ;
	System.out.println(predictor.getMeterReadingCurrent()) ;
	System.out.println() ;
	System.out.print("Next    Bill Date   : ") ;
	System.out.println(predictor.getDateBillNext()) ;
	int predictedUsage = predictor.predictUsage() ;
	if ((predictedUsage>=500) && (predictedUsage<=1000)) {
	    System.out.print("Predicted   Usage : ") ;
	    System.out.println(predictedUsage) ;
	} else {
	    System.err.print("Predicted   Usage : ") ;
	    System.err.println(predictedUsage) ;
	}
	System.out.println() ;
	System.out.println(">> "+predictor.getDateBillCurrent().toString()) ;
    }
}

class DateLabelFormatter extends AbstractFormatter {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
//    private String datePattern = "yyyy-MM-dd";
    //
    // Pattern gives: 
    // day_name month_name day_number_of_month, 4_digit_year_number
    //
    private String datePattern = "EEEEEEEEE LLLLLLLLL dd, yyyy";
    private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);

    @Override
    public Object stringToValue(String text) throws ParseException {
        return dateFormatter.parseObject(text);
    }

    @Override
    public String valueToString(Object value) {
        if (value != null) {
            Calendar cal = (Calendar) value;
            return dateFormatter.format(cal.getTime());
        }

        return "";
    }
}


