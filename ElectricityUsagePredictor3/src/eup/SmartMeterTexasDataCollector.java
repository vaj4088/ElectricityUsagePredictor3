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
 * HTTP protocol. Cookies are automatically handled by the underlying Apache
 * Commons HttpClient version 3.1 code.
 * <p>
 * 
 * @author Ian Shef
 * @version 1.0 25 Aug 2018
 * 
 * @since 1.0
 * 
 */

public class SmartMeterTexasDataCollector {

    Feedbacker fb;

    // Maximum number of tries in case
    // of a redirect.
    private static final int tryRedirectMax = 10;

    HttpClient client; // Handles the work, holds context
    // (i.e. cookies).
    PostMethod method ;  //  The method (e.g. POST, GET) used for web access.
//    ExecutorService executor = Executors.newSingleThreadExecutor() ;
    
    String addressSuffix = "" ;
    
    // The following can be enabled to provide some debugging
    // information on System.out
    private boolean displayResponseBody = false;
    private boolean displayQueryOptions = false;
    private boolean displayHeadersAndFooters = false;
    private boolean displayCookies = false;
    private boolean displayPostParameters = false;
    boolean displayWebPageExtractAddressFromGetData = false ;
    boolean displayGetDataPage = false ;
    boolean displayGetDataParameters = false ;
    

    static final AtomicInteger ai = new AtomicInteger() ;
    
    private static final String SLASH = "/" ;
	
