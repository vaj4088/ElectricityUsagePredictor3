package eup;
/**
 * 
 */


import java.awt.Color;
import java.awt.Font;
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
import java.util.concurrent.CountDownLatch;

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
public class ElectricityUsagePredictor
extends JFrame
implements ActionListener {

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

    static
    	java.util.concurrent.atomic.AtomicReference<ElectricityUsagePredictor>
    		guiAtomicReference = new 
    		java.util.concurrent.atomic.AtomicReference
    		<ElectricityUsagePredictor>() ;
    
    private CountDownLatch cdl ;
    private Date cBD ;
    private Date cD ;
    private Date nBD ;

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
	
	//  Create two horizontal boxes, one above the other.
	//  The upper box will be used for date pickers and a button.
	//  The bottom box will be used for the operations log and
	//    will get set up first.
	//
	//
	//  Pennywise Power used the "Start Read" reading on a "Date",
	//  (from smartmetertexas.com)
	//  truncated to an integer.
	//  So does Energy Express.
	//
	Box vbox  = Box.createVerticalBox() ;
	Box hbox1 = Box.createHorizontalBox() ;
	vbox.add(hbox1) ;
	Box hbox3 = Box.createHorizontalBox() ;
	vbox.add(hbox3) ;

	hbox3.add(fb.getOperationsLog()) ;

	Properties p = new Properties();
	
	UtilDateModel modelCurrentBillDate = new UtilDateModel(
		Date.from(LocalDate.of(2018, Month.JULY, 16).
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
		Date.from(LocalDate.of(2018, Month.AUGUST, 16).
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
	
	JButton jb = new JButton("GO (predict)") ; 
	jb.setBackground(Color.GREEN) ;
	jb.setFont(jb.getFont().deriveFont(Font.BOLD)) ;
	hbox1.add(jb) ;

	add(vbox) ;
	pack();
	fb.log("Making visible.", Feedbacker.TO_FILE + 
		Feedbacker.TO_GUI);
	setVisible(true);
//	datePickerCurrentBillDate.addActionListener(this) ;
//	datePickerCurrentDate.    addActionListener(this) ;
//	datePickerNextBillDate.   addActionListener(this) ;
	jb.                       addActionListener(this) ;
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
	Integer h ;
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
	try {
	    javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
		@Override
		public void run() {
		    ElectricityUsagePredictor gui = 
			    new ElectricityUsagePredictor(
			    "Electricity Usage Predictor");
		    gui.fb.log("Finished setting up GUI.");
		    guiAtomicReference.set(gui);
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
//	LocalDate date = LocalDate.of(2018, 8, 1) ;
//	java.time.format.DateTimeFormatter dTF = 
//		java.time.format.DateTimeFormatter.ofPattern(
//	        "MM'%2F'dd'%2F'yyyy") ;
//	String s = date.format(dTF) ;
//	s = "The date of 20180801 has been formatted as " + s ;
//	ElectricityUsagePredictor gui = gui2.get() ; 
//	gui.msgEDT(s) ;
	while (true) {
	    ElectricityUsagePredictor gui = guiAtomicReference.get() ;
	    gui.cdl = new CountDownLatch(1) ;
	    try {
		gui.cdl.await() ;
	    } catch (InterruptedException e) {
		e.printStackTrace();
		// Restore the interrupted status
		Thread.currentThread().interrupt();
	    }
	    SmartMeterTexasDataCollector smtdc = 
		    new SmartMeterTexasDataCollector() ;
	    //
	    // cBDLD is current current Bill Date as a Local Date
	    //
	    LocalDate cBDLD = gui.cBD.toInstant().
		    atZone(ZoneId.systemDefault()).
		    toLocalDate() ;
	    //
	    // cDLD is current date as a Local Date.
	    //
	    LocalDate cDLD = gui.cD.toInstant().
		    atZone(ZoneId.systemDefault()).
		    toLocalDate() ;
	    int currentMeterReading     = 
		    (smtdc.new GetData(cDLD)).getStartRead() ;
	    int currentBillMeterReading = 
		    (smtdc.new GetData(cBDLD)).getStartRead() ;
//	    int currentBillMeterReading = 0 ; // TEMPORARY: DELETE, + UNCOMMENT ABOVE.
	    Predictor predictor = new Predictor.Builder().
		    currentBillDate(cBDLD).
		    currentBillMeterReading(currentBillMeterReading).
		    currentDate(cDLD).
		    currentMeterReading(currentMeterReading).
		    nextBillDate(gui.nBD.toInstant().
			    atZone(ZoneId.systemDefault()).toLocalDate()).
		    build();
	    //
	    //  Above does not require EDT.
	    //
	    //  Below outputs to gui so should be on EDT.
	    //
	    gui.msgEDT("");
	    gui.msgNoNewlineEDT("Current Bill Date: ");
	    gui.msgEDT(predictor.getDateBillCurrent());
	    gui.msgNoNewlineEDT("Current Bill Meter Reading: ");
	    h = new Integer(predictor.getMeterReadingBillCurrent()) ;
	    gui.msgEDT(h) ;
	    gui.msgEDT("");
	    gui.msgNoNewlineEDT("Current      Date   : ");
	    gui.msgEDT(predictor.getDateCurrent());
	    gui.msgNoNewlineEDT("Current     Meter Reading : ");
	    h = new Integer(predictor.getMeterReadingCurrent()) ;
	    gui.msgEDT(h);
	    gui.msgEDT("");
	    gui.msgNoNewlineEDT("Next    Bill Date   : ");
	    gui.msgEDT(predictor.getDateBillNext());
	    gui.msgNoNewlineEDT("Days Remaining : ") ;
	    gui.msgEDT(Long.valueOf(predictor.daysRemaining())) ;
	    int predictedUsage = predictor.predictUsage() ;
	    PrintStream where = System.err ;
	    if ((predictedUsage>=500) && (predictedUsage<=1000)) {
		where = System.out ;
	    }
	    where.print("Predicted   Usage : ") ;
	    where.println(predictedUsage) ;
//	    gui.msgEDT("") ;
//	    gui.msgEDT(">> "+predictor.getDateBillCurrent().toString()) ;
	}
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
// 	msg("Action Event " + ae.toString()) ;
 	ElectricityUsagePredictor gui = guiAtomicReference.get() ;
 	JDatePickerImpl dPCBD = gui.datePickerCurrentBillDate ;
 	JDatePickerImpl dPCD  = gui.datePickerCurrentDate ;
 	JDatePickerImpl dPNBD = gui.datePickerNextBillDate ;
 	cBD = (Date) dPCBD.getModel().getValue() ;
 	cD  = (Date)  dPCD.getModel().getValue() ;
 	nBD = (Date) dPNBD.getModel().getValue() ;
 	//
 	//  Above should be on EDT.
 	//
 	//
 	//  Below does not require EDT and
 	//  gets us off of the EDT.
 	//
 	cdl.countDown() ;
     }

    /**
     * A convenience method for displaying a line of text on System.out.
     * 
     * @param ob
     *            An <tt>Object</tt> or a <tt>String</tt> to be displayed on
     *            System.out. If an <tt>Object</tt>, its toString() method will
     *            be called.
     */
    void msg(Object ob) {
	if (null == fb) {
	    System.out.println(ob);
	} else {
	    fb.log(ob, Feedbacker.TO_OUT + Feedbacker.TO_FILE);
	}
    }
    
    /**
     * A convenience method for displaying a line of text on System.out.
     * 
     * @param ob
     *            An <tt>int</tt> to be displayed on
     *            System.out.
     */
    void msg(int ob) {
	msg(Integer.toString(ob)) ;
    }
    
    /**
     * A convenience method for displaying a line of text on System.out but
     * without a newline.
     * 
     * @param ob
     *            An <tt>Object</tt> or a <tt>String</tt> to be displayed on
     *            System.out. If an <tt>Object</tt>, its toString() method will
     *            be called.
     */
    void msgNoNewline(Object ob) {
	if (null == fb) {
	    System.out.print(ob);
	} else {
	    fb.logBare(ob, Feedbacker.TO_OUT + Feedbacker.TO_FILE);
	}
    }
    
    /**
     * A convenience method for displaying a line of text on System.out
     * using the Event Dispatch Thread.
     * 
     * @param ob
     *            An <tt>Object</tt> or a <tt>String</tt> to be displayed on
     *            System.out. If an <tt>Object</tt>, its toString() method will
     *            be called.
     */
    void msgEDT(Object ob) {
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		msg(ob) ;
	    }
	    
	});
    }
    
    /**
     * A convenience method for displaying a line of text 
     * without appending a newline character
     * on System.out
     * using the Event Dispatch Thread.
     * 
     * @param ob
     *            An <tt>Object</tt> or a <tt>String</tt> to be displayed on
     *            System.out. If an <tt>Object</tt>, its toString() method will
     *            be called.
     */
    void msgNoNewlineEDT(Object ob) {
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		msgNoNewline(ob) ;
	    }
	    
	});
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
	private SimpleDateFormat dateFormatter = 
		new SimpleDateFormat(datePattern);

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
}


