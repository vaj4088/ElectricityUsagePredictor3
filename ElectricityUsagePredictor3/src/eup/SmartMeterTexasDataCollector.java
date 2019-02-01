package eup;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.xml.sax.InputSource;

import webPage.WPLocation;
import webPage.WebPage;

import java.awt.Container;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * A <tt>SmartMeterTexasDataCollector</tt> represents the actions needed to
 * access the
 * web pages containing the electrical meter data at smartmetertexas.com.
 * Access is via <tt>GET</tt> and <tt>POST</tt> methods accessed by using the
 * HTTP protocol. Most cookies are automatically handled by the
 * underlying Apache Commons HttpClient version 3.1 code.
 * <p>
 * 
 * @author Ian Shef
 * @version 1.0 25 Aug 2018
 * 
 * @since 1.0
 * 
 */

public class SmartMeterTexasDataCollector {
    /*
     * Some fields are volatile due to access from multiple threads.
     */
    private volatile LocalDate date ; // The date of this object.
    private volatile int startRead ;
    private volatile int endRead ;
    private volatile boolean dateChanged = false ;
    private volatile boolean dataValid = false ;

    private final Object lock = new Object() ;

    private static final String msgDown = "No results found.";
    private static final String msgNoResource = 
	    "The Access Manager WebSEAL server cannot find the resource " +
		    "you have requested." ;
    private static final String fromStringStartRead  = 
	    "<SPAN name=\"ViewDailyUsage_RowSet_Row_column7\">" ;
    private static final String toStringStartRead = 
	    "</SPAN></TD>" ;
    private static final String fromStringEndRead  = 
	    "<SPAN name=\"ViewDailyUsage_RowSet_Row_column8\">" ;
    private static final String toStringEndRead = 
	    "</SPAN></TD>" ;
    /*
     * To find Start of Day Meter Reading, use
     * 
     * <SPAN name="ViewDailyUsage_RowSet_Row_column7">
     * 
     */

    //
    // The following are:
    // volatile due to potential access from multiple threads, and
    // static so that values are maintained throughout the
    //        lifetime of this class.
    //
    static volatile LocalDate cachedDate ;
    static volatile long cachedMeterReading ;
    static volatile boolean cachedValuesValid = false ;
    static volatile boolean cachedValuesUsed  = false ;
    final Object cacheLock                    = new Object() ;
    //
    //
    //

    Feedbacker fb;

    // Maximum number of tries in case
    // of a redirect.
    private static final int tryRedirectMax = 10;

    HttpClient client; // Handles the work, holds context
    // (i.e. cookies).
    PostMethod method ;  //  The method (e.g. POST, GET) used for web access.

    String addressSuffix = "" ;

    // The following can be enabled to provide some debugging
    // information on System.out
    private boolean displayResponseBody = false;
    private boolean displayQueryOptions = false;
    private boolean displayHeadersAndFooters = false;
    private boolean displayCookies = false;
    private boolean displayPostParameters = false;
    private boolean displayWebPageExtractAddressFromGetData = false ;
    private boolean displayGetDataPage = false ;
    private boolean displayGetDataParameters = false ;
    private boolean displayUseProxy = false ;
    private boolean displayRedirectDetails = false ;
    
//    private int accessCount = 1 ;
    private static final int progressDelta = 11 ;
    private static int progress = progressDelta ;


    static final AtomicInteger ai = new AtomicInteger() ;

    private static final String SLASH = "/" ;

    /**
     * No publicly-available no-argument constructor.
     */
    @SuppressWarnings("unused")
    private SmartMeterTexasDataCollector() {
    }

    /**
     * The publicly-available constructor for getting
     * Smart Meter of Texas information
     * from my electrical meter.
     * 
     */
    public SmartMeterTexasDataCollector(LocalDate date) {
	this.date = date ;
	client = new HttpClient();
	if (displayUseProxy) {
	    useProxy(client);
	}
    }
    /**
     * Returns the body text of the web page as a <tt>WebPage</tt>, using the
     * html <tt>GET</tt> method.
     * 
     * @param url
     *            The web address to use, as a <tt>String</tt>.
     * 
     * @return A <tt>WebPage</tt> containing the lines of source text of the web
     *         page.
     */
    public WebPage getPage(String url) {
	WebPage wp ;
	// Create a method instance.
	GetMethod gm = new GetMethod(url);
	//
	// 3 Nov 2012 - Be lenient about cookies.
	//
	gm.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
	wp = requestResponse(gm);
	return wp;
    }

