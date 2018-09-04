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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JFrame;

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

    private HttpClient client; // Handles the work, holds context
    // (i.e. cookies).
    String addressSuffix = "" ;
    
    // The following can be enabled to provide some debugging
    // information on System.out
    private boolean displayResponseBody = true;
    private boolean displayQueryOptions = false;
    private boolean displayHeadersAndFooters = false;
    private boolean displayCookies = false;
    private boolean displayPostParameters = false;

    static final AtomicInteger ai = new AtomicInteger() ;
	
    /**
     * No-argument constructor for getting Smart Meter of Texas  information
     * from my electrical meter.
     */
    public SmartMeterTexasDataCollector() {
//	msg("SmartMeterTexasDataCollector construction start.") ;
	client = new HttpClient();
//	msg("SmartMeterTexasDataCollector construction end.") ;
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
	WebPage wp = null;
	// Create a method instance.
	GetMethod method = new GetMethod(url);
	//
	// 3 Nov 2012 - Be lenient about cookies. NAU cookies violate
	// RFC 2109 and cause a warning.
	//
	method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
	wp = requestResponse(method);
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
	PostMethod method = new PostMethod(url);
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
     * @param method
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
    private WebPage requestResponse(HttpMethodBase method) {
	int statusCode; // Result of HTTP request.
	WebPage wp = null; // Text of response will be saved in wp.

	// Provide custom retry handler if necessary
	method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
		new DefaultHttpMethodRetryHandler(3, false));

	try {
	    // Ensure that status Code is initialized with an invalid value.
	    statusCode = -1;
	    for (int tryNum = 1; tryNum <= tryRedirectMax; tryNum++) {
		// Execute the method.
		statusCode = client.executeMethod(method);
		if ((HttpStatus.SC_MOVED_PERMANENTLY == statusCode)
			|| (HttpStatus.SC_MOVED_TEMPORARILY == statusCode)
			|| (HttpStatus.SC_SEE_OTHER == statusCode)
			|| (HttpStatus.SC_TEMPORARY_REDIRECT == statusCode)) {
		    String redirectLocation;
		    Header locationHeader = method
			    .getResponseHeader("location");
		    if (locationHeader != null) {
			redirectLocation = locationHeader.getValue();
		    } else {
			// The response is invalid and did not provide the new
			// location for the resource. Report an error.
			msgEDT(toString() + 
				"#requestResponse failed attempt to "
				+ "executeMethod on "
				+ method.getClass().getName()
				+ ", status code " + statusCode + ": "
				+ method.getStatusLine()
				+ ", but missing new location.");
			break; // No point in continuing to loop.
		    } // end of if (locationHeader != null)

		    // Here if redirecting.
		    // Second parameter true indicates that the URI
		    // is already escaped.
		    method.setURI(new URI(redirectLocation, true));
		    msgEDT(toString() + "#requestResponse redirecting for "
			    + method.getClass().getName() + ", status code "
			    + statusCode + ": " + method.getStatusLine()
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
			+ method.getClass().getName() + ", status code "
			+ statusCode + ": " + method.getStatusLine());
	    }

	    // Read the response body.
	    BufferedReader br = new BufferedReader(new InputStreamReader(
		    method.getResponseBodyAsStream()));
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
	    //
	    // Should attempt to catch:
	    //
	    // Fatal transport error: Connection timed out
	    // java.net.ConnectException: Connection timed out
	    //
	} catch (IOException e) {
	    // This happens fairly frequently with e.getMessage()
	    // returning "Connection reset" and the stack trace begins
	    // with
	    // "java.net.SocketException: Connection reset"
	    //
	    // May need to figure out how to recover automatically.
	    //
	    // Also can get e.getMessage()
	    // returning "Connection timed out" and the stack trace begins
	    // with
	    // "java.net.ConnectException: Connection timed out"
	    //
	    //
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
		msgEDT("Follow redirects = " + method.getFollowRedirects());
		msgEDT("Do authentication = " + method.getDoAuthentication());
		msgEDT("Path used = " + method.getPath());
		msgEDT("Query = " + method.getQueryString());
		StatusLine sl = method.getStatusLine();
		if (null != sl) {
		    msgEDT("Status = " + sl.toString());

		}
	    }
	    if (displayHeadersAndFooters) {
		Header[] h = method.getResponseHeaders();
		for (int i = 0; i < h.length; i++) {
		    msgEDT("Header " + i + ":  " + h[i].toString());
		}
		Header[] f = method.getResponseFooters();
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
	    method.releaseConnection();
	}
//		} else {
//		    throw new UnacceptableFormsException(e.getMessage(),
//			    e.getCause());
//		}
//	    }
//	}
	return wp;
    }

    /**
     * A method that extracts the first form in a web page, and then extracts
     * the hidden fields within. This can be helpful when a web page contains
     * more than one form, because WebPage.getHiddenFields cannot deal with
     * multiple forms.
     * 
     * @param wp
     *            The WebPage from which to extract the hidden fields.
     * @return Map<String, String> of the name and the value of each hidden
     *         field found.
     */
    public static Map<String, String> getHiddenFieldsInFirstForm(WebPage wp) {
	final String FORM = "<form ";
	final String FORM_END = "</form>";
	final String INPUT = "<input ";
	final String HIDDEN1 = " type='hidden' ";
	final String HIDDEN2 = " type=\"hidden\" ";
	final String NAME1 = " name='";
	final String NAME2 = " name=\"";
	final String VALUE1 = " value='";
	final String VALUE2 = " value=\"";
	final String CLOSE1 = "'";
	final String CLOSE2 = "\"";
	int start = -1; // Assume no start of form is found.
	int end = 0;
	int current = 0;
	Map<String, String> firstFormHiddenInputFields;

	// Locate the start.
	while (current >= 0) {
	    WPLocation loc = wp.indexOf(FORM, current);
	    current = loc.getLine();
	    if (current < 0)
		break; // None found.
	    int startChar = loc.getColumn();
	    int docWriteLoc = wp.line(current).indexOf("document.write(\"");
	    int docWrite2Loc = wp.line(current).indexOf("iframeDoc.write(\"");
	    if ((current >= 0)
		    && ((docWriteLoc < 0) || (docWriteLoc > startChar))
		    && ((docWrite2Loc < 0) || (docWrite2Loc > startChar))) {
		start = current; // Found acceptable start of form.
		// Record its line number, and
		// set up to
		// continue processing.
		current = -1; // Indicate no further searching for start
		// of form.
	    } else {
		current++; // Not acceptable start of form, so
		// set up to continue the search.
	    }
	}

	// Locate the end.
	current = start;
	while (current >= 0) {
	    current = wp.indexOf(FORM_END, current).getLine();
	    if (current >= 0) {
		if ((wp.line(current).indexOf("document.write(\"") < 0)
			&& (wp.line(current).indexOf(
				"iframeDoc.write(\"") < 0)) {
		    end = current;
		    current = -1; // Indicate no further searching for
		    // end of form.
		} else {
		    current++;
		}
	    }
	}

	// Get the hidden input fields.
	firstFormHiddenInputFields = Util.makeLinkedHashMap();
	current = start;
	while ((current >= 0) && (current <= end)) {
	    WPLocation loc = wp.indexOf(INPUT, current);
	    current = loc.getLine();
	    // If the input field was found beyond end or not found, then quit.
	    if ((current > end) || (current < 0))
		break;

	    String text = wp.substring(loc);
	    // If the input field is not a hidden field, continue to the next
	    // one.
	    if (text.contains(HIDDEN1)) {
		// Here if we have a hidden input field within the form.
		// Get the name and the corresponding value.
		String name;
		String value;
		name = wp.subString(loc, NAME1, CLOSE1);
		value = wp.subString(loc, VALUE1, CLOSE1);
		firstFormHiddenInputFields.put(name, value);
	    } else if (text.contains(HIDDEN2)) {
		// Here if we have a hidden input field within the form.
		// Get the name and the corresponding value.
		String name;
		String value;
		name = wp.subString(loc, NAME2, CLOSE2);
		value = wp.subString(loc, VALUE2, CLOSE2);
		firstFormHiddenInputFields.put(name, value);
	    }
	    current++;
	}
	// System.out.println(Util.getCallerMethodName() +
	// " found " + firstFormHiddenInputFields) ; // debug
	return firstFormHiddenInputFields;
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

//	getFeedbacker().log(
//		"PROGRAM DONE.",
//		Feedbacker.FLUSH + Feedbacker.TO_FILE + Feedbacker.TO_GUI
//		+ Feedbacker.TO_OUT);
	System.exit(0);
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
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		msg(ob) ;
	    }
	    
	});
    }
    
    /*
     * javadocs for HttpClient are at
     * http://hc.apache.org/httpcomponents-client-ga/
     * httpclient/apidocs/index.html
     * 
     * (for some version of the files).
     */

    class GetData extends Thread {
	/*
	 * 
	 * To use this class, do:
	 * 
	 * (new GetData()).start() ;
	 * 
	 * may need to do: (SmartMeterTexasDataCollector.new GetData()).start()
	 * ;
	 * 
	 * except that the GetData constructor needs a parameter.
	 * 
	 */

	/*
	 * Some fields are volatile due to access from multiple threads.
	 */
	private volatile LocalDate date ; 
	private volatile int startRead ;
	private volatile int endRead ;
	private volatile boolean dataValid = false ;
	
	private final Object lock = new Object() ;
	
	private static final String msgDown = "No results found.";
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
	    String startFrom1 = "_f.action = &quot;/";
	    String startFrom2 = "/";
	    String goTo = "#";
	    WPLocation wpl = wp.indexOf(startFrom1);
	    assertGoodLocation(wpl);
	    wpl = wp.indexOf(startFrom2, wpl.getLine(), wpl.getColumn());
	    assertGoodLocation(wpl);
	    wpl = wp.indexOf(startFrom2, wpl.getLine(), wpl.getColumn());
	    assertGoodLocation(wpl);
	    WPLocation wpl2 = wp.indexOf(goTo, wpl.getLine(), wpl.getColumn());
	    assertGoodLocation(wpl2);
	    String s = wp.subString(wpl, startFrom2, goTo);
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

	private void login() {
	    List<NameValuePair> nameValuePairs = new ArrayList<>();

	    getPage("https://www.smartmetertexas.com:443/CAP/public/"); // 91

	    nameValuePairs.add(new NameValuePair("pass_dup", ""));
	    nameValuePairs.add(new NameValuePair("username", "VAJ4088"));
	    nameValuePairs.add(new NameValuePair("password", "bri2bri"));
	    nameValuePairs.add(new NameValuePair("buttonName", ""));
	    nameValuePairs.add(new NameValuePair("login-form-type", "pwd"));
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
	}

	private void getData() {
	    List<NameValuePair> nameValuePairs = new ArrayList<>();

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

	    //
	    // Preparing to do POST 164
	    //
	    nameValuePairs.add(new NameValuePair("_bowStEvent",
		    "Usage%2Fportlet%2FUsageCustomerMetersPortlet%21fireEvent"
			    + "%3AForm%3AViewUsagePage_SaveDataSubmitEvent"));
	    nameValuePairs.add(new NameValuePair("tag_UserLocale", "en"));
	    nameValuePairs.add(new NameValuePair("reportType", "DAILY"));
	    DateTimeFormatter dtf = 
		    DateTimeFormatter.ofPattern("MM'%2F'dd'%2F'yyyy") ;
	    String dateString = date.format(dtf) ;
//	    nameValuePairs.add(
//		    new NameValuePair("viewUsage_startDate", "08%2F01%2F2018")); // <<<<<<<<<<<< Class DateTimeFormatter "MM'%2F'dd'%2F'yyyy"
//	    nameValuePairs.add(
//		    new NameValuePair("viewUsage_endDate", "08%2F03%2F2018")) ;  // <<<<<<<<<<<< Class DateTimeFormatter "MM'%2F'dd'%2F'yyyy"
	    nameValuePairs.add(
		    new NameValuePair("viewUsage_startDate", dateString)) ; // <<<<<<<<<<<< Class DateTimeFormatter "MM'%2F'dd'%2F'yyyy"
	    nameValuePairs.add(
		    new NameValuePair("viewUsage_endDate"  , dateString)) ; // <<<<<<<<<<<< Class DateTimeFormatter "MM'%2F'dd'%2F'yyyy"
	    nameValuePairs
		    .add(new NameValuePair("_bst_locator_Usage_00215portlet"
			    + "_00215UsageCustomerMetersPortlet"
			    + "_00515ResidentialC"
			    + "_00515Default_00515Default" + "_00515Default"
			    + "_00515Esiid_005151651b3535a4_00515b6e7e", ""));
	    nameValuePairs.add(new NameValuePair(
		    "_bst_locator_Usage_00215portlet"
			    + "_00215UsageCustomerMetersPortlet"
			    + "_00515ResidentialC"
			    + "_00515Default_00515Default_00515Default"
			    + "_00515Esiid" + "_005151651b3535a4_00515b6e7e1",
		    ""));
	    nameValuePairs.add(new NameValuePair(
		    "_bst_locator_Usage_00215portlet"
			    + "_00215UsageCustomerMetersPortlet"
			    + "_00515ResidentialC"
			    + "_00515Default_00515Default_00515Default"
			    + "_00515Esiid" + "_005151651b3535a4_00515b6e7e2",
		    ""));
	    WebPage wp = getPage(
		    "https://www.smartmetertexas.com:443" + addressSuffix,
		    nameValuePairs, null);
	    //
	    // Check that there really is data.
	    //
	    WPLocation serverDown = wp.indexOf(msgDown);
	    if (!badLocation(serverDown)) {
		synchronized(lock) {
		    dataValid = false ;
		}
		fb.log("No predicting is possible now, "
			+ "please try again later.",
			Feedbacker.TO_FILE + Feedbacker.TO_GUI
			+ Feedbacker.FLUSH);
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
		 * preceeds the data, and
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
	}

	private void logout() {
	    //
	    // Conversation 173 GET - sets some cookies.
	    //
	    getPage("https://www.smartmetertexas.com:443" + addressSuffix);
	    //
	    // Conversation 174 GET - sets some cookies.
	    //
	    getPage("https://www.smartmetertexas.com:443/pkmslogout?"
		    + "filename=SMTLogout.html&type=public&lang=en");
	    //
	    // Conversation 175 GET - sets some cookies.
	    //
	    getPage("https://www.smartmetertexas.com:443/CAP/public");
	    // Response is
	    // 301 Moved Permanently, which automatically causes 176.
	}

	@Override
	public void run() {
	    msg("GetData run about to login() #" + Integer.toString(ai.getAndIncrement()) + ".") ;
	    login();
	    msg("GetData run about to getData() #" + Integer.toString(ai.getAndIncrement()) + ".") ;
	    getData();
	    msg("GetData run about to logout() #" + Integer.toString(ai.getAndIncrement()) + ".") ;
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
		msg("getStartRead about to start().") ;
		start() ;  //  Causes the run method to execute on a new Thread.
		msg("getStartRead after start().") ;
		value = startRead ;
	    }
	    return value;
	}

	/**
	 * @return the startReadValid
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
		start() ;  //  Causes the run method to execute on a new Thread.
		value = endRead ;
	    }
	    return value;
	}
    }
}
