package eup;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.ext.DefaultHandler2;

import webPage.UnacceptableFormsException;
import webPage.WPLocation;
import webPage.WebPage;

import java.awt.Container;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
 * @version 1.0 24 June 2018
 * 
 * @since 1.0
 * 
 */

public class SmartMeterTexasDataCollector {

    private Feedbacker fb;

    // Maximum number of tries in case
    // of a redirect.
    private static final int tryRedirectMax = 10;

    private static final String msgDown = "The application server "
	    + "is down at this time.";

    private Map<String, String> hiddenInputFields = Util.makeLinkedHashMap();

    private HttpClient client; // Handles the work, holds context
    // (i.e. cookies).
    
    String addressSuffix = "" ;
    
    private static final String field1Begin = "<select name='";
    private static final String field1End = "' ";
    private static final String field2Begin = "<input type='text' name='";
    private static final String field2End = "' ";
    private static final String field3Begin = "<a name='";
    private static final String field3End = "'";

    private final FieldManager fm1 = FieldManager.makeFieldManager(field1Begin,
	    field1End);
    private final FieldManager fm2 = FieldManager.makeFieldManager(field2Begin,
	    field2End);
    private final FieldManager fm3 = FieldManager.makeFieldManager(field3Begin,
	    field3End);

    final Collection<FieldManager> fmCollection = FieldManager
	    .makeFmCollection(fm1, fm2, fm3);


    // The following can be enabled to provide some debugging
    // information on System.out
    private boolean displayResponseBody = false;
    private boolean displayQueryOptions = false;
    private boolean displayHeadersAndFooters = false;
    private boolean displayCookies = false;
    private boolean displayPostParameters = false;