    /**
     * Returns the body text of the web page as a <tt>WebPage</tt>, using the
     * html <tt>POST</tt> method, conveniently turning each supplied Collection
     * into an array before calling the array-based version of this method.
     * 
     * @param url
     *            The web address to use, as a <tt>String</tt>.
     * 
     * @param postData
     *            A <tt>Collection</tt> containing zero or more NameValuePair to
     *            provide the data for the <tt>POST</tt> operation. If the
     *            results of the previous getPage(...) contained a form
     *            containing hidden input fields, their data is also provided
     *            for the <tt>POST</tt> operation.
     * 
     * @param removeData
     *            A <tt>Collection</tt> containing zero or more NameValuePair
     *            (as in postData), but their data is removed prior to the
     *            <tt>POST</tt> operation. Removal is after all postData and
     *            hidden input fields have been added. Removal is by matching
     *            names, the values are ignored. May be null.
     * 
     * @return A <tt>WebPage</tt> containing the lines of source text of the web
     *         page.
     */
    public WebPage getPage(String url, Collection<NameValuePair> postData,
	    Collection<NameValuePair> removeData) {
	NameValuePair[] nvpa = postData.toArray(new NameValuePair[0]);
	NameValuePair[] nvra = null;
	if (removeData != null) {
	    nvra = removeData.toArray(new NameValuePair[0]);
	}
	return getPage(url, nvpa, nvra);
    }

    /**
     * Returns the body text of the web page as a <tt>WebPage</tt>, using the
     * html <tt>POST</tt> method.
     * 
     * @param url
     *            The web address to use, as a <tt>String</tt>.
     * 
     * @param postData
     *            An array of NameValuePair to provide the data for the
     *            <tt>POST</tt> operation. If the results of the previous
     *            getPage(...) contained a form containing hidden input fields,
     *            their data is also provided for the <tt>POST</tt> operation.
     * 
     * @param removeData
     *            An array of NameValuePair (as in postData), but their data is
     *            removed prior to the <tt>POST</tt> operation. Removal is after
     *            all postData and hidden input fields have been added. Removal
     *            is by matching names, the values are ignored. May be null.
     * 
     * @return A <tt>WebPage</tt> containing the lines of source text of the web
     *         page.
     */
    public WebPage getPage(String url, NameValuePair[] postData,
	    NameValuePair[] removeData) {
	WebPage wp = null;
	method = new PostMethod(url);

	// This will remove any of the form's hidden input fields that
	// are being explicitly set.
	for (int i = 0; i < postData.length; i++) {
	    // Remove all parameters of this name, if any.
	    method.removeParameter(postData[i].getName());
	    // Now add the specifically requested parameter.
	    method.addParameter(postData[i]);
	}

	//
	// Now remove any parameters that have been requested for removal.
	//
	if (removeData != null) {
	    for (int i = 0; i < removeData.length; i++) {
		// Remove all parameters of this name, if any.
		method.removeParameter(removeData[i].getName());
	    }
	}

	if (displayPostParameters) {
	    NameValuePair[] params = method.getParameters();
	    msgEDT("" + params.length + " parameters:");
	    for (int i = 0; i < params.length; i++) {
		msgEDT(
			"    " + params[i].getName() + 
			"=" + params[i].getValue());
	    }
	}
	if (displayResponseBody) {
	    msgEDT(
		    "*******************************************    POST  START"
		    );
	}
	//
	// 3 Nov 2012 - Be lenient about cookies.
	//
	method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
	wp = requestResponse(method);
	if (displayResponseBody) {
	    msgEDT("*******************************************    POST  END");
	}
	return wp;
    }

    /**
     * Returns the body text of the web page as a <tt>WebPage</tt>, using the
     * method defined by its parameter. Any hidden input fields in the returned
     * page are collected for use by getPage if the next request is a
     * <tt>POST</tt>.
     * 
     * @param hmb
     *            A class inheriting from <tt>HttpMethodBase</tt> such as
     *            <tt>GetMethod</tt> or <tt>PostMethod</tt> that implements the
     *            necessary http method. Debug information will be sent to
     *            System.out if enabled.
     * 
     *            Exceptions (HttpException and IOException) are handled by
     *            displaying a message and a stack trace on System.err.
     * 
     * @return A <tt>WebPage</tt> containing the lines of source text of the web
     *         page.
     */
    private WebPage requestResponse(HttpMethodBase hmb) {
	int statusCode; // Result of HTTP request.
	WebPage wp = null; // Text of response will be saved in wp.

	// Provide custom retry handler if necessary
	hmb.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
		new DefaultHttpMethodRetryHandler(3, false));

