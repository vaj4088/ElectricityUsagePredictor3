package eup;
/**
 * 
 */


import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
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
    private static final boolean disableOutRedirection = false;
    private static final boolean disableErrRedirection = false;
    
    private static final int startProgress1 = 10 ;
    private static final int changeProgress = 10 ;
    private static final int startProgress2 = 
	    startProgress1 + 5 * changeProgress ;
    
    private static final long serialVersionUID = 1L;

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
    private JButton jb ;
    private volatile Date cBD ;
    private volatile Date cD ;
    private volatile Date nBD ;
    
	/*
	 * Used for getting stored billing date information.
	 */
	
    Map<?extends String, ? extends String> store = 
	    CommonPreferences.getPreferences() ;
    Map<? extends String, ? extends Setting> settingsMap = 
	    Setting.getSettingsMap() ;

	/*
	 * End of used for getting stored billing date information.
	 */
    
//    public static final String MOST_RECENT_BILL_DATE_YEAR  = 
//	    "mostRecentBillDateYear" ;
//    public static final String MOST_RECENT_BILL_DATE_MONTH = 
//	    "mostRecentBillDateMonth" ;
//    public static final String MOST_RECENT_BILL_DATE_DAY   = 
//	    "mostRecentBillDateDay" ;
//    public static final String NEXT_BILL_DATE_YEAR  = "nextBillDateYear" ;
//    public static final String NEXT_BILL_DATE_MONTH  = "nextBillDateMonth" ;
//    public static final String NEXT_BILL_DATE_DAY  = "nextBillDateDay" ;

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
	//  The upper box will be used for date pickers and a button.
	//  The middle box will have a progress indication.
	//  The bottom box will be used for the operations log.
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
	
	Box hbox2 = Box.createHorizontalBox() ;
	JComponent pb = fb.getProgressBar(Color.GREEN) ;
	pb.setBorder(BorderFactory.
		createCompoundBorder(BorderFactory.
			createCompoundBorder(BorderFactory.
				createTitledBorder("Progress"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)),
				pb.getBorder()));
	Box hbox3 = Box.createHorizontalBox() ;
	vbox.add(hbox2) ;
	vbox.add(hbox3) ;

	hbox2.add(pb) ;
	hbox3.add(fb.getOperationsLog()) ;

	Properties p = new Properties();
	
	/*
	 * Getting stored billing date information.
	 */
	
	UtilDateModel modelCurrentBillDate = new UtilDateModel(
		Date.from(LocalDate.of(
	       Integer.parseInt(store.get(Setting.MOST_RECENT_BILL_DATE_YEAR)), 
	       Integer.parseInt(store.get(Setting.MOST_RECENT_BILL_DATE_MONTH)), 
	       Integer.parseInt(store.get(Setting.MOST_RECENT_BILL_DATE_DAY))).
			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;

	/*
	 * Got the stored billing date information.
	 */
	
//	UtilDateModel modelCurrentBillDate = new UtilDateModel(
//		Date.from(LocalDate.of(2019, Month.JANUARY, 9).
//			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;
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
//	Box vboxCurrent = Box.createVerticalBox() ;
//	hbox1.add(vboxCurrent) ;
//	vboxCurrent.add(new )
	hbox1.add(datePickerCurrentDate) ;
	
	/*
	 * Getting stored billing date information.
	 */
	
	UtilDateModel modelNextBillDate = new UtilDateModel(
		Date.from(LocalDate.of(
		    Integer.parseInt(store.get(Setting.NEXT_BILL_DATE_YEAR)), 
		    Integer.parseInt(store.get(Setting.NEXT_BILL_DATE_MONTH)), 
		    Integer.parseInt(store.get(Setting.NEXT_BILL_DATE_DAY))).
			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;

	/*
	 * Got the stored billing date information.
	 */
	
//	UtilDateModel modelNextBillDate = new UtilDateModel(
//		Date.from(LocalDate.of(2019, Month.FEBRUARY, 8).
//			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;
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
	
	jb = new JButton("GO (predict)") ; 
	jb.setBackground(Color.GREEN) ;
	jb.setFont(jb.getFont().deriveFont(Font.BOLD)) ;
	hbox1.add(jb) ;

	add(vbox) ;
	pack();
	fb.log("Making visible.", Feedbacker.TO_FILE + 
		Feedbacker.TO_GUI);
	setVisible(true);
	jb.                       addActionListener(this) ;
    } 

    /**
     * Must be called while executing on Event Dispatch Thread (EDT).
     */
    private void connectInternalListeners() {
	fb.log("GUI controller setup commencing.", Feedbacker.TO_FILE);
	fb.log("GUI controller setup completed.", Feedbacker.TO_FILE);
    }

    @Override
    public String toString() {
	return this.getClass().getName();
    }

    /**
     * To be called from the EDT.
     */
    static Feedbacker getFeedbacker() {
	return ((ElectricityUsagePredictor)(Frame.getFrames()[0])).fb ; 
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
	Integer h ;
	boolean changedTheDate ;
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
	while (true) {
	    ElectricityUsagePredictor gui = guiAtomicReference.get() ;
	    /*
	     * Workaround for enabling the date pickers.
	     */
	    gui.datePickerCurrentBillDate.getComponent(1).setEnabled(true) ;
	    gui.datePickerCurrentDate.getComponent(1).setEnabled(true) ;
	    gui.datePickerNextBillDate.getComponent(1).setEnabled(true) ;
	    /*
	     * End workaround for enabling the date pickers.
	     */
	    gui.jb.setEnabled(true);
	    gui.fb.progressAnnounce(false, "Waiting for GO (predict)") ;
	    gui.cdl = new CountDownLatch(1) ;
	    try {
		//
		//  WAIT HERE until the GO button is pushed.
		//
		gui.cdl.await() ;
		//
		//  CONTINUE because the GO button was pushed.
		//
	    } catch (InterruptedException e) {
		e.printStackTrace();
		// Restore the interrupted status
		Thread.currentThread().interrupt();
	    }
	    /*
	     * Workaround for disabling the date pickers.
	     */
	    gui.datePickerCurrentBillDate.getComponent(1).setEnabled(false) ;
	    gui.datePickerCurrentDate.getComponent(1).setEnabled(false) ;
	    gui.datePickerNextBillDate.getComponent(1).setEnabled(false) ;
	    /*
	     * End workaround for disabling the date pickers.
	     */
	    gui.jb.setEnabled(false) ;
	    gui.fb.activityAnnounce(
		    startProgress1, 
		    "Starting...", 
		    startProgress1+changeProgress
		    ) ;
	    //
	    // cBDLD is current Bill Date as a Local Date
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
	    
	    /*
	     * nBDLD is next Bill Date as a LocalDate.
	     */
	    LocalDate nBDLD = gui.nBD.toInstant().
		    atZone(ZoneId.systemDefault()).toLocalDate() ;
	    
	    /*
	     *  Store current billing date for future use.
	     */
	    writeToCache(
		    gui.settingsMap, 
		    Setting.MOST_RECENT_BILL_DATE_YEAR, 
		    Setting.MOST_RECENT_BILL_DATE_MONTH, 
		    Setting.MOST_RECENT_BILL_DATE_DAY, 
		    Setting.MOST_RECENT_BILL_DATE_READING,
		    Setting.MOST_RECENT_BILL_READING_VALIDITY,
		    cBDLD
		    ) ;
	    /*
	     * End of storing year, month and day of the current billing date.
	     */
	    
	    /*
	     *  Store next billing date for future use.
	     */
	    writeToCache(
		    gui.settingsMap, 
		    Setting.NEXT_BILL_DATE_YEAR, 
		    Setting.NEXT_BILL_DATE_MONTH, 
		    Setting.NEXT_BILL_DATE_DAY, 
		    null,
		    null,
		    nBDLD
		    ) ;
	    /*
	     * End of storing year, month and day of the next billing date.
	     */

	    /*
	     * Get data for the current date meter reading.
	     */
	    int currentMeterReading ;
	    LocalDate currentDateUsed ;
	    SmartMeterTexasDataCollector gdcDLD = null;
	    boolean usedCurrentDateReadingCache = false ;
	    if (canUseCachedValue(
		    gui.settingsMap,
		    Setting.CURRENT_DATE_YEAR,
		    Setting.CURRENT_DATE_MONTH,
		    Setting.CURRENT_DATE_DAY,
		    Setting.CURRENT_READING_VALIDITY,
		    cDLD
		    )) {
		currentMeterReading = 		
			Integer.parseInt(
				CommonPreferences.get(
				gui.settingsMap.
				get(Setting.CURRENT_DATE_READING)
				)) ;
		currentDateUsed = cDLD ;
		changedTheDate = false ;
		usedCurrentDateReadingCache = true ;
	    } else {
		gdcDLD = 
			new SmartMeterTexasDataCollector.Builder().
			date(cDLD).
			startProgressAt(startProgress1).
			changeProgressBy(changeProgress).
			labelTheProgress("Getting data for current date.").
			build() ;
		gdcDLD.setFeedbacker(gui.fb) ;
		currentMeterReading     = 
			gdcDLD.getStartRead() ;
		writeToCache(
			gui.settingsMap, 
			Setting.CURRENT_DATE_YEAR,
			Setting.CURRENT_DATE_MONTH,
			Setting.CURRENT_DATE_DAY, 
			Setting.CURRENT_DATE_READING,
			Setting.CURRENT_READING_VALIDITY,
			gdcDLD.getDate()
			) ;
		CommonPreferences.set(
			gui.settingsMap.
			get(Setting.CURRENT_DATE_READING),
			Integer.toString(currentMeterReading)
			) ;
		CommonPreferences.set(
			gui.settingsMap.
			get(Setting.CURRENT_READING_VALIDITY),
			Setting.VALID
			) ;
		currentDateUsed = gdcDLD.getDate() ;
		changedTheDate = gdcDLD.isDateChanged() ;
		usedCurrentDateReadingCache = false ;
	    }
	    /*
	     * End of
	     * get data for the current date meter reading.
	     */
	    
	    /*
	     * Get data for the current bill date meter reading.
	     */
	    int currentBillMeterReading ;
	    LocalDate currentBillDateUsed ;
	    SmartMeterTexasDataCollector gdcBDLD = null ;
	    boolean usedMostRecentBillReadingCache = false ;
	    if (canUseCachedValue(
		    gui.settingsMap,
		    Setting.MOST_RECENT_BILL_DATE_YEAR,
		    Setting.MOST_RECENT_BILL_DATE_MONTH,
		    Setting.MOST_RECENT_BILL_DATE_DAY,
		    Setting.MOST_RECENT_BILL_READING_VALIDITY,
		    cBDLD
		    )) {
		currentBillMeterReading = 
			Integer.parseInt(
				CommonPreferences.get(
				gui.settingsMap.
				get(Setting.MOST_RECENT_BILL_DATE_READING)
				)) ;
		currentBillDateUsed = cBDLD ;
		usedMostRecentBillReadingCache = true ;
	    } else {
		    gdcBDLD = new SmartMeterTexasDataCollector.Builder()
			    .date(cBDLD)
			    .startProgressAt(startProgress2)
			    .changeProgressBy(changeProgress)
			    .labelTheProgress(
				    "Getting data for most " +
				    "recent billing date.")
			    .build();
		    gdcBDLD.setFeedbacker(gui.fb) ;
		    currentBillMeterReading = gdcBDLD.getStartRead();
			writeToCache(
				gui.settingsMap, 
				Setting.MOST_RECENT_BILL_DATE_YEAR,
				Setting.MOST_RECENT_BILL_DATE_MONTH,
				Setting.MOST_RECENT_BILL_DATE_DAY,
				Setting.MOST_RECENT_BILL_DATE_READING,
				Setting.MOST_RECENT_BILL_READING_VALIDITY,
				gdcBDLD.getDate()
				) ;
			CommonPreferences.set(
				gui.settingsMap.
				get(Setting.MOST_RECENT_BILL_DATE_READING),
				Integer.toString(currentBillMeterReading)
				) ;
			CommonPreferences.set(
				gui.settingsMap.
				get(Setting.MOST_RECENT_BILL_READING_VALIDITY),
				Setting.VALID
				) ;
		    currentBillDateUsed = gdcBDLD.getDate();
		    usedMostRecentBillReadingCache = false ;
	    }
	    /*
	     * End of
	     * get data for the current bill date meter reading.
	     */

	    Predictor predictor = new Predictor.Builder().
		    currentBillDate(currentBillDateUsed).
		    currentBillMeterReading(currentBillMeterReading).
		    currentDate(currentDateUsed).
		    currentMeterReading(currentMeterReading).
		    nextBillDate(nBDLD).
		    build();
	    String nonbreakingSpace = "\u00A0" ;
	    String fiveNonBreakingSpaces = nonbreakingSpace + 
		    nonbreakingSpace + nonbreakingSpace + 
		    nonbreakingSpace + nonbreakingSpace ;
	    StringBuilder sb = new StringBuilder("\r\n") ;
	    sb.append("Current Bill Date: ") ;
	    sb.append(predictor.getDateBillCurrent()) ;
	    sb.append("\r\n") ; 
	    sb.append("Current Bill Meter Reading: ") ;
	    h = new Integer(predictor.getMeterReadingBillCurrent()) ;
	    sb.append(h.intValue()) ;
	    if (usedMostRecentBillReadingCache) {
		sb.append(
			fiveNonBreakingSpaces +
			"<<<<<<<<<<<<  Cached Value Used  >>>>>>>>>>>>"
			) ;
	    }
	    sb.append("\r\n\r\n") ;
	    sb.append("Current ");
	    sb.append(fiveNonBreakingSpaces) ;
	    sb.append(nonbreakingSpace) ;
	    sb.append("Date: ");
	    sb.append(predictor.getDateCurrent().toString()) ;
	    if (changedTheDate) {
		sb.append(
			fiveNonBreakingSpaces +
				"<<<<<<<<<<<< "+
				"LATEST DATA AVAILABLE USED"+
				" >>>>>>>>>>>>"
				) ;
	    }
	    sb.append("\r\n") ;
	    sb.append(
		    "Current" +
			    fiveNonBreakingSpaces + 
			    nonbreakingSpace + 
			    nonbreakingSpace + 
			    "Meter Reading: "
		    ) ;
	    h = new Integer(predictor.getMeterReadingCurrent()) ;
	    sb.append(h.intValue()) ;
	    if (usedCurrentDateReadingCache) {
		sb.append(
			fiveNonBreakingSpaces +
			"<<<<<<<<<<<<  Cached Value Used  >>>>>>>>>>>>"
			) ;
	    }
	    sb.append("\r\n\r\n") ;
	    sb.append("Next ") ;
	    sb.append(fiveNonBreakingSpaces) ;
	    sb.append("Bill Date   : ") ;
	    sb.append(predictor.getDateBillNext().toString()) ;
	    sb.append(" (") ;
	    sb.append(Long.valueOf(predictor.
		    billingCycleDurationDays()).toString()) ;
	    sb.append(" day billing cycle).") ;
	    sb.append("\r\n") ;
	    sb.append("Days Remaining : ") ;
	    sb.append(Long.valueOf(predictor.daysRemaining()).toString()) ;
	    sb.append("\r\n") ;
	    gui.fb.progressAnnounce(false) ;
	    int predictedUsage = predictor.predictUsage() ;
	    String predictedUsageString ;
	    if (predictor.getDateBillCurrent().
		    equals(predictor.getDateCurrent())) 
	    {
		predictedUsageString = 
			"No prediction is possible " +
				"because Date of Most Recent Bill " + 
				"and Current Date are the same." ;
	    } else {
		predictedUsageString = String.valueOf(predictedUsage);
	    }
	    @SuppressWarnings("resource")
	    PrintStream where = 
	        ((predictedUsage>=500) && (predictedUsage<=1000))?
	        	System.out:System.err ;
	    //
	    //  Above does not require EDT.
	    //
	    //
	    //  Below outputs to gui so should be on EDT.
	    //
	    SwingUtilities.invokeLater(new Runnable() {
	        @Override
	        public void run() {
	            System.out.println(sb) ;
		    where.print("Predicted Usage : ") ;
		    where.println(predictedUsageString) ;
	        }
	    });
	}
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
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
    
    /**
     * @param settingsMap2
     * @param storedNameYear
     * @param storedNameMonth
     * @param storedNameDay
     * @param lD
     * @return
     */
    private static boolean canUseCachedValue(
	    final Map<? extends String, ? extends Setting> settingsMap2,
	    final String storedNameYear,
	    final String storedNameMonth,
	    final String storedNameDay,
	    final String storedNameValidity,
	    final LocalDate lD
	    ) {

	boolean valid ;
	String lDYear = String.valueOf(lD.getYear()) ;
	String lDMonth = String.valueOf(lD.getMonthValue()) ;
	String lDDay = String.valueOf(lD.getDayOfMonth()) ;
	
	if (storedNameValidity == null) {
	    valid = false ;
	} else {
	    valid = (
		    CommonPreferences.get(
			    settingsMap2.
			    get(storedNameValidity)
			    )
		    ).equals(Setting.VALID) ;
	}
	String storedYear =
		CommonPreferences.get(
			settingsMap2.
			get(storedNameYear)
			) ;
	String storedMonth = 
		CommonPreferences.get(
			settingsMap2.
			get(storedNameMonth)
			) ;
	String storedDay = 
		CommonPreferences.get(
			settingsMap2.
			get(storedNameDay)
			) ;

	return (
		valid &&
		storedYear.equals(lDYear) &&
		storedMonth.equals(lDMonth) &&
		storedDay.equals(lDDay)
		) ;
    }

    /**
     * @param settingsMap2
     * @param storedNameYear
     * @param storedNameMonth
     * @param storedNameDay
     * @param lD
     */
    private static void writeToCache(
	    final Map<? extends String, ? extends Setting> settingsMap2,
	    final String storedNameYear, 
	    final String storedNameMonth,
	    final String storedNameDay, 
	    final String storedNameReading,
	    final String storedNameValidity,
	    final LocalDate lD
	    ) {
	String lDYear = String.valueOf(lD.getYear()) ;
	String lDMonth = String.valueOf(lD.getMonthValue()) ;
	String lDDay = String.valueOf(lD.getDayOfMonth()) ;
	
	if (canUseCachedValue(
		settingsMap2, 
		storedNameYear, 
		storedNameMonth, 
		storedNameDay,
		storedNameValidity,
		lD
		)) 
	{
	    // Nothing to do when date matches
	} else {
	    // 
		CommonPreferences.set(
			settingsMap2.
			get(storedNameYear), 
			lDYear) ;
		CommonPreferences.set(
			settingsMap2.
			get(storedNameMonth), 
			lDMonth) ;
		CommonPreferences.set(
			settingsMap2.
			get(storedNameDay), 
			lDDay) ;
		if (storedNameReading != null) {
		    CommonPreferences.set(
			    settingsMap2.
			    get(storedNameReading),
			    "-1");
		}
		if (storedNameValidity != null) {
		    CommonPreferences.set(settingsMap2.get(storedNameValidity),
			    Setting.INVALID);
		}
	}
    }
    
    class DateLabelFormatter extends AbstractFormatter {
	private static final long serialVersionUID = 1L;
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