    /**
     * No-argument constructor for getting schedule of classes information for
     * any semester at Northern Arizona University. Note that the semester is
     * given as a <tt>String</tt> whose value is determined by a web-based form
     * and is thus arbitrary. Here is a partial list as of 14 March 2010:
     * 
     * Semester Description
     * 
     * 1107 Fall 2010 1104 Summer 2010 1101 Spring 2010 1098 Winter 2009 1097
     * Fall 2009 1094 Summer 2009
     * 
     */
    public SmartMeterTexasDataCollector() {
	client = new HttpClient();

	makeSessionOps();
//	makeDeptOps();
//	makeCourseOps();
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
	Iterator<Map.Entry<String, String>> it = hiddenInputFields.entrySet()
		.iterator();
	while (it.hasNext()) {
	    Map.Entry<String, String> entry = it.next();
	    method.addParameter(entry.getKey(), entry.getValue());
	}

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
	    msg("" + params.length + " parameters:");
	    for (int i = 0; i < params.length; i++) {
		msg("    " + params[i].getName() + "=" + params[i].getValue());
	    }
	}
	if (displayResponseBody) {
	    msg("*******************************************    POST  START");
	}
	//
	// 3 Nov 2012 - Be lenient about cookies. NAU cookies violate
	// RFC 2109 and cause a warning.
	//
	method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
	wp = requestResponse(method);
	if (displayResponseBody) {
	    msg("*******************************************    POST  END");
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
			msg(toString() + "#requestResponse failed attempt to "
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
		    msg(toString() + "#requestResponse redirecting for "
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
		msg(toString()
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
		    msg(rline);
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
		msg("==========    OTHER INFORMATION    ==========");
	    }
	    if (displayQueryOptions) {
		msg("Follow redirects = " + method.getFollowRedirects());
		msg("Do authentication = " + method.getDoAuthentication());
		msg("Path used = " + method.getPath());
		msg("Query = " + method.getQueryString());
		StatusLine sl = method.getStatusLine();
		if (null != sl) {
		    msg("Status = " + sl.toString());

		}
	    }
	    if (displayHeadersAndFooters) {
		Header[] h = method.getResponseHeaders();
		for (int i = 0; i < h.length; i++) {
		    msg("Header " + i + ":  " + h[i].toString());
		}
		Header[] f = method.getResponseFooters();
		for (int i = 0; i < f.length; i++) {
		    msg("Footer " + i + ":  " + f[i].toString());
		}
	    }
	    if (displayCookies) {
		Cookie[] c = client.getState().getCookies();
		for (int i = 0; i < c.length; i++) {
		    msg("Cookie " + i + ":  " + c[i].toString());
		}
	    }
	    if (displayQueryOptions || displayHeadersAndFooters
		    || displayCookies) {
		msg("==========    END   INFORMATION    ==========");
	    }

	    // Release the connection.
	    method.releaseConnection();
	}
	if (null == wp) {
	    if (null != hiddenInputFields) {
		hiddenInputFields.clear();
	    }
	} else {
	    // Get hidden input fields in preparation for the
	    // possibility that they will be needed if the
	    // next operation is a POST.
	    try {
		// /
		// / Updated 3 Nov 2012 by Ian Shef ibs
		// / due to NAU web site changes.
		// /
		// / hiddenInputFields = getHiddenFieldsInFirstForm(wp);
		// /
		// / Add any new hidden input fields, but don't
		// / delete any existing hidden input fields that
		// / are unchanged.
		Map<String, String> m = getHiddenFieldsInFirstForm(wp);
		if (m != null) {
		    hiddenInputFields.putAll(m);
		}
	    } catch (UnacceptableFormsException e) {
		//
		// Cannot properly find hidden input fields,
		// so clear out list.
		hiddenInputFields.clear();
		//
		// Error could be due to:
		// Transport (communications), or
		// Programming error, or
		// Presented this way when server is down.
		// Check for this last possibility.
		WPLocation serverDown = wp.indexOf(msgDown);
		if ((serverDown.getLine() > -1)
			&& (serverDown.getColumn() > -1)) {
		    fb.log("No scheduling is possible now, "
			    + "please try again later.", Feedbacker.TO_FILE
			    + Feedbacker.TO_GUI + Feedbacker.FLUSH);
		} else {
		    throw new UnacceptableFormsException(e.getMessage(),
			    e.getCause());
		}
	    }
	}
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
    private static Map<String, String> getHiddenFieldsInFirstForm(WebPage wp) {
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
			&& (wp.line(current).indexOf("iframeDoc.write(\"") < 0)) {
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

    /**
     * A convenience method for displaying a line of text on System.out.
     * 
     * @param ob
     *            An <tt>Object</tt> or a <tt>String</tt> to be displayed on
     *            System.out. If an <tt>Object</tt>, its toString() method will
     *            be called.
     */
//    public void msg(Object ob) {
//	if (null == fb) {
//	    System.out.println(ob);
//	} else {
//	    fb.log(ob, Feedbacker.TO_OUT + Feedbacker.TO_FILE);
//	}
//    }

//	int sessionTotal = sessionOps.size();
//	int sessionProgress = 100;
//	for (SessionOp op : sessionOps) {
//	    fb.progressAnnounce(sessionProgress / sessionTotal,
//		    "Setting up to get sessions");
//	    op.process();
//	    sessionProgress += 100;


//	fb.progressAnnounce(0, "Setting up to get departments");
//	int deptTotal = deptOps.size();
//	int deptProgress = 100;
//	for (DeptOp op : deptOps) {
//	    fb.progressAnnounce(deptProgress / deptTotal, "Getting departments");
//	    op.process(session);
//	    deptProgress += 100;
//	}
//	fb.log("Found " + departments.size() + " departments.",
//		Feedbacker.TO_FILE + Feedbacker.TO_GUI);


    public Feedbacker getFeedbacker() {
	return fb;
    }

    public void setFeedbacker(Feedbacker fb) {
	this.fb = fb;
    }

//    @Override
//    public String toString() {
//	return Util.getCallerClassName() + 
//		" " +
//		Util.getCallerMethodName() +
//		" is incomplete.";
//    }

    static Feedbacker setupFeedbacker() {
	final ArrayList<Feedbacker> holder = Util.makeArrayList(1);
	try {
	    javax.swing.SwingUtilities.invokeAndWait((new Runnable() {
		@Override
		public void run() {
		    final FeedbackerImplementation fb1 = new FeedbackerImplementation();
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
     * A main program for testing purposes to develop and test access to the
     * schedule of classes at Northern Arizona University.
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

//    private class SessionOp {
//	private final String url;
//
//	SessionOp(String url) {
//	    this.url = url;
//	}
//
//	WebPage process() {
//	    return getPage(url);
//	}
//    }

//    private class DeptOp {
//	private final String url;
//
//	DeptOp(String url) {
//	    this.url = url;
//	}
//
	/**
	 * @param session
	 *            Provides the session.
	 */
//	WebPage process(Session session) {
//	    Collection<NameValuePair> nvpc = Util.makeArrayList(40);
//	    nvpc.add(new NameValuePair("ICAJAX", "1"));
//	    nvpc.add(new NameValuePair("ICNAVTYPEDROPDOWN", "1"));
//	    nvpc.add(new NameValuePair("ICType", "Panel"));
//	    nvpc.add(new NameValuePair("ICElementNum", "0"));
//	    nvpc.add(new NameValuePair("ICAction",
//		    "CLASS_SRCH_WRK2_STRM%2435%24"));
//	    nvpc.add(new NameValuePair("ICXPos", "0"));
//	    nvpc.add(new NameValuePair("ICYPos", "0"));
//	    nvpc.add(new NameValuePair("ResponsetoDiffFrame", "-1"));
//	    nvpc.add(new NameValuePair("TargetFrameName", "None"));
//	    nvpc.add(new NameValuePair("FacetPath", "None"));
//	    nvpc.add(new NameValuePair("ICFocus", ""));
//	    nvpc.add(new NameValuePair("ICSaveWarningFilter", "0"));
//	    nvpc.add(new NameValuePair("ICChanged", "-1"));
//	    nvpc.add(new NameValuePair("ICResubmit", "0"));
//	    nvpc.add(new NameValuePair("ICActionPrompt", "false"));
//	    nvpc.add(new NameValuePair("ICFind", ""));
//	    nvpc.add(new NameValuePair("ICAddCount", ""));
//	    nvpc.add(new NameValuePair("ICAPPCLSDATA", ""));
//	    nvpc.add(new NameValuePair("CLASS_SRCH_WRK2_INSTITUTION$31$",
//		    "NAU00"));
//	    nvpc.add(new NameValuePair("CLASS_SRCH_WRK2_STRM$35$", session
//		    .getCode()));
//	    nvpc.add(new NameValuePair("SSR_CLSRCH_WRK_SUBJECT_SRCH$0", ""));
//	    nvpc.add(new NameValuePair("SSR_CLSRCH_WRK_SSR_EXACT_MATCH1$1", "E"));
//	    nvpc.add(new NameValuePair("SSR_CLSRCH_WRK_CATALOG_NBR$1", ""));
//	    nvpc.add(new NameValuePair(
//		    "SSR_CLSRCH_WRK_N__ONLY_ONLINE_CMP$chk$3", ""));
//	    nvpc.add(new NameValuePair("SSR_CLSRCH_WRK_CAMPUS$3", ""));
//	    nvpc.add(new NameValuePair("SSR_CLSRCH_WRK_RQMNT_DESIGNTN$6", ""));
//	    nvpc.add(new NameValuePair("SSR_CLSRCH_WRK_ACAD_CAREER$7", ""));
//	    nvpc.add(new NameValuePair("SSR_CLSRCH_WRK_SSR_OPEN_ONLY$chk$8",
//		    "Y"));
//	    nvpc.add(new NameValuePair("SSR_CLSRCH_WRK_SSR_OPEN_ONLY$8", "Y"));
//
//	    Collection<NameValuePair> nvrc = Util.makeArrayList(2);
//	    // nvrc.add(new NameValuePair(
//	    // "N__SR_CLASS_WRK_N__ONLY_ONLINE_CMP$chk", "")) ;
//	    // nvrc.add(new NameValuePair(
//	    // "CLASS_SRCH_WRK2_SSR_OPEN_ONLY$chk", "")) ;
//	    WebPage wp = getPage(getUrl(), nvpc, nvrc);
//	    // fb.log(wp) ;
//	    // for (String s : wp.getLines()) {
//	    // fb.log(s) ;
//	    // }
//	    Parser parser = new Parser();
//	    DeptCH ch = new DeptCH(session);
//	    parser.setContentHandler(ch);
//	    try {
//		parser.parse(makeWebPageInputSource(wp));
//	    } catch (IOException e) {
//		e.printStackTrace();
//	    } catch (SAXException e) {
//		e.printStackTrace();
//	    }
//	    departments.addAll(ch.getDepartments());
//	    return wp;
//	}

	/**
	 * @return the url
	 */
//	public String getUrl() {
//	    return url;
//	}
//    }


    private final void makeSessionOps() {
	@SuppressWarnings("unused")
	String u;
	u = "https:"
		+ "//www.peoplesoft.nau.edu:443/psp/ps90prcs/EMPLOYEE/HRMS/c/"
		+ "COMMUNITY_ACCESS.CLASS_SEARCH.GBL?"
		+ "Page=SSR_CLSRCH_MAIN&Action=U";
//	sessionOps.add(new SessionOp(u));
	u = "https:"
		+ "//www.peoplesoft.nau.edu:443/psp/ps90prcs/EMPLOYEE/HRMS/c/"
		+ "COMMUNITY_ACCESS.CLASS_SEARCH.GBL?"
		+ "Page=SSR_CLSRCH_MAIN&Action=U&";
//	sessionOps.add(new SessionOp(u));
	u = "https:"
		+ "//www.peoplesoft.nau.edu:443/psp/ps90prcs/EMPLOYEE/HRMS/c/"
		+ "COMMUNITY_ACCESS.CLASS_SEARCH.GBL?"
		+ "Page=SSR_CLSRCH_MAIN&Action=U";
//	sessionOps.add(new SessionOp(u));
	u = "https:"
		+ "//www.peoplesoft.nau.edu:443/psc/ps90prcs/EMPLOYEE/HRMS/c/"
		+ "COMMUNITY_ACCESS.CLASS_SEARCH.GBL?"
		+ "Page=SSR_CLSRCH_MAIN&Action=U&"
		+ "PortalActualURL=https%3a%2f%2fwww.peoplesoft.nau.edu"
		+ "%2fpsc%2fps90prcs%2fEMPLOYEE%2fHRMS%2fc%2f"
		+ "COMMUNITY_ACCESS.CLASS_SEARCH.GBL"
		+ "%3fPage%3dSSR_CLSRCH_MAIN%26"
		+ "Action%3dU&PortalContentURL=https%3a%2f%2f"
		+ "www.peoplesoft.nau.edu%2fpsc%2fps90prcs%2fEMPLOYEE"
		+ "%2fHRMS%2fc%2f"
		+ "COMMUNITY_ACCESS.CLASS_SEARCH.GBL&"
		+ "PortalContentProvider=HRMS&"
		+ "PortalCRefLabel=Class%20Search%2fBrowse%20"
		+ "Catalog&PortalRegistryName=EMPLOYEE&"
		+ "PortalServletURI=https%3a%2f%2fwww.peoplesoft.nau.edu"
		+ "%2fpsp%2f"
		+ "ps90prcs%2f&PortalURI=https%3a%2f%2fwww.peoplesoft.nau.edu"
		+ "%2fpsc%2f"
		+ "ps90prcs%2f&PortalHostNode=HRMS&NoCrumbs=yes";
//	sessionOps.add(new SessionOp(u) {
//	    @SuppressWarnings("synthetic-access")
//	    @Override
//	    WebPage process() {
//		WebPage wp = super.process();
//		WebPage wp;
//		Parser parser = new Parser();
//		SessionCH ch = new SessionCH();
//		parser.setContentHandler(ch);
//		try {
//		    parser.parse(makeWebPageInputSource(wp));
//		} catch (IOException e) {
//		    e.printStackTrace();
//		} catch (SAXException e) {
//		    e.printStackTrace();
//		}
//		sessions = ch.getSessions();
//		defaultSemesterCode = ch.getDefaultSession().getCode();
//		return wp;
//	    }
//    }

//    private final List<DeptOp> deptOps = Util.makeArrayList(1);

//    private final void makeDeptOps() {
//	String u;
//	u = "https:" + "//www.peoplesoft.nau.edu:443/psc/ps90prcs/"
//		+ "EMPLOYEE/HRMS/c/" + "COMMUNITY_ACCESS.CLASS_SEARCH.GBL";
//	deptOps.add(new DeptOp(u) {
//	    @Override
//	    WebPage process(Session session) {
//		return getPage(getUrl());
//	    }
//	});
//	deptOps.add(new DeptOp(u));
//    }


//    private final void makeCourseOps() {
//	final String u = "https:"
//		+ "//www.peoplesoft.nau.edu:443/psc/ps90prcs/EMPLOYEE/HRMS/c/"
//		+ "COMMUNITY_ACCESS.CLASS_SEARCH.GBL";

//	courseOps.add(new CourseOp(u) {
//	    private boolean displayCourseOp1WebPageInFile = false ;
//	    boolean displayCourseOp1WebPageInFile = false ;
//	    @SuppressWarnings("hiding")
//	    @Override
//	    WebPage process(Department dept) {
//		String term = dept.getSession().getCode() ;
//		Collection<NameValuePair> nvpc = Util.makeArrayList(1) ;
//		Collection<NameValuePair> nvpr = Collections.emptyList() ;
//		addNVP(nvpc, "ICAJAX", "1") ;
//		addNVP(nvpc, "ICNAVTYPEDROPDOWN", "1") ;
//		addNVP(nvpc, "ICType", "Panel") ;
//		addNVP(nvpc, "ICElementNum", "0") ;
//		//
//		// Dollar signs in two places replaced %24 in two places.
//		//
//		addNVP(nvpc, "ICAction", "CLASS_SRCH_WRK2_STRM$35$") ;
//		addNVP(nvpc, "ICXPos", "0") ;
//		addNVP(nvpc, "ICYPos", "0") ;
//		addNVP(nvpc, "ResponsetoDiffFrame", "-1") ;
//		addNVP(nvpc, "TargetFrameName", "None") ;
//		addNVP(nvpc, "FacetPath", "None") ;
//		addNVP(nvpc, "ICFocus", "") ;
//		addNVP(nvpc, "ICSaveWarningFilter", "0") ;
//		addNVP(nvpc, "ICChanged", "-1") ;
//		addNVP(nvpc, "ICResubmit", "0") ;
//		addNVP(nvpc, "ICActionPrompt", "false") ;
//		addNVP(nvpc, "ICFind", "") ;
//		addNVP(nvpc, "ICAddCount", "") ;
//		addNVP(nvpc, "ICAPPCLSDATA", "") ;
//		addNVP(nvpc, "CLASS_SRCH_WRK2_INSTITUTION$31$", "NAU00") ;
//		addNVP(nvpc, "CLASS_SRCH_WRK2_STRM$35$", term) ;
//		addNVP(nvpc, "SSR_CLSRCH_WRK_SUBJECT_SRCH$0", "") ;
//		addNVP(nvpc, "SSR_CLSRCH_WRK_SSR_EXACT_MATCH1$1", "E") ;
//		addNVP(nvpc, "SSR_CLSRCH_WRK_CATALOG_NBR$1", "") ;
//		addNVP(nvpc, "SSR_CLSRCH_WRK_N__ONLY_ONLINE_CMP$chk$3", "") ;
//		addNVP(nvpc, "SSR_CLSRCH_WRK_CAMPUS$3", "") ;
//		addNVP(nvpc, "SSR_CLSRCH_WRK_RQMNT_DESIGNTN$6", "") ;
//		addNVP(nvpc, "SSR_CLSRCH_WRK_ACAD_CAREER$7", "") ;
//		addNVP(nvpc, "SSR_CLSRCH_WRK_SSR_OPEN_ONLY$chk$8", "Y") ;
//		addNVP(nvpc, "SSR_CLSRCH_WRK_SSR_OPEN_ONLY$8", "Y") ;
//		WebPage result = getPage(getUrl(), nvpc, nvpr);
//		WebPage result;
//		if (displayCourseOp1WebPageInFile) {
//		    displayWebPageInFile(result);
		}
//		return result;
//	    }
//	}
//		);

//	courseOps.add(new CourseOp(u) {
//	    @Override
//	    WebPage process(Department dept) {
//		String term = dept.getSession().getCode() ;
//		String subject = dept.getAbbreviation() ;
//		String greaterThanOrEqualTo = "G" ;
//		String courseStart = "1" ;
//		Collection<NameValuePair> nvpc = Util.makeArrayList(1) ;
//		Collection<NameValuePair> nvpr = Collections.emptyList() ;
//		addNVP(nvpc, "ICAJAX", "1") ;
//		addNVP(nvpc, "ICNAVTYPEDROPDOWN", "1") ;
//		addNVP(nvpc, "ICType", "Panel") ;
//		addNVP(nvpc, "ICElementNum", "0") ;
//		addNVP(nvpc, "ICAction", "CLASS_SRCH_WRK2_SSR_PB_CLASS_SRCH") ;
//		addNVP(nvpc, "ICXPos", "0") ;
//		addNVP(nvpc, "ICYPos", "0") ;
//		addNVP(nvpc, "ResponsetoDiffFrame", "-1") ;
//		addNVP(nvpc, "TargetFrameName", "None") ;
//		addNVP(nvpc, "FacetPath", "None") ;
//		addNVP(nvpc, "ICFocus", "") ;
//		addNVP(nvpc, "ICSaveWarningFilter", "0") ;
//		addNVP(nvpc, "ICChanged", "-1") ;
//		addNVP(nvpc, "ICResubmit", "0") ;
//		addNVP(nvpc, "ICActionPrompt", "false") ;
//		addNVP(nvpc, "ICFind", "") ;
//		addNVP(nvpc, "ICAddCount", "") ;
//		addNVP(nvpc, "ICAPPCLSDATA", "") ;
//		addNVP(nvpc, 
//			"CLASS_SRCH_WRK2_INSTITUTION$31$",  
//			"NAU00") ;
//		addNVP(nvpc, 
//			"CLASS_SRCH_WRK2_STRM$35$", 
//			term) ;
//		addNVP(nvpc, 
//			"SSR_CLSRCH_WRK_SUBJECT_SRCH$0", 
//			subject) ;
//		addNVP(nvpc, 
//			"SSR_CLSRCH_WRK_SSR_EXACT_MATCH1$1", 
//			greaterThanOrEqualTo) ;
//		addNVP(nvpc, 
//			"SSR_CLSRCH_WRK_CATALOG_NBR$1",
//			courseStart) ;
//		addNVP(nvpc, 
//			"SSR_CLSRCH_WRK_N__ONLY_ONLINE_CMP$chk$3",
//				"") ;
//		addNVP(nvpc, 
//			"SSR_CLSRCH_WRK_CAMPUS$3",
//			"") ;
//		addNVP(nvpc, 
//			"SSR_CLSRCH_WRK_RQMNT_DESIGNTN$6",
//			"") ;
//		addNVP(nvpc, 
//			"SSR_CLSRCH_WRK_ACAD_CAREER$7",
//			Course_Level_Chosen) ;
//		addNVP(nvpc, 
//			"SSR_CLSRCH_WRK_SSR_OPEN_ONLY$chk$8",
//			Sections_Open) ;
//		WebPage result = getPage(getUrl(), nvpc, nvpr);
//		Parser parser = new Parser();
//		CourseCH ch = new CourseCH(dept);
//		parser.setContentHandler(ch);
//		try {
//		    parser.parse(makeWebPageInputSource(result));
//		} catch (IOException e) {
//		    e.printStackTrace();
//		} catch (SAXException e) {
//		    e.printStackTrace();
//		}
//		coursesCO = ch.getCourses();
//		return result;
//	    }
//	}
//		);
//////////////////////////////////////////////////////////////
	// /
	// / Post Operation #1 - After this, ready for a new search.
	// /
//	courseOps.add(new CourseOp(u) {
//	    private boolean displayNewSearchWebPageInFile = false ;
//	    @Override
//	    WebPage process(Department dept) {
//		Collection<NameValuePair> nvpc = Util.makeArrayList(18);
//		Collection<NameValuePair> nvpr = Collections.emptyList() ;
//		addNVP(nvpc, "ICAJAX" , "1") ;
//		addNVP(nvpc, "ICNAVTYPEDROPDOWN" , "1") ;
//		addNVP(nvpc, "ICType" , "Panel") ;
//		addNVP(nvpc, "ICElementNum" , "0") ;
//		addNVP(nvpc, "ICAction" , "CLASS_SRCH_WRK2_SSR_PB_NEW_SEARCH") ;
//		addNVP(nvpc, "ICXPos" , "0") ;
//		addNVP(nvpc, "ICYPos" , "0") ;
//		addNVP(nvpc, "ResponsetoDiffFrame" , "-1") ;
//		addNVP(nvpc, "TargetFrameName" , "None") ;
//		addNVP(nvpc, "FacetPath" , "None") ;
//		addNVP(nvpc, "ICFocus" , "") ;
//		addNVP(nvpc, "ICSaveWarningFilter" , "0") ;
//		addNVP(nvpc, "ICChanged" , "-1") ;
//		addNVP(nvpc, "ICResubmit" , "0") ;
//		addNVP(nvpc, "ICActionPrompt" , "false") ;
//		addNVP(nvpc, "ICFind" , "") ;
//		addNVP(nvpc, "ICAddCount" , "") ;
//		addNVP(nvpc, "ICAPPCLSDATA" , "") ;
//		WebPage result = getPage(getUrl(), nvpc, nvpr);
//		if (displayNewSearchWebPageInFile) {
//		    displayWebPageInFile(result);
//		}
//		return result;
//	    }
//	}) ;
//    }

//    @Override
//    public Collection<Section> getSections(Course course) {
//	// Should never get called, because sections were provided when
//	// departments were provided.
//	throw new AssertionError(toString()
//		+ "#getSections(Course) was called unexpectedly.");
//    }

    /**
     * @param input
     */
//    void displayWebPageInFile(WebPage input) {
//	boolean APPEND = true ;
//	boolean AUTOFLUSH = false ;
//	String dir = System.getProperty("user.home") ;
//	String sep = System.getProperty("file.separator") ;
//	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss") ;
//	String path = dir + sep + "Desktop" + sep 
//		+ "NAUClassScheduleSpiderWebPage ("
//		+ sdf.format(new Date())
//		+ ").txt" ;
//	FileWriter fw = null ;
//	try {
//	    fw = new FileWriter( path, APPEND );
//	} catch (IOException e) {
//	    e.printStackTrace();
//	}
//	final BufferedWriter bw = new BufferedWriter( fw, 16_384) ;
//	final PrintWriter prw = new PrintWriter( bw, AUTOFLUSH);
//	prw.println("==========================================") ;
//	for ( String line : input.getLines()) {
//	    prw.println(line) ;
//	}
//	prw.println("==========================================") ;
//	prw.close() ;
//    }
//}

class SessionCH extends DefaultHandler2 {
    //
    // for enabling debugging messages
    //
//    private boolean displayExtractedSessions = false;
//    private boolean displaySessionInfo = false;
    //
    // Concatenating text.
    //
    private StringBuilder textAccum = new StringBuilder(2048);
    //
    //
    //

//    private enum State {
//	RESET_SESSION, WAIT_FOR_SESSION, GET_SESSION, GET_SESSION_DEFAULT, 
//	ENDED_SESSION_PROCESSING
//    }
//
//    private State state = State.RESET_SESSION;

    @Override
    public void characters(char[] ch, int start, int length) {
	if (0 == textAccum.length())
	    textAccum.append("char: ");
	String tail = new String(ch, start, length).trim();
	textAccum.append(tail);
    }

//    @Override
//    public void endElement(String uri, String localName, String name) {
//	handleChars(); // Deal with accumulated characters.
//	State newState = state;
//	switch (state) {
//	case GET_SESSION:
//	    if (localName.equals("select"))
//		newState = State.ENDED_SESSION_PROCESSING;
//	    break;
//	case GET_SESSION_DEFAULT:
//	    if (localName.equals("option"))
//		newState = State.GET_SESSION;
//	    break;
//	default: /* Do nothing */
//	    break;
//	}
//	setState(newState);
//    }
//
//    @Override
//    public void startDocument() {
//	handleChars(); // Deal with accumulated characters.
//	setState(State.WAIT_FOR_SESSION);
//    }

//    @Override
//    public void startElement(String uri, String localName, String name,
//	    Attributes atts) {
//	handleChars(); // Deal with accumulated characters.
//	State newState = state;
//	switch (state) {
//	case RESET_SESSION: /* Do nothing */
//	    break;
//	case WAIT_FOR_SESSION:
//	    if ((localName.equals("select")) && (name.equals("select"))
//		    //
//		    // Updated 8 June 2010 by Ian Shef ibs
//		    //
//		    // && (atts.getValue("id")).equals("CLASS_SRCH_WRK2_STRM$47$")) {
//		    //
//		    // Updated 3 Nov 2101 by Ian Shef ibs
//		    //
//		    // && (atts.getValue("id")).equals("CLASS_SRCH_WRK2_STRM$48$")) {
//		    //
//		    // Updated 1 April 2014 by Ian Shef ibs
//		    //
//		    //
//		    // && (atts.getValue("id")).equals("CLASS_SRCH_WRK2_STRM$52$")) {
//		    && (atts.getValue("id"))
//		    .startsWith("CLASS_SRCH_WRK2_STRM$")) {
//
//		newState = State.GET_SESSION;
//	    }
//	    break;
//	case GET_SESSION:
//	    if ((localName.equals("option")) && (name.equals("option"))
//		    && ("selected".equals(atts.getValue("selected")))) {
//		newState = State.GET_SESSION_DEFAULT;
//	    }
//	    break;
//	case ENDED_SESSION_PROCESSING: /* Do nothing */
//	    break;
//	default: /* Do nothing */
//	    break;
	}
//	setState(newState);
//    }

//    @Override
//    public String toString() {
//	return new String(getClass().getName());
//    }

    /**
     * @return the sessions
     */
//    Collection<Session> getSessions() {
//	return sessions;
//    }

    /**
     * @return the defaultSession
     */
//    Session getDefaultSession() {
//	return defaultSession;
//    }

//    private void msg(Object ob) {
//	System.out.println(ob);
//    }

//    private void handleChars() {
//	if (textAccum.length() == 0)
//	    return; // Nothing to do.
//	if (textAccum.toString().trim().equals("char:"))
//	    return;
//
//	if ((state == State.GET_SESSION)
//		|| (state == State.GET_SESSION_DEFAULT)) {
//	    String s = textAccum.substring(6).toString();
//	    Session session = Session.EMPTY;
//	    String[] parts = s.split("-");
//	    if (parts.length == 2) {
//		if (displaySessionInfo) {
//		    msg("Session (code, name) = (" + parts[0] + "," + parts[1]
//			    + ")");
//		}
//		session = new Session.Builder(parts[1], parts[1], parts[0])
//		.build();
//		sessions.add(session);
//	    } else {
//		if (displaySessionInfo) {
//		    msg("Unexpected session informationignored: " + textAccum);
//		}
//	    }
//	    if (state == State.GET_SESSION_DEFAULT) {
//		if (displayExtractedSessions) {
//		    msg("DEFAULT \\/ \\/ \\/");
//		}
//		if (defaultSession != null) {
//		    throw new AssertionError(getClass().getName()
//			    + " expected defaultSession to be null, but it "
//			    + "contained " + defaultSession);
//		}
//		defaultSession = session;
//	    }
//	    if (displayExtractedSessions) {
//		msg("SESSION:  [" + parts[0] + "," + parts[1] + "]");
//	    }
//	}
//	textAccum.setLength(0); // Clear the accumulation buffer.
//    }

//    private void setState(State state) {
//	if (state != this.state) {
//	    // msg("State changed to " + state);
//	    this.state = state;
//	}
//    }
//}

class DeptCH extends DefaultHandler2 {
    //
    // for enabling debugging messages
    //
    private boolean displayExtractedDepartments = false ;
    private boolean displayXML = false ;
//    private boolean displayStateChanges = false ;
    //
    // Concatenating text.
    //
    private StringBuilder textAccum = new StringBuilder(2048);
    //
    //
    //
//    private Collection<Department> departments = Util.makeArrayList(150);
//
//    private enum State {
//	RESET_DEPT, WAIT_FOR_DEPT, GET_DEPT, ENDED_DEPT_PROCESSING
//    }
//
//    private State state = State.RESET_DEPT;
//    private Session session;
//
//    DeptCH(Session session) {
//	this.session = session;
//    }

    @Override
    public void characters(char[] ch, int start, int length) {
	if (0 == textAccum.length())
	    textAccum.append("char: ");
	String tail = new String(ch, start, length).trim();
	textAccum.append(tail);
    }

    @Override
    public void endElement(String uri, String localName, String name) {
	handleChars(); // Deal with accumulated characters.
	if (displayXML) {
	    msg("endElement: [" + uri + "," + localName + "," + name + "]");
	}
    }

    @Override
    public void startDocument() {
	handleChars(); // Deal with accumulated characters.
	if (displayXML) {
	    msg("startDocument");
	}
//	setState(State.WAIT_FOR_DEPT);
    }

    @Override
    public void startElement(String uri, String localName, String name,
	    Attributes atts) {
	handleChars(); // Deal with accumulated characters.
	if (displayXML) {
	    StringBuilder result = new StringBuilder();
	    int count = atts.getLength();
	    for (int i = 0; i < count; i++) {
		if (i > 0)
		    result.append(", ");
		result.append(atts.getURI(i)).append("|");
		result.append(atts.getQName(i)).append("|");
		result.append(atts.getLocalName(i)).append("|");
		result.append(atts.getType(i)).append("|");
		result.append(atts.getValue(i));
	    }
	    msg("startElement: [" + uri + "," + localName + "," + name + "] "
		    + result.toString());
	}
    }

    @Override
    public String toString() {
	return new String(getClass().getName());
    }

    /**
     * @return the sessions
     */
//    Collection<Department> getDepartments() {
//	return departments;
//    }

    private void msg(Object ob) {
	System.out.println(ob.toString());
    }

    private void handleChars() {
	if (textAccum.length() == 0)
	    return; // Nothing to do.
	if (textAccum.toString().trim().equals("char:"))
	    return;
//	State newState = state;
//
//	switch (state) {
//	case GET_DEPT:
//	    String s = textAccum.toString();
//	    if (s.startsWith("char: Course Number")) {
//		newState = State.ENDED_DEPT_PROCESSING;
//	    } else {
//		s = textAccum.substring(6).toString();
//		String[] parts = s.split("-");
//		if (parts.length == 2) {
//		    for (int i = 0; i < parts.length; i++) {
//			parts[i] = SmartMeterTexasDataCollector.htmlToText(parts[i]);
//		    }
//		    Department department = new Department.Builder(session,
//			    parts[0].trim()).name(parts[1].trim()).build();
//		    departments.add(department);
//		    if (displayExtractedDepartments) {
//			msg("DEPARTMENT:  [" + parts[0] + "," + parts[1] + "]");
//		    }
//		}
//	    }
//	    break;
//	case WAIT_FOR_DEPT:
//	    String deptInfo = textAccum.toString();
//	    // msg("....WAIT_FOR_DEPT handling " + deptInfo) ;
//	    if (deptInfo.startsWith("char: Course Subject")) {
//		newState = State.GET_DEPT;
//	    }
//	    if (deptInfo.startsWith("char: <table  border='0' "
//		    + "id='ACE_width' cellpadding='0' cellspacing='0' "
//		    + "class='PSPAGECONTAINER' ")) {
//		// msg("......WAIT_FOR_DEPT will parse.") ;
//		handleNAUDeptsNov2012(deptInfo);
//		newState = State.ENDED_DEPT_PROCESSING;
//		// } else {
//		// msg("......WAIT_FOR_DEPT skipping parse.") ;
//	    }
//	    break;
//	default:
//	    break;
//	}
//	setState(newState);

	if (displayXML)
	    msg(textAccum);
	textAccum.setLength(0); // Clear the accumulation buffer.
    }

//    private void setState(State state) {
//	if (state != this.state) {
//	    if (displayStateChanges)
//		msg("State changed to " + state);
//	    this.state = state;
//	}
//    }

//    private void handleNAUDeptsNov2012(String s) {
	void handleNAUDeptsNov2012(String s) {
	int startDeptInfo = s.indexOf("id='SSR_CLSRCH_WRK_SUBJECT_SRCH$", 0);
	startDeptInfo = s.indexOf("<option value=", startDeptInfo);
	startDeptInfo = s.indexOf("</option>", startDeptInfo);
	int endAllDeptInfo = s.indexOf("</select>", startDeptInfo);
	boolean proceed = true;
	while (proceed) {
	    startDeptInfo = s.indexOf("<option value=", startDeptInfo);
	    if (startDeptInfo > endAllDeptInfo) {
		proceed = false;
		break;
	    }
	    startDeptInfo = s.indexOf(">", startDeptInfo);
	    startDeptInfo++;
	    int endDeptInfo = s.indexOf("</option>", startDeptInfo);
	    if ((startDeptInfo < 0) || (endDeptInfo < 0)
		    || (startDeptInfo > endDeptInfo)) {
		msg("startDeptInfo=" + startDeptInfo);
		msg("endDeptInfo=" + endDeptInfo);
		throw new AssertionError("handleNAUDept2012 has bad indices.");
	    }
	    String deptInfo = s.substring(startDeptInfo, endDeptInfo);
	    String[] parts = deptInfo.split("-");
	    if (parts.length == 2) {
		for (int i = 0; i < parts.length; i++) {
		    parts[i] = SmartMeterTexasDataCollector.htmlToText(parts[i]);
		}
//		Department department = new Department.Builder(session,
//			parts[0].trim()).name(parts[1].trim()).build();
//		departments.add(department);
		if (displayExtractedDepartments) {
		    msg("DEPARTMENT:  [" + parts[0] + "," + parts[1] + "]");
		}
	    }
	}
    }
}

class CourseCH extends DefaultHandler2 {
    //
    // for enabling debugging messages
    //
//    private boolean displayExtractedCourses = false ;
//    private boolean displayExtractedSections = false ;
//    private boolean displayXML = false ;
//    private boolean displayStateChanges = false ;
    //
    // The Department being processed.
    //
//    private final Department dept;
    //
    // Concatenating text.
    //
//    private StringBuilder textAccum = new StringBuilder(2048);
    //
    // Accumulating courses and sections.
    //
//    private List<Course> coursesCH = Util.makeArrayList(10);
//    private Course currentCourse;
//    private Section currentSection ;
//    private boolean statusOpen;
//    private String sectionNum;
//    private String medium;
//    private String daysAndTimes = "";
//    private String location = "";
//    private String instructor = "";

    //
    // ====================================================================
    //

//    CourseCH(Department dept) {
//	this.dept = dept;
    }

    //
    // ====================================================================
    //

//    private enum State {
//	RESET_COURSE, 
//	WAIT_FOR_COURSE, GET_COURSE_NO_NEST, GET_COURSE_ONE_NEST, 
//	WAIT_FOR_OPEN_STATUS, GET_OPEN_STATUS, 
//	WAIT_FOR_SECTION_NUMBER1, GET_SECTION_NUMBER1, 
//	WAIT_FOR_SECTION_NUMBER2, GET_SECTION_NUMBER2, 
//	WAIT_FOR_DAYS_AND_TIMES, GET_DAYS_AND_TIMES, 
//	WAIT_FOR_LOCATION, GET_LOCATION, 
//	WAIT_FOR_INSTRUCTOR, GET_INSTRUCTOR, 
//	WAIT_FOR_DATES, GET_DATES, 
//	ENDED_COURSE
//    }

//    private State state = State.RESET_COURSE;

//    @Override
//    public void characters(char[] ch, int start, int length) {
//	if (0 == textAccum.length())
//	    textAccum.append("char: ");
//	String tail = new String(ch, start, length).trim();
//	textAccum.append(tail);
//    }

//    @Override
//    public void endDocument() {
//	handleChars(); // Deal with accumulated characters.
//	if (displayXML) {
//	    msg("endDocument") ;
//	}
//	State newState = state ;
//	if (state.equals(State.ENDED_COURSE)) {
//	    newState = State.RESET_COURSE ;
//	} 
//	setState(newState);
//    }

//    @Override
//    public void endElement(String uri, String localName, String name) {
//	handleChars(); // Deal with accumulated characters.
//	if (displayXML) {
//	    msg("endElement: [" + uri + "," + localName + "," + name + "]");
//	}
//	State newState = state;
//	setState(newState);
//    }
//
//    @Override
//    public void startDocument() {
//	handleChars(); // Deal with accumulated characters.
//	if (displayXML) {
//	    msg("startDocument");
//	}
//	if (state.equals(State.RESET_COURSE)) {
//	    setState(State.WAIT_FOR_COURSE);
//	}
//    }

//    @Override
//    public void startElement(String uri, String localName, String name,
//	    Attributes atts) {
//	handleChars(); // Deal with accumulated characters.
//	if (displayXML) {
//	    StringBuilder result = new StringBuilder();
//	    int count = atts.getLength();
//	    for (int i = 0; i < count; i++) {
//		if (i > 0)
//		    result.append(", ");
//		result.append(atts.getURI(i)).append("|");
//		result.append(atts.getQName(i)).append("|");
//		result.append(atts.getLocalName(i)).append("|");
//		result.append(atts.getType(i)).append("|");
//		result.append(atts.getValue(i));
//	    }
//
//	    msg("startElement: [" + uri + "," + localName + "," + name + "] "
//		    + result.toString());
//	}
//	State newState = state;
//	switch (state) {
//	case RESET_COURSE:
////	    Take no action.
//	    break ;
//	case GET_COURSE_NO_NEST:
////	    Take no action.
//	    break ;
//	case GET_SECTION_NUMBER1:
////	    Take no action.
//	    break ;
//	case GET_SECTION_NUMBER2:
////	    Take no action.
//	    break ;
//	case GET_DAYS_AND_TIMES:
////	    Take no action.
//	    break ;
//	case GET_LOCATION:
////	    Take no action.
//	    break ;
//	case GET_INSTRUCTOR:
////	    Take no action.
//	    break ;
//	case ENDED_COURSE:
////	    Take no action.
//	    break ;
//	case WAIT_FOR_COURSE:
//	    if (uri.equals("http://www.w3.org/1999/xhtml")
//		    && localName.equals("FIELD")
//		    && name.equals("FIELD")
//		    && atts.getType(0).equals("CDATA")
//		    && atts.getValue(0).equals("win0divPAGECONTAINER")) {
//		newState = State.GET_COURSE_NO_NEST ;
//	    }
//	    break ;
//	case GET_COURSE_ONE_NEST:
//	    final String PREFIX = "Collapse section " ;
//	    final int    LENGTH = PREFIX.length() ;
//	    if (uri.equals("http://www.w3.org/1999/xhtml")
//		    && localName.equals("img")
//		    && name.equals("img")
//		    && atts.getType(1).equals("CDATA")
//		    && atts.getValue(1).startsWith(PREFIX)) {
//		String[] parts = atts.getValue(1).substring(LENGTH).split("-");
//		if (parts.length > 1) {
//		    for (int i = 0; i < parts.length; i++)
//			parts[i] = parts[i].trim();
//		    int firstSpace = parts[0].indexOf(' ');
//		    String subject = parts[0].substring(0, firstSpace);
//		    int lastSpace = parts[0].lastIndexOf(' ');
//		    String courseNum = parts[0].substring(lastSpace + 1);
//		    if (displayExtractedCourses) {
//			msg("COURSE:  [" + subject + " " + courseNum + ": "
//				+ parts[1] + "]");
//		    }
//		    currentCourse = new Course.Builder(dept, courseNum)
//		    .subject(subject).title(parts[1]).build();
//		    coursesCH.add(currentCourse);
//		newState = State.WAIT_FOR_SECTION_NUMBER1 ;
//		}
//	    }
//	    break ;
//	case WAIT_FOR_SECTION_NUMBER1:
//	    if (uri.equals("http://www.w3.org/1999/xhtml")
//		    && localName.equals("a")
//		    && name.equals("a")
//		    && atts.getLength()>3
//		    && atts.getQName(4).equals("name")
//		    && atts.getLocalName(4).equals("name")
//		    && atts.getType(4).equals("CDATA")
//		    && atts.getValue(4).startsWith("MTG_CLASSNAME")) {
//		currentSection = null ;
//		newState = State.GET_SECTION_NUMBER1 ;
//	    } else if (uri.equals("http://www.w3.org/1999/xhtml")
//		    && localName.equals("a")
//		    && name.equals("a")
//		    && atts.getLength()>3
//		    && atts.getQName(4).equals("name")
//		    && atts.getLocalName(4).equals("name")
//		    && atts.getType(4).equals("CDATA")
//		    && atts.getValue(4).startsWith("SSR_CLSRSLT_WRK_GROUPBOX2")
//		    ) {
//		newState = State.GET_COURSE_ONE_NEST ;
//	    } else if (uri.equals("http://www.w3.org/1999/xhtml")
//		    && localName.equals("span")
//		    && name.equals("span")
//		    && atts.getLength()>0
//		    && atts.getQName(1).equals("id")
//		    && atts.getLocalName(1).equals("id")
//		    && atts.getType(1).equals("ID")
//		    && atts.getValue(1).startsWith("MTG_DAYTIME")
//		    ) {
//		newState = State.GET_DAYS_AND_TIMES ;
//	    }
//	    break ;
//	case WAIT_FOR_SECTION_NUMBER2:
//	    if (uri.equals("http://www.w3.org/1999/xhtml")
//		    && localName.equals("span")
//		    && name.equals("span")
//		    && atts.getLength()>0
//		    && atts.getType(0).equals("NMTOKEN")
//		    && atts.getValue(0).equals("PSHYPERLINKDISABLED")) {
//		newState = State.GET_SECTION_NUMBER2 ;
//	    }
//	    break ;
//	case WAIT_FOR_DAYS_AND_TIMES:
//	    if (uri.equals("http://www.w3.org/1999/xhtml")
//		    && localName.equals("span")
//		    && name.equals("span")
//		    && atts.getLength()>0
//		    && atts.getType(0).equals("NMTOKEN")
//		    && atts.getValue(0).equals("PSLONGEDITBOX")) {
//		newState = State.GET_DAYS_AND_TIMES ;
//	    }
//	    break ;
//	case WAIT_FOR_LOCATION:
//	    if (uri.equals("http://www.w3.org/1999/xhtml")
//		    && localName.equals("span")
//		    && name.equals("span")
//		    && atts.getLength()>0
//		    && atts.getType(0).equals("NMTOKEN")
//		    && atts.getValue(0).equals("PSLONGEDITBOX")) {
//		newState = State.GET_LOCATION ;
//	    }
//	    break ;
//	case WAIT_FOR_INSTRUCTOR:
//	    if (uri.equals("http://www.w3.org/1999/xhtml")
//		    && localName.equals("span")
//		    && name.equals("span")
//		    && atts.getLength()>1
//		    && atts.getType(0).equals("NMTOKEN")
//		    && atts.getValue(0).equals("PSLONGEDITBOX")) {
//		newState = State.GET_INSTRUCTOR ;
//	    }
//	    break ;
//	case WAIT_FOR_OPEN_STATUS:
//	    if (uri.equals("http://www.w3.org/1999/xhtml")
//		    && localName.equals("img")
//		    && name.equals("img")
//		    && atts.getLength()>0
//		    && atts.getType(0).equals("NMTOKEN")
//		    && atts.getValue(0).equals("SSSIMAGECENTER")) {
//		newState = State.GET_OPEN_STATUS ;
//	    }
//	    break ;
//	case GET_OPEN_STATUS:
//	    if (uri.equals("http://www.w3.org/1999/xhtml")
//		    && localName.equals("img")
//		    && name.equals("img")
//		    && atts.getLength()>4
//		    && atts.getType(0).equals("NMTOKEN")
//		    && atts.getValue(0).equals("SSSIMAGECENTER")) {
//		statusOpen = atts.getValue(5).startsWith("Open") ;
//		//
//		// Build a Section here.
//		//
//		Section.Builder sb = new Section.Builder(currentCourse,
//			sectionNum) ;
//		sb.dayAndTime(daysAndTimes);
//		sb.instructor(instructor);
//		sb.medium(medium);
//		sb.open(statusOpen);
//		sb.room(location);
//		sb.session("");
//		daysAndTimes = "";
//		sectionNum = "";
//		instructor = "";
//		medium = "";
//		location = "";
//		currentSection = sb.build() ;
//		currentCourse.addSection(currentSection);
//		if (displayExtractedSections) {
//		    msg("...Added section " + currentSection + " to course.") ;
//		}
//		//
//		newState = State.WAIT_FOR_SECTION_NUMBER1 ;
//	    }
//	    break ;
//	default:
//	    throw new AssertionError(
//		    "Unexpected default case #1 for " + state) ;
//	}
//	setState(newState);
//    }

    @Override
    public String toString() {
	return new String(getClass().getName());
    }

    private void msg(Object ob) {
	System.out.println(ob.toString());
    }

//    private void handleChars() {
////	String[] parts;
//	if (textAccum.length() == 0)
//	    return; // Nothing to do.
//	String s = textAccum.toString().substring(6);
//	if (s.length() == 0)
//	    return; // Nothing to do.
//
//	State newState = state;
//
//
//	if (displayXML)
//	    msg(textAccum);
//	switch (state) {
//	case RESET_COURSE:
////	    Take no action.
//	    break ;
//	case WAIT_FOR_COURSE:
////	    Take no action.
//	    break ;
//	case WAIT_FOR_OPEN_STATUS:
////	    Take no action.
//	    break ;
//	case WAIT_FOR_SECTION_NUMBER1:
////	    Take no action.
//	    break ;
//	case WAIT_FOR_SECTION_NUMBER2:
////	    Take no action.
//	    break ;
//	case WAIT_FOR_DAYS_AND_TIMES:
////	    Take no action.
//	    break ;
//	case WAIT_FOR_LOCATION:
////	    Take no action.
//	    break ;
//	case WAIT_FOR_INSTRUCTOR:
////	    Take no action.
//	    break ;
//	case GET_COURSE_ONE_NEST:
////	    Take no action.
//	    break;
//	case GET_OPEN_STATUS:
////	    Take no action.
//	    break;
//	case ENDED_COURSE:
////	    Take no action.
//	    break ;
//	case GET_COURSE_NO_NEST:
//	    setState(State.GET_COURSE_ONE_NEST);
//	    if (displayStateChanges) {
//		msg("==========          NESTED          ==========");
//	    }
//	    Parser parser = new Parser();
//	    CourseCH ch = new CourseCH(dept);
//	    ch.setState(state);
//	    parser.setContentHandler(ch);
//	    try {
//		parser.parse(SmartMeterTexasDataCollector
//			.makeCharSequenceInputSource(textAccum));
//	    } catch (IOException e) {
//		e.printStackTrace();
//	    } catch (SAXException e) {
//		e.printStackTrace();
//	    }
//	    coursesCH = new ArrayList<Course>(ch.getCourses()) ;
//	    newState = State.ENDED_COURSE ;
//	    if (displayStateChanges) {
//		msg("==========          UNNESTED          ==========");
//	    }
//	    break ;
//	case GET_SECTION_NUMBER1:
//	    sectionNum = s + "(" ;
//	    medium = s.split("-")[1].trim() ;
//	    newState = State.WAIT_FOR_SECTION_NUMBER2 ;
//	    break ;
//	case GET_SECTION_NUMBER2:
//	    sectionNum += s + ")" ;
//	    newState = State.WAIT_FOR_DAYS_AND_TIMES ;
//	    break ;
//	case GET_DAYS_AND_TIMES:
//	    if (currentSection != null) {
//		currentSection.addDayAndTime(s);
//		if (displayExtractedSections) {
//		    msg("......Added time " + s + " to section.") ;
//		}
//		newState = State.WAIT_FOR_SECTION_NUMBER1 ;
//	    } else {
//		daysAndTimes = s ;
//		newState = State.WAIT_FOR_LOCATION ;
//	    }
//	    break ;
//	case GET_LOCATION:
//	    location = s ;
//	    newState = State.WAIT_FOR_INSTRUCTOR ;
//	    break ;
//	case GET_INSTRUCTOR:
//	    //
//	    // If there are multiple instructors, the first instructor's name
//	    // will be followed by a comma.  The following line of code
//	    // replaces the comma with an ellipsis, and then we ignore any
//	    // additional instructors' names.
//	    //
//	    instructor = s.replace(",", "...") ;
//	    newState = State.WAIT_FOR_OPEN_STATUS ;
//	    break ;
//	default:
//	    throw new AssertionError(
//		    "Unexpected default case #2 for " + state) ;
//	}
//	textAccum.setLength(0); // Clear the accumulation buffer.
//	setState(newState);
//    }

//    private void setState(State newState) {
//	if (newState != state) {
//	    if (displayStateChanges)
//		msg(this + " State changed to " + newState);
//	    state = newState;
//	}
//    }

//    public Collection<Course> getCourses() {
//	return coursesCH;
//    }
    /*
     * 
     * enum ContentHandlerAction { attributeDecl, characters, comment,
     * elementDecl, endCDATA, endDTD, endDocument, endElement, endEntity,
     * endPrefixMapping, error, externalEntityDecl, fatalError,
     * getExternalSubset, ignorableWhitespace, internalEntityDecl, notationDecl,
     * processingInstruction, resolveEntity, setDocumentLocator, skippedEntity,
     * startCDATA, startDTD, startDocument, startElement, startEntity,
     * startPrefixMapping, unparsedEntityDecl, warning }
     */
    
    /*
     * javadocs for HttpClient are at 
     * http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/index.html
     * 
     * (for some version of the files).
     */
    
//    private String contentLocationValue() {
//	char commentChar = '#' ;
//	if (contentLocation == null) contentLocation = "" ;
//	int index = contentLocation.indexOf(commentChar) ;
//	if (index >= 0) {
//	    return contentLocation.substring(0, index) ;
//	}
//	return contentLocation ;
//    }
    
    class GetData extends Thread {
	/*
	 
	 To use this class, do:
	 
	 (new GetData()).start() ;
	 
	 may need to do:  (new SmartMeterTexasDataCollector.GetData()).start() ;
	 
	 */
	    private String extractAddress(WebPage wp) {
		if (wp == null) return "" ;
		return "" ;
	    }
	    
	    private void login() {
		List <NameValuePair> nameValuePairs = new ArrayList<>() ;

		getPage("https://www.smartmetertexas.com:443/CAP/public/") ; // 91
		
		nameValuePairs.add(new NameValuePair("pass_dup", "")) ;
		nameValuePairs.add(new NameValuePair("username", "VAJ4088")) ;
		nameValuePairs.add(new NameValuePair("password", "bri2bri")) ;
		nameValuePairs.add(new NameValuePair("buttonName", "")) ;
		nameValuePairs.add(new NameValuePair("login-form-type", "pwd")) ;
		WebPage wp =
		getPage("https://www.smartmetertexas.com:443/pkmslogin.form",
			nameValuePairs,
			null) ; // 114 POST- sets some cookies and 
		                // leads to 115 automatically.
		/*
		 * from
		 * 00 - WebScarab 20180808 myexpressenergy_com Login
		 * 
		 * Uses these messages:
		 *  91 GET - may not be needed, but sets some cookies.
		 * 114 POST- sets some cookies and leads to 115 automatically.
		 *  Page data is
	  pass_dup=&username=VAJ4088&password=bri2bri&buttonName=&login-form-type=pwd
		 *  Response is
		 *  302 Moved Temporarily
		 * 115 GET - sets some cookies and probably leads to 116 automatically.
		 *  Response is
		 *  302 Found
		 * 116 GET - sets some cookies. 
		 */
		addressSuffix = extractAddress(wp) ;
	    }
	    
	    private void getData() {
		List<NameValuePair> nameValuePairs = new ArrayList<>();

		/*
		 * from 00 - WebScarab 20180808 myexpressenergy_com Get Data
		 * 
		 * Uses these messages: 164 POST- address from ? 
		 * Response contains some
		 * data. Page data is
		 * _bowStEvent=Usage%2Fportlet%2FUsageCustomerMetersPortlet%21fireEvent%
		 * 3AForm%3AViewUsagePage_SaveDataSubmitEvent&tag_UserLocale=en&
		 * reportType=DAILY&viewUsage_startDate=08%2F06%2F2018&viewUsage_endDate
		 * =08%2F06%2F2018&
		 * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e
		 * =&
		 * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e1
		 * =&
		 * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e2
		 * =&
		 * _bst_locator_Usage_00215portlet_00215UsageCustomerMetersPortlet_00515ResidentialC_00515Default_00515Default_00515Default_00515Esiid_005151651b3535a4_00515b6e7e2=
		 * 170 POST Response contains the data! Page Data is
		 * _bowStEvent=Usage%2Fportlet%2FUsageCustomerMetersPortlet%21fireEvent%
		 * 3AForm%3AViewUsagePage_SaveDataSubmitEvent&tag_UserLocale=en&
		 * reportType=DAILY&viewUsage_startDate=08%2F01%2F2018&viewUsage_endDate
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
		nameValuePairs.add(
			new NameValuePair("viewUsage_startDate", "08%2F01%2F2018"));
		nameValuePairs
			.add(new NameValuePair("viewUsage_endDate", "08%2F03%2F2018"));
		nameValuePairs.add(new NameValuePair("_bst_locator_Usage_00215portlet"
			+ "_00215UsageCustomerMetersPortlet_00515ResidentialC"
			+ "_00515Default_00515Default_00515Default"
			+ "_00515Esiid_005151651b3535a4_00515b6e7e", ""));
		nameValuePairs.add(new NameValuePair("_bst_locator_Usage_00215portlet"
			+ "_00215UsageCustomerMetersPortlet_00515ResidentialC"
			+ "_00515Default_00515Default_00515Default"
			+ "_00515Esiid_005151651b3535a4_00515b6e7e1", ""));
		nameValuePairs.add(new NameValuePair("_bst_locator_Usage_00215portlet"
			+ "_00215UsageCustomerMetersPortlet_00515ResidentialC"
			+ "_00515Default_00515Default_00515Default"
			+ "_00515Esiid_005151651b3535a4_00515b6e7e2", ""));
		WebPage wp = 
			getPage("https://www.smartmetertexas.com:443" + addressSuffix, 
			nameValuePairs, null) ;
		/*
		 * NOW :  GET THE DATA !!!
		 * ALSO:  GET THE NEW addressSuffix !!!
		 */
		extractAddress(wp) ;
	    }
	    
	    private void logout() {
		//
		// Conversation 173 GET - sets some cookies.
		//
		getPage("https://www.smartmetertexas.com:443" + addressSuffix) ;
		//
		// Conversation 174 GET - sets some cookies.
		//
		getPage("https://www.smartmetertexas.com:443/pkmslogout?" +
		"filename=SMTLogout.html&type=public&lang=en") ;
		//
		// Conversation 175 GET - sets some cookies.
		//
		getPage("https://www.smartmetertexas.com:443/CAP/public") ;
		// Response is
		// 301 Moved Permanently, which automatically causes 176.
	    }
	    
	    @Override
	    public void run() {
		login() ;
		getData() ;
		logout() ;
	    }
    }
}