	try {
	    // Ensure that status Code is initialized with an invalid value.
	    statusCode = -1;
	    for (int tryNum = 1; tryNum <= tryRedirectMax; tryNum++) {
		// Execute the method.
		statusCode = client.executeMethod(hmb);
		ElectricityUsagePredictor.
		  getFeedbacker().
		    progressAnnounce(progress, "Getting Data");
		progress += progressDelta ;
//		msgEDT(toString() + ", Access #" + accessCount++) ;
		if ((HttpStatus.SC_MOVED_PERMANENTLY == statusCode)
			|| (HttpStatus.SC_MOVED_TEMPORARILY == statusCode)
			|| (HttpStatus.SC_SEE_OTHER == statusCode)
			|| (HttpStatus.SC_TEMPORARY_REDIRECT == statusCode)) {
		    String redirectLocation;
		    Header locationHeader = hmb
			    .getResponseHeader("location");
		    if (locationHeader != null) {
			redirectLocation = locationHeader.getValue();
		    } else {
			// The response is invalid and did not provide the new
			// location for the resource. Report an error.
			msgEDT(toString() + 
				"#requestResponse failed attempt to "
				+ "executeMethod on "
				+ hmb.getClass().getName()
				+ ", status code " + statusCode + ": "
				+ hmb.getStatusLine()
				+ ", but missing new location.");
			break; // No point in continuing to loop.
		    } // end of if (locationHeader != null)

		    // Here if redirecting.
		    // Second parameter true indicates that the URI
		    // is already escaped.
		    hmb.setURI(new URI(redirectLocation, true));
		    if (displayRedirectDetails) {
			msgEDT(toString() + "#requestResponse redirecting for "
				+ hmb.getClass().getName() + ", status code "
				+ statusCode + ": " + hmb.getStatusLine()
				+ " to " + redirectLocation);
		    }
		    continue; // Loop back and retry.

		} // end of if (statusCode is <any of several values> )

		// Here if statusCode is not one of the handled redirect codes.
		// Thus, just continue on without looping.
		break;

	    } // end of for (int tryNum = 1 ; ...)

	    if (statusCode != HttpStatus.SC_OK) {
		msgEDT(toString()
			+ "#requestResponse failed attempt to executeMethod on "
			+ hmb.getClass().getName() + ", status code "
			+ statusCode + ": " + hmb.getStatusLine());
	    }

	    // Read the response body.
	    BufferedReader br = new BufferedReader(new InputStreamReader(
		    hmb.getResponseBodyAsStream()));
	    String rline;

	    // Deal with the response.
	    wp = new WebPage(); // An empty WebPage.
	    while ((rline = br.readLine()) != null) {
		wp.appendLine(rline);
		if (displayResponseBody)
		    msgEDT(rline);
	    }
	    // Use caution: ensure correct character encoding and
	    // is not binary data

	} catch (HttpException e) {
	    System.err.println("Fatal protocol violation: " + e.getMessage());
	    e.printStackTrace();
	} catch (IOException e) {
	    System.err.println("Fatal transport error: " + e.getMessage());
	    e.printStackTrace();
	} finally {
	    // Use "method" to provide various pieces
	    // of information (such as response headers).

	    if (displayQueryOptions || displayHeadersAndFooters
		    || displayCookies) {
		msgEDT("==========    OTHER INFORMATION    ==========");
	    }
	    if (displayQueryOptions) {
		msgEDT("Follow redirects = " + hmb.getFollowRedirects());
		msgEDT("Do authentication = " + hmb.getDoAuthentication());
		msgEDT("Path used = " + hmb.getPath());
		msgEDT("Query = " + hmb.getQueryString());
		StatusLine sl = hmb.getStatusLine();
		if (null != sl) {
		    msgEDT("Status = " + sl.toString());

		}
	    }
	    if (displayHeadersAndFooters) {
		Header[] h = hmb.getResponseHeaders();
		for (int i = 0; i < h.length; i++) {
		    msgEDT("Header " + i + ":  " + h[i].toString());
		}
		Header[] f = hmb.getResponseFooters();
		for (int i = 0; i < f.length; i++) {
		    msgEDT("Footer " + i + ":  " + f[i].toString());
		}
	    }
	    if (displayCookies) {
		Cookie[] c = client.getState().getCookies();
		for (int i = 0; i < c.length; i++) {
		    msgEDT("Cookie " + i + ":  " + c[i].toString());
		}
	    }
	    if (displayQueryOptions || displayHeadersAndFooters
		    || displayCookies) {
		msgEDT("==========    END   INFORMATION    ==========");
	    }

	    // Release the connection.
	    hmb.releaseConnection();
	}
	return wp ;
    }

    /**
     * A method that extracts the first form in a web page, and then extracts
     * some fields.
     * 
     * This method also deals with fields where the value is missing.
     * In such cases, an empty value is used.  Ian Shef IBS 15 Oct 2018.  This 
     * is the Smart Meter Texas version of getHiddenFieldsInFirstForm(...).  
     * Some of the constants have changed case (Upper Case versus Lower Case)
     * and the type of the returned value has changed.  
     * Ian Shef  IBS  17 Oct 2018
     * 
     * @param wp
     *            The WebPage from which to extract the fields.
     * @return ArrayList<NameValuePair> of the name and the value of each
     *         desired field found.
     */
    public static List<NameValuePair> 
      getSomeFieldsInFirstFormSMT(WebPage wpInput) {
	final String FORM = "<form " ;
	final String FORM_END = "</form>" ;
	final String INPUT = "<input ";
	final String NAME = " name='" ;
	final String VALUE = " value='" ;
	final String CLOSE = "'" ;
	final String SELECT = "<select " ;
	final String HIDDEN = "hidden" ;
	final String VIEWUSAGE = "name='viewusage_" ;
	int start = -1 ; // Assume no start of form is found.
	int end   = -1 ; // Assume no  end  of form is found.
	int current = 0 ;
	List<NameValuePair> firstFormSomeInputFields = Util.makeArrayList(10) ;

	WebPage wp = new WebPage() ;

	Iterator<String> it = wpInput.getLines().iterator() ;
	while (it.hasNext()) {
	    //
	    // Convert to lower case and convert 
	    // double quotes to single quotes.
	    //
	    String stringOrig = it.next() ;
	    String s = stringOrig.toLowerCase().replace("\"", "'") ;
	    wp.appendLine(s);
	    if (start == -1) {
		if (s.indexOf(FORM) >= 0) {
		    start = current ;
		}
	    } else if (end == -1) {
		if (s.indexOf(FORM_END) >= 0) {
		    end = current ;
		}
	    }
	    if ((start >= 0) && (end == -1)) { // Form started & has not ended.
		int inputStart = s.indexOf(INPUT) ;
		int hiddenStart = s.indexOf(HIDDEN, inputStart) ;
		int viewusageStart = s.indexOf(VIEWUSAGE, inputStart) ;
		if ((inputStart >= 0) &&
			((hiddenStart >= 0) || (viewusageStart >= 0))) {
		    int nameStart = s.indexOf(NAME, inputStart) + NAME.length();
		    int nameEnd   = s.indexOf(CLOSE, nameStart) ;
		    String name   = stringOrig.substring(nameStart, nameEnd) ;
		    String value  = "" ;  // Use this if 
		    // there is no value given.
		    int valueStart = s.indexOf(VALUE) ;
		    if (valueStart >= 0) {
			valueStart += VALUE.length() ;
			int valueEnd = s.indexOf(CLOSE, valueStart) ;
			value = stringOrig.substring(valueStart, valueEnd) ;
			try {
			    value = java.net.URLEncoder.encode(value, "UTF-8") ;
			} catch (UnsupportedEncodingException e) {
			    e.printStackTrace();
			    System.exit(-27);
			}
		    }
		    firstFormSomeInputFields.add(
			    new NameValuePair(name, value)) ;
		}
		int selStart = s.indexOf(SELECT) ;
		if (selStart >= 0) {
		    int nameStart = s.indexOf(NAME, selStart) + NAME.length() ;
		    int nameEnd   = s.indexOf(CLOSE, nameStart) ;
		    String name   = stringOrig.substring(nameStart, nameEnd) ;
		    String value  = "DAILY" ;  // Use this for the value.
		    firstFormSomeInputFields.add(
			    new NameValuePair(name, value)) ;
		}
	    }
	    current++ ;
	}
	return firstFormSomeInputFields;
    }

    public void setDisplayCookies(boolean displayCookies) {
	this.displayCookies = displayCookies;
    }

    /**
     * A setter method that sets the state of <tt>displayHeadersAndFooters</tt>.
     * When enabled (true), all response headers and footers are displayed on
     * System.out when a response is received.
     * 
     * @param displayHeadersAndFooters
     *            boolean (initial state is false).
     */
    public void setDisplayHeadersAndFooters(boolean displayHeadersAndFooters) {
	this.displayHeadersAndFooters = displayHeadersAndFooters;
    }

    /**
     * A setter method that sets the state of <tt>displayQueryOptions</tt>. When
     * enabled (true), all options used in the query are displayed on System.out
     * when a response is received.
     * 
     * @param displayQueryOptions
     *            boolean (initial state is false).
     */
    public void setDisplayQueryOptions(boolean displayQueryOptions) {
	this.displayQueryOptions = displayQueryOptions;
    }

    /**
     * A setter method that sets the state of <tt>displayResponseBody</tt>. When
     * enabled (true), body text is displayed on System.out when a response is
     * received.
     * 
     * @param displayResponseBody
     *            boolean (initial state is false).
     */
    public void setDisplayResponseBody(boolean displayResponseBody) {
	this.displayResponseBody = displayResponseBody;
    }


    public Feedbacker getFeedbacker() {
	return fb;
    }

    public void setFeedbacker(Feedbacker fb) {
	this.fb = fb;
    }


    static Feedbacker setupFeedbacker() {
	final ArrayList<Feedbacker> holder = Util.makeArrayList(1);
	try {
	    javax.swing.SwingUtilities.invokeAndWait((new Runnable() {
		@Override
		public void run() {
		    final FeedbackerImplementation fb1 = 
			    new FeedbackerImplementation();
		    JFrame frame = new JFrame(fb1.toString());
		    Container cp = frame.getContentPane();
		    cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
		    cp.add(fb1.getProgressBar());
		    cp.add(fb1.getOperationsLog());
		    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		    frame.pack();
		    frame.setVisible(true);
		    System.setOut(new PrintStream(new FeedbackerOutputStream(
			    fb1, "<font color=\"green\">")));
		    System.setErr(new PrintStream(new FeedbackerOutputStream(
			    fb1, "<font color=\"red\">")));
		    holder.add(fb1);
		} // end of run()
	    }));
	} catch (InterruptedException e) {
	    e.printStackTrace();
	} catch (InvocationTargetException e) {
	    e.printStackTrace();
	}
	return holder.get(0);
    }

    static String htmlToText(final String s) {
	final String HTML_Ampersand = "&amp;";
	final String Text_Ampersand = "&";
	final String HTML_Apostrophe = "&#039;";
	final String Text_Apostrophe = "'";
	String result = s.replace(HTML_Ampersand, Text_Ampersand);
	result = result.replace(HTML_Apostrophe, Text_Apostrophe);
	return result;
    }

    /**
     * A main program for testing purposes to develop and test web access.
     * 
     * @param args
     *            Required but currently unused.
     */
    public static void main(String[] args) {
	ElectricityUsagePredictor.main(null) ;
	//	System.exit(0);
    }

    static InputSource makeWebPageInputSource(WebPage wp) {
	List<String> lines = wp.getLines();
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	for (String line : lines) {
	    pw.println(line);
	}
	StringReader sr = new StringReader(sw.toString());
	pw.close();
	/*
	 * sw.close() omitted because it has no effect but must deal with
	 * IOException
	 */
	return new InputSource(sr);
    }

    static InputSource makeCharSequenceInputSource(CharSequence cs) {
	String s = cs.toString() ;
	return new InputSource(new StringReader(s)) ;
    }


    @Override
    public String toString() {
	return new String(getClass().getName());
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
     * A convenience method for displaying a line of text on System.out
     * using the Event Dispatch Thread.
     * 
     * @param ob
     *            An <tt>Object</tt> or a <tt>String</tt> to be displayed on
     *            System.out. If an <tt>Object</tt>, its toString() method will
     *            be called.
     */
    void msgEDT(Object ob) {
	SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		msg(ob) ;
	    }
	}) ;
    }

    @SuppressWarnings("boxing")
    private final void useProxy(HttpClient h) {
	int result = -1 ;
	/*
	 * 
NOTE: you must make sure you are NOT on the EDT when you call this code, 
as the get() will never return and the EDT will never be released to go 
execute the FutureTask... – Eric Lindauer Nov 20 '12 at 6:08
	 *
	 */
	Callable<Integer> c = new Callable<Integer>() {
	    @Override public Integer call() {
		return JOptionPane.showConfirmDialog(null,
			"Do you want to use the proxy?") ;
	    }
	} ;
	FutureTask<Integer> dialogTask = 
		new FutureTask<Integer>(c);
	if (SwingUtilities.isEventDispatchThread()) {
	    try {
		result = c.call() ;
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    try {
		SwingUtilities.invokeAndWait(dialogTask);
	    } catch (InvocationTargetException e1) {
		e1.printStackTrace();
	    } catch (InterruptedException e1) {
		e1.printStackTrace();
	    }
	    try {
		result = dialogTask.get().intValue() ;
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    } catch (ExecutionException e) {
		e.printStackTrace();
	    }
	}
	if (result == JOptionPane.YES_OPTION) {
	    HostConfiguration hostConfiguration = 
		    h.getHostConfiguration() ;
	    hostConfiguration.setProxy("localhost", 8080)  ;
	    h.setHostConfiguration(hostConfiguration) ;
	}
    }

    /*
     * javadocs for HttpClient are at
     * http://hc.apache.org/httpcomponents-client-ga/
     * httpclient/apidocs/index.html
     * 
     * (for some version of the files).
     */


    private String extractAddressFromLogin(WebPage wp) {
	/*
	 * May also get
	 * 
	 * Third-party server not responding.
	 */
	String startFrom1 = "value='Update Report'";
	String startFrom2 = "onclick=\"this.form.action = &quot;";
	String goTo = "&quot;;";
	if (displayWebPageExtractAddressFromGetData) {
	    StringBuilder sb = new StringBuilder() ;
	    for (String s : wp.getLines()) {
		sb.append(s) ;
		sb.append(System.lineSeparator()) ;
	    }
	    msg("Web Page is:") ;
	    msg(sb) ;
	}
	WPLocation wpl = wp.indexOf(startFrom1);
	assertGoodLocation(wpl);
	wpl = wp.indexOf(startFrom2, wpl.getLine()) ; // Get to the
	// correct line.
	assertGoodLocation(wpl);
	String s = wp.subString(wpl, startFrom2, goTo);
	int commentStart = s.indexOf('#') ;
	if (commentStart != -1) {
	    s = s.substring(0, commentStart - 1) ;
	}
	if (!s.startsWith(SLASH)) {
	    s = SLASH + s ;
	}
	//	    msg("New suffix from login: " + s + " .");
	return s;
    }

    private String extractAddressFromGetData(WebPage wp) {
	String startFrom1 = "<div id=\"banner_logout\">";
	String startFrom2 = "href='";
	String startFrom3 = "/";
	String goTo = "'";
	WPLocation wpl = wp.indexOf(startFrom1);
	assertGoodLocation(wpl);
	wpl = wp.indexOf(startFrom2, wpl.getLine());
	assertGoodLocation(wpl);
	wpl = wp.indexOf(startFrom3, wpl.getLine(), wpl.getColumn());
	assertGoodLocation(wpl);
	WPLocation wpl2 = wp.indexOf(goTo, wpl.getLine(), wpl.getColumn());
	assertGoodLocation(wpl2);
	String s = wp.subString(wpl, startFrom3, goTo);
	if (!s.startsWith(SLASH)) {
	    s = SLASH + s ;
	}
	return s;
    }

    private boolean badLocation(WPLocation wpl) {
	return (wpl.getColumn() < 0 && wpl.getLine() < 0);
    }

    private void assertGoodLocation(WPLocation wpl) {
	if (badLocation(wpl))
	    throw new AssertionError("Bad location.");
    }

    WebPage login() {
	WebPage wp = null ;
	

	//
	// <><><><><>  Get a web page  <><><><><><>
	//
	//
	//  <><><><><>  This web page may be UNNECESSARY.  <><><><><><>
	//
//	getPage("https://www.smartmetertexas.com:443/CAP/public/"); // 91

	List<NameValuePair> nameValuePairs = new ArrayList<>();
	nameValuePairs.add(new NameValuePair("pass_dup", ""));
	nameValuePairs.add(new NameValuePair("username", "VAJ4088"));
	nameValuePairs.add(new NameValuePair("password", "bri2bri"));
	nameValuePairs.add(new NameValuePair("buttonName", ""));
	nameValuePairs.add(new NameValuePair("login-form-type", "pwd"));
	while (wp == null) {
	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    //  <><><><><>  This web page is REQUIRED.  <><><><><><>
	    //
	    //  The returned web page may be null due to a software-caused
	    //  connection abort or due to a timeout.
	    //
	    //  Keep trying until we receive something not null.
	    //
	    wp = getPage("https://www.smartmetertexas.com:443/pkmslogin.form",
		    nameValuePairs, null); // 114 POST- sets some cookies and
	    // leads to 115 automatically.
	}
	addressSuffix = extractAddressFromLogin(wp);
	/*
	 * Need to add getting a web page so that some cookies are set.
	 * 
	 */
	//
	// <><><><><>  Get a web page  <><><><><><>
	//
	WebPage wpCache =
		getPage("https://www.smartmetertexas.com/texas/wps/myportal") ;
	getLatestEndMeterReadingAndUpdateCache(wpCache) ;
	/*
	 * This web page (above) should have 
	 * the latest end read meter reading.
	 * 
	 * Look for this stuff:
	 * (the date is January 7, 2019)
	 * 
           	<TD><SPAN name="ler_td_ler">Latest End of Day Read</SPAN></TD> 
           	<TD><SPAN name="ler_date">01/07/2019</SPAN></TD> 
           	<TD><SPAN name="ler_time">00:00:00</SPAN></TD> 
           	<TD><SPAN name="ler_read">28781.924</SPAN></TD> 
           	<TD><SPAN name="ler_usage"></SPAN></TD> 
	 * 
	 */
	return wp ;
    }

    void getLatestEndMeterReadingAndUpdateCache(WebPage wp) {
	WPLocation wpl =
		wp.indexOf("Latest End of Day Read") ;
	assertGoodLocation(wpl) ;
	int line = wpl.getLine() ;
	String endDate = wp.subString(
		line+1, 
		"<TD><SPAN name=\"ler_date\">", 
		"</SPAN></TD>"
		) ;
	String endValue = wp.subString(
		line+3, 
		"<TD><SPAN name=\"ler_read\">", 
		"</SPAN></TD>"
		) ;
	LocalDate startDate = getLatestStartDate(endDate) ;
	long startReading = getLatestStartRead(endValue) ;
	synchronized(cacheLock) {
	    cachedDate         = startDate ;
	    cachedMeterReading = startReading ;
	    cachedValuesValid = true ;
	}
    }

    private LocalDate getLatestStartDate(String dateIn) {
	final char FSLASH = '/' ;
	if ((dateIn.charAt(2) == FSLASH) && (dateIn.charAt(5) == FSLASH)) {
	    String yearString = dateIn.substring(6, 10) ;
	    String monthString = dateIn.substring(0, 2) ;
	    String dayString = dateIn.substring(3, 5) ;
	    int year  = Integer.parseInt(yearString) ;
	    int month = Integer.parseInt(monthString) ;
	    int day   = Integer.parseInt(dayString) ; 
	    return LocalDate.of(year, month, day).plusDays(1) ;
	}
	throw new AssertionError(
		"Bad date string of " +
		dateIn +
		" in getLatestStartDate."
		) ;
    }

    private long getLatestStartRead(String in) {
	return (long)Float.parseFloat(in) ;
    }

    void getData(WebPage webPage) {
	DateTimeFormatter dtf = 
		DateTimeFormatter.ofPattern("MM'/'dd'/'yyyy") ;
	String dateString ;
	/*
	 * Need to compare variable date of type LocalDate
	 * with variable cachedDate of type LocalDate,
	 * using cache lock cacheLock to synchronize.
	 * 
	 * If boolean variable cachedValuesValid is true
	 * and the comparison is equal, then
	 * get the cached meter reading from the long variable
	 * cachedMeterReading.
	 * 
	 */
	synchronized (cacheLock) {
	    cachedValuesUsed  = false ;
	    if (cachedValuesValid && 
		    (date.isEqual(cachedDate) || date.isAfter(cachedDate))
		    ) {
		cachedValuesUsed = true ;
		/*
		 * If we got here, then fake the end reading
		 * (making it the same as the start reading
		 *  because further data is unavailable)
		 * and get data for a prior day
		 * (because there are suffixes to be handled
		 *   so that logging out may be performed).
		 */
		synchronized (lock) {
		    startRead = (int) cachedMeterReading;
		    endRead = startRead ;
		    dataValid = true ;
		    dateString = date.minusDays(3).format(dtf) ;
		    if (date.isAfter(cachedDate)) {
			//
			//
			//  This next line is a MAJOR design decision
			//  to change the date of this object to the cached
			//  date despite this object having been created 
			//  with a different date.
			//
			//
			date = cachedDate ;
			//
			//
			//
			dateChanged = true ;
		    }
		}
	    } else {
		synchronized (lock) {
		    dateString = date.format(dtf) ;
		}
	    }
	}

	List<NameValuePair> nameValuePairs = new ArrayList<>();
	final String VIEWUSAGE = "viewUsage_" ; 	// The capital "U" is 
	// significant !

	ArrayList<NameValuePair> al = Util.makeArrayList(
		getSomeFieldsInFirstFormSMT(webPage)) ;
	ListIterator<NameValuePair> lit = al.listIterator() ;
	while (lit.hasNext()) {
	    NameValuePair nvp = lit.next() ;
	    String name = nvp.getName() ;
	    if (name.startsWith(VIEWUSAGE)) {
		nvp.setValue(dateString);
		lit.set(nvp) ;
	    }
	}
	nameValuePairs.addAll(al) ;
	String pageURL = "https://www.smartmetertexas.com" + addressSuffix ;
	//
	// Get the client's current state.
	//
	HttpState state = client.getState() ;
	//
	// Get the client's first cookie (cookie 0).
	//
	Cookie cookie = state.getCookies()[0] ;
	//
	// Get the parameters of the cookie so that 
	// we know what parameters to use.
	//
	String domain = cookie.getDomain() ;
	String path   = cookie.getPath() ;
	Date expires  = cookie.getExpiryDate() ;
	boolean secure= cookie.getSecure() ;
	//
	// Create the new cookie and add it to the collection of cookies.
	//
	Cookie newCookie = new Cookie(
		domain, 
		"IV_JCT", 
		"%2Ftexas",
		path,
		expires,
		secure
		) ;
	state.addCookie(newCookie) ; 
	//
	//  Update the client's state.
	//
	client.setState(state) ;
	method.addRequestHeader("Accept", 
		"text/html,application/xhtml+xml,"+
		"application/xml;q=0.9,*/*;q=0.8") ;
	method.addRequestHeader("Accept-Language", "en-US,en;q=0.5") ;
	method.addRequestHeader("Referer", 
		"https://www.smartmetertexas.com/texas/wps/myportal") ;
	method.addRequestHeader("Connection", "keep-alive") ;
	method.addRequestHeader("Upgrade-Insecure-Requests", "1") ;
	method.removeRequestHeader("Content-Length") ;
	//
	// Initialized to account for the subtraction 
	// that will be needed.
	//
	int contentLengthAccumulator = -1 ;
	for (NameValuePair nvp : nameValuePairs) {
	    contentLengthAccumulator += nvp.getName().length() ;
	    contentLengthAccumulator += nvp.getValue().length() ;
	    contentLengthAccumulator += 2 ;  // For '&' and ';'
	}
	if (contentLengthAccumulator == -1) {
	    contentLengthAccumulator = 0 ;  // No length to body.
	}
	method.addRequestHeader(
		"Content-Length", 
		Integer.toString(contentLengthAccumulator)) ;
	//
	// <><><><><>  Get a web page  <><><><><><>
	//
	WebPage wp = getPage(
		pageURL,nameValuePairs, null);
	//
	// Check that there really is data.
	//

	//
	// First, check that the data was properly accessed.
	//
	WPLocation resourceMissing = wp.indexOf(msgNoResource) ;
	if (!badLocation(resourceMissing)) {
	    StringBuilder sb = new StringBuilder("Resource is missing, "
		    + "fix the program and please try again later.") ;
	    if (null == fb) {
		System.out.println(sb);
	    } else {
		fb.log(sb,
			Feedbacker.TO_FILE + Feedbacker.TO_GUI + 
			Feedbacker.TO_OUT
			+ Feedbacker.FLUSH) ;
	    }
	    try {
		Thread.sleep(10000) ;
	    } catch (InterruptedException e) {
		e.printStackTrace();
		// Restore the interrupted status
		Thread.currentThread().interrupt();
	    }
	    System.exit(-26) ;
	}

	//
	// Second, check that the server is up.
	//
	WPLocation serverDown = wp.indexOf(msgDown);
	if (!badLocation(serverDown)) {
	    synchronized(lock) {
		dataValid = false ;
	    }
	    StringBuilder sb = new StringBuilder(
		    "No predicting is possible now, "
			    + "please try again later.") ;
	    if (null == fb) {
		System.out.println(sb);
	    } else {
		fb.log(sb,
			Feedbacker.TO_FILE + Feedbacker.TO_GUI + 
			Feedbacker.TO_OUT
			+ Feedbacker.FLUSH) ;
	    }
	    try {
		Thread.sleep(10000) ;
	    } catch (InterruptedException e) {
		e.printStackTrace();
		// Restore the interrupted status
		Thread.currentThread().interrupt();
	    }
	    System.exit(-27) ;
	} else {
	    /*
	     * NOW : GET THE DATA !!!
	     */
	    WPLocation wpData = wp.indexOf(fromStringStartRead) ;
	    if (displayGetDataPage) {
		if (badLocation(wpData)) {
		    StringBuilder sb = new StringBuilder() ;
		    for (String line : wp.getLines()) {
			sb.append(line) ;
		    }
		    System.out.println("=====     SHOW PAGE.     =====") ;
		    System.out.println(sb) ;
		    System.out.println("=====     SHOWED PAGE.     =====") ;
		}
	    }
	    if (displayGetDataParameters) {
		System.out.println(
			"=====     SHOW PARAMETERS.     =====") ;
		System.out.println("URL:") ;
		System.out.println(pageURL) ;
		System.out.println("NAMEVALUEPAIRS:") ;
		for (NameValuePair nvp : nameValuePairs) {
		    System.out.println(nvp) ;
		}
		System.out.println("REQUEST HEADERS2:") ;
		for (Header h : method.getRequestHeaders()) {
		    System.out.println(h) ;
		}
		System.out.println(
			"=====     SHOWED PARAMETERS.     =====") ;
	    }
	    assertGoodLocation(wpData) ;
	    String dataString = wp.subString(wpData, 
		    fromStringStartRead, 
		    toStringStartRead) ;
	    float startReadFloat = Float.parseFloat(dataString) ;

	    wpData = wp.indexOf(fromStringEndRead) ;
	    dataString = wp.subString(wpData, 
		    fromStringEndRead, 
		    toStringEndRead) ;
	    float endReadFloat = Float.parseFloat(dataString) ;
	    synchronized (cacheLock) {
		if (!cachedValuesUsed) {
		    synchronized (lock) {
			startRead = (int) startReadFloat;
			endRead = (int) endReadFloat;
			dataValid = true;
		    }
		}
	    }
	}
	/*
	 * NOW : GET THE NEW addressSuffix !!!
	 */
	addressSuffix = extractAddressFromGetData(wp);

    }

    void logout() {

	//
	// This clears some cookies but is otherwise UNNECESSARY.
	//
//	getPage("https://www.smartmetertexas.com:443" + addressSuffix);
	//
	// <><><><><>  This web page is REQUIRED.
	//
//	getPage("https://www.smartmetertexas.com:443/pkmslogout?"
//		+ "filename=SMTLogout.html&type=public&lang=en");
	//
	// <><><><><>  Get a web page  <><><><><><>
	//
	//
	//  <><><><><>  This web page may be UNNECESSARY.  <><><><><><>
	//
//	getPage("https://www.smartmetertexas.com:443/CAP/public");
    }

    public void invoke() {
	WebPage wp = login();
	getData(wp);
	logout();
    }

    /**
     * @return the date
     */
    public LocalDate getDate() {
	return date;
    }

    /**
     * @return the startRead
     */
    public int getStartRead() {
	int value ;
	boolean dv ;
	synchronized(lock) {
	    value = startRead ;
	    dv = dataValid ;
	}
	if (!dv) {
	    invoke() ;
	    value = startRead ;
	}
	return value;
    }

    /**
     * @return whether the data is valid
     */
    public boolean isDataValid() {
	boolean value ;
	synchronized(lock) {
	    value = dataValid ;
	}
	return value;
    }

    /**
     * @return the endRead
     */
    public int getEndRead() {
	int value ;
	boolean dv ;
	synchronized(lock) {
	    value = endRead ;
	    dv = dataValid ;
	}
	if (!dv) {
	    invoke() ;  
	    value = endRead ;
	}
	return value;
    }

    /**
     * @return the dateChanged
     */
    public boolean isDateChanged() {
	boolean dc ;
	synchronized (lock) {
	    dc = dateChanged ;
	}
	return dc ;
    }
}