    /**
     * No-argument constructor for getting Smart Meter of Texas  information
     * from my electrical meter.
     */
    public SmartMeterTexasDataCollector() {
//	msg("SmartMeterTexasDataCollector construction start.") ;
	client = new HttpClient();
//	msg("SmartMeterTexasDataCollector construction end.") ;
	useProxy(client) ;
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
	// Create a method instance.
//	PostMethod method = new PostMethod(url);
	method = new PostMethod(url);
	// Add parameters from the form's hidden input fields.
//	Iterator<Map.Entry<String, String>> it = hiddenInputFields.entrySet()
//		.iterator();
//	while (it.hasNext()) {
//	    Map.Entry<String, String> entry = it.next();
//	    method.addParameter(entry.getKey(), entry.getValue());
//	}

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
		    msgEDT(toString() + "#requestResponse redirecting for "
			    + hmb.getClass().getName() + ", status code "
			    + statusCode + ": " + hmb.getStatusLine()
			    + " to " + redirectLocation);
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
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    } else {
		try {
		    SwingUtilities.invokeAndWait(dialogTask);
		} catch (InvocationTargetException e1) {
		    // TODO Auto-generated catch block
		    e1.printStackTrace();
		} catch (InterruptedException e1) {
		    // TODO Auto-generated catch block
		    e1.printStackTrace();
		}
		try {
		    result = dialogTask.get().intValue() ;
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (ExecutionException e) {
		    // TODO Auto-generated catch block
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

    class GetData {
	/*
	 * Some fields are volatile due to access from multiple threads.
	 */
	private volatile LocalDate date ; 
	private volatile int startRead ;
	private volatile int endRead ;
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

	@SuppressWarnings("unused")
	private GetData() {
	} // No available no-argument constructor.

	public GetData(LocalDate date) {
//		msg("GetData construction start.") ;
	    this.date = date;
//		msg("GetData construction end.") ;
	}

	private String extractAddressFromLogin(WebPage wp) {
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
	    msg("New suffix from login: " + s + " .");
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
	    msg("New suffix from Get Data: " + s + " .");
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
	    List<NameValuePair> nameValuePairs = new ArrayList<>();

	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    //  <><><><><>  This web page is for debugging.  <><><><><><>
	    //
	    getPage("https://www.ubuntu.com/debug/login_start");

	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    //  <><><><><>  This web page may be UNNECESSARY.  <><><><><><>
	    //
	    getPage("https://www.smartmetertexas.com:443/CAP/public/"); // 91

	    nameValuePairs.add(new NameValuePair("pass_dup", ""));
	    nameValuePairs.add(new NameValuePair("username", "VAJ4088"));
	    nameValuePairs.add(new NameValuePair("password", "bri2bri"));
	    nameValuePairs.add(new NameValuePair("buttonName", ""));
	    nameValuePairs.add(new NameValuePair("login-form-type", "pwd"));
	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    //  <><><><><>  This web page is REQUIRED.  <><><><><><>
	    //
	    WebPage wp = getPage(
		    "https://www.smartmetertexas.com:443/pkmslogin.form",
		    nameValuePairs, null); // 114 POST- sets some cookies and
					   // leads to 115 automatically.
	    /*
	     * from 00 - WebScarab 20180808 myexpressenergy_com Login
	     * 
	     * Uses these messages: 91 GET - may not be needed, but sets some
	     * cookies. 114 POST- sets some cookies and leads to 115
	     * automatically. Page data is
	     * pass_dup=&username=VAJ4088&password=bri2bri&buttonName=&login-
	     * form-type=pwd Response is 302 Moved Temporarily 115 GET - sets
	     * some cookies and probably leads to 116 automatically. Response is
	     * 302 Found 116 GET - sets some cookies.
	     */
	    addressSuffix = extractAddressFromLogin(wp);
	    /*
	     * Need to add getting a web page so that some cookies are set.
	     * 
	     */
	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    getPage("https://www.smartmetertexas.com/texas/wps/myportal") ;
	    /*
	     * 
	     */

	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    //  <><><><><>  This web page is for debugging.  <><><><><><>
	    //
	    getPage("https://www.ubuntu.com/debug/login_end");
	    
	    return wp ;
	}

	void getData(WebPage webPage) {
	    List<NameValuePair> nameValuePairs = new ArrayList<>();
	    final String VIEWUSAGE = "viewUsage_" ; 	// The capital "U" is 
	    						// significant !

	    /*
	     * from 00 - WebScarab 20180808 myexpressenergy_com Get Data
	     * 
	     * Uses these messages: 164 POST- address from ? Response contains
	     * some data. Page data is
	     * _bowStEvent=Usage%2Fportlet%2FUsageCustomerMetersPortlet%
	     * 21fireEvent%
	     * 3AForm%3AViewUsagePage_SaveDataSubmitEvent&tag_UserLocale=en&
	     * reportType=DAILY&viewUsage_startDate=08%2F06%2F2018&
	     * viewUsage_endDate =08%2F06%2F2018&
	     * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e
	     * =&
	     * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e1
	     * =&
	     * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e2
	     * =&
	     * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e2=
	     * 170 POST Response contains the data! Page Data is
	     * _bowStEvent=Usage%2Fportlet%2FUsageCustomerMetersPortlet%
	     * 21fireEvent%
	     * 3AForm%3AViewUsagePage_SaveDataSubmitEvent&tag_UserLocale=en&
	     * reportType=DAILY&viewUsage_startDate=08%2F01%2F2018&
	     * viewUsage_endDate
	     * =08%2F03%2F2018&viewusage_but_updaterpt=Update+Report&
	     * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e
	     * =&
	     * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e1
	     * =&
	     * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e2
	     * =&
	     * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e2=
	     * 
	     */
/*
 * _bowStEvent=Usage%2Fportlet%2FUsageCustomerMetersPortlet%21fireEvent%3AForm%3AViewUsagePage_SaveDataSubmitEvent&tag_UserLocale=en&reportType=INTERVAL&viewUsage_startDate=10%2F12%2F2018&viewUsage_endDate=10%2F12%2F2018&viewusage_but_updaterpt=Update+Report&_bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515NRCAdmin_00515Default_00515Default_00515Default_00515Esiid_0051516674c265d7_00515bf4cc=&_bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515NRCAdmin_00515Default_00515Default_00515Default_00515Esiid_0051516674c265d7_00515bf4cc1=&_bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515NRCAdmin_00515Default_00515Default_00515Default_00515Esiid_0051516674c265d7_00515bf4cc2=&_bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515NRCAdmin_00515Default_00515Default_00515Default_00515Esiid_0051516674c265d7_00515bf4cc2=
 */

	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    //  <><><><><>  This web page is for debugging.  <><><><><><>
	    //
	    getPage("https://www.ubuntu.com/debug/get_Data_Start");

	    //
	    // Preparing to do POST 164
	    //
//	    nameValuePairs.add(new NameValuePair("reportType", "DAILY"));
	    DateTimeFormatter dtf = 
//		    DateTimeFormatter.ofPattern("MM'%2F'dd'%2F'yyyy") ;
		    DateTimeFormatter.ofPattern("MM'/'dd'/'yyyy") ;
	    String dateString = date.format(dtf) ;
//	    nameValuePairs.add(
//		    new NameValuePair("viewUsage_startDate", dateString)) ; // <<<<<<<<<<<< Class DateTimeFormatter "MM'%2F'dd'%2F'yyyy"
//	    nameValuePairs.add(
//		    new NameValuePair("viewUsage_endDate"  , dateString)) ; // <<<<<<<<<<<< Class DateTimeFormatter "MM'%2F'dd'%2F'yyyy"

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
	    System.out.println("<br>===========================") ;
	    System.out.println("Attempting to get data from") ;
	    System.out.println(pageURL) ;
	    System.out.println("using the following parameters") ;
	    Iterator<NameValuePair> it = nameValuePairs.iterator() ;
	    while (it.hasNext()) {
		msg(it.next());
	    }
	    System.out.println("<br>===========================") ;
	    System.out.println("<br>===========================") ;
	    System.out.println("Adding the cookie:") ;
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
	    System.out.println(newCookie) ;
	    System.out.println("<br>===========================") ;

	    System.out.println("<br>===========================") ;
	    System.out.println("<br>===========================") ;
	    System.out.println(
		    "Adding the request headers (and fixing Content-Length):") ;
	    method.addRequestHeader("Accept", 
		    "text/html,application/xhtml+xml,"+
		    "application/xml;q=0.9,*/*;q=0.8") ;
	    method.addRequestHeader("Accept-Language", "en-US,en;q=0.5") ;
	    method.addRequestHeader("Referer", 
		    "https://www.smartmetertexas.com/texas/wps/myportal") ;
	    method.addRequestHeader("Connection", "keep-alive") ;
	    method.addRequestHeader("Upgrade-Insecure-Requests", "1") ;
	    method.removeRequestHeader("Content-Length") ;
	    System.out.println("     ATTEMPTED to remove Content-Length.") ;
	    //
	    // Get the content length.
	    //
	    
	    System.out.println("     Accumulating new Content-Length.") ;
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
	    System.out.println("     Added new Content-Length.") ;
	    System.out.println("REQUEST HEADERS1") ;
	    for (Header header : method.getRequestHeaders()) {
		System.out.println(header) ;
	    }
	    System.out.println("     Accumulated new Content-Length of ") ;
	    System.out.println(Integer.toString(contentLengthAccumulator)) ;
	    System.out.println(".     ") ;
	    System.out.println("<br>===========================") ;
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
		    Thread.sleep(5000) ;
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
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
		    Thread.sleep(5000) ;
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		    // Restore the interrupted status
		    Thread.currentThread().interrupt();
		}
		System.exit(-27) ;
	    } else {
		/*
		 * NOW : GET THE DATA !!!
		 */
		/*
		 * Look for
		 * 
		 * <SPAN
		 * name="ViewDailyUsage_RowSet_Row_column7">25407.133</SPAN></
		 * TD>
		 * 
		 * where
		 * 
		 * <SPAN name="ViewDailyUsage_RowSet_Row_column7">
		 * 
		 * precedes the data, and
		 * 
		 * </SPAN></TD>
		 * 
		 * follows the data, and the data
		 * 
		 * 25407.133
		 * 
		 * is truncated to 25407
		 * 
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
			System.out.println("=====     SHOW PARAMETERS.     =====") ;
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
			System.out.println("=====     SHOWED PARAMETERS.     =====") ;
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
		synchronized(lock) {
		    startRead = (int) startReadFloat ;
		    endRead   = (int) endReadFloat ;
		    dataValid = true ;
		}
	    }
	    /*
	     * NOW : GET THE NEW addressSuffix !!!
	     */
	    addressSuffix = extractAddressFromGetData(wp);

	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    //  <><><><><>  This web page is for debugging.  <><><><><><>
	    //
	    getPage("https://www.ubuntu.com/debug/get_Data_End");

	}
	
	void logout() {

	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    //  <><><><><>  This web page is for debugging.  <><><><><><>
	    //
	    getPage("https://www.ubuntu.com/debug/logout_start");

	    //
	    // Conversation 173 GET - sets some cookies.
	    //
	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    // This clears some cookies but is otherwise UNNECESSARY.
	    //
	    getPage("https://www.smartmetertexas.com:443" + addressSuffix);
	    //
	    // Conversation 174 GET - sets some cookies.
	    //
	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    // <><><><><>  This web page is REQUIRED.
	    //
	    getPage("https://www.smartmetertexas.com:443/pkmslogout?"
		    + "filename=SMTLogout.html&type=public&lang=en");
	    //
	    // Conversation 175 GET - sets some cookies.
	    //
	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    //  <><><><><>  This web page may be UNNECESSARY.  <><><><><><>
	    //
	    getPage("https://www.smartmetertexas.com:443/CAP/public");
	    // Response is
	    // 301 Moved Permanently, which automatically causes 176.

	    //
	    // <><><><><>  Get a web page  <><><><><><>
	    //
	    //
	    //  <><><><><>  This web page is for debugging.  <><><><><><>
	    //
	    getPage("https://www.ubuntu.com/debug/logout_end");

	}

	public void invoke() {
	    WebPage wp = login();
	    getData(wp);
	    logout();
	}
	
/*
 * 
	public void invoke() {
//	    msg("GetData run about to login() #" + Integer.toString(ai.getAndIncrement()) + ".") ;
	    Future<WebPage> fLogin = executor.submit(new Callable<WebPage>() {
		@SuppressWarnings("unused")
		@Override
		public WebPage call() throws Exception {
		    msg("GetData run about to login() #" + 
			    Integer.toString(ai.getAndIncrement()) + ".") ;
		    return login() ;
		}
	    } ) ;
//	    WebPage wp = login();
//	    msg("GetData run about to getData() #" + Integer.toString(ai.getAndIncrement()) + ".") ;
	    Future<Integer> fGetData = executor.submit(new Callable<Integer>() {
		@Override
		public Integer call() throws Exception {
		    msg("GetData run about to getData() #" + 
			    Integer.toString(ai.getAndIncrement()) + ".") ;
		    WebPage wp = fLogin.get();  //  Ensure sequencing.
		    getData(wp) ;
		    return Integer.valueOf(1) ;
		}
	    } ) ;
//	    getData(wp);
//	    msg("GetData run about to logout() #" + Integer.toString(ai.getAndIncrement()) + ".") ;
	    Future<Integer> fLogout = executor.submit(new Callable<Integer>() {
		@SuppressWarnings("unused")
		@Override
		public Integer call() throws Exception {
		    //
		    // The following line removes an IDE warning.
		    //
		    Integer throwaway = fGetData.get() ;  //  Ensure sequencing.
		    msg("GetData run about to logout() #" + 
			    Integer.toString(ai.getAndIncrement()) + ".") ;
		    logout() ;
		    return Integer.valueOf(1) ;
		}
	    } ) ;
//	    logout();
	    fLogout.isDone() ;  //  Removes an IDE warning.
	}
	*
	*/

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
		msg("getStartRead about to start().") ;
		invoke() ;
		msg("getStartRead after start().") ;
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
    }
}
