package webPage;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A <tt>WebPage</tt> represents the source text of a web page accessed by using
 * the HTTP protocol.
 * <p>
 * Once constructed, a <tt>WebPage</tt> cannot be changed.
 * <p>
 * Most of the methods are concerned with retrieving substrings of the WebPage,
 * or finding the location of substrings. Note that when a search for a
 * substring takes place, the search takes place line by line. Searches will not
 * find substrings that wrap from one line to the next.
 * <p>
 * </p>
 * NOTE: A <tt>WebPage</tt> is a <b>static</b> text page. It will
 * <b><u>NOT</u></b>:
 * <ul>
 * <li>execute Javascript or Java code.
 * <li>refresh.
 * <li>load images or other references.
 * <li>respond to requests for passwords or other authentication.
 * </ul>
 * It is strictly the source text of the page as received from the server.
 * 
 * @author Ian Shef
 * @version 1.4 6 May 2011 - Added method size().
 * @version 1.3 30 November 2007 - Added getting hidden input form fields. -
 *          Added use of generics.
 * @version 1.2 31 December 2006
 * @version 1.1 26 November 2006 version 1.0 28 January 2003
 * @see WPLocation
 * @see chargeNumberXRef
 * @since 1.0
 * 
 */
public class WebPage {

    private static final String FORM = "<form ";
    private static final String FORM_END = "</form>";
    private static final String INPUT = "<input ";
    private static final String HIDDEN1 = " type='hidden' ";
    private static final String HIDDEN2 = " type=\"hidden\" ";
    private static final String NAME1 = " name='";
    private static final String NAME2 = " name=\"";
    private static final String VALUE1 = " value='";
    private static final String VALUE2 = " value=\"";
    private static final String CLOSE1 = "'";
    private static final String CLOSE2 = "\"";
    private static final String WINDOWVARIABLE = ".value=";
    private static final String WINDOWVARIABLESTART = ".";
    private static final String WINDOWVARIABLEEND = ";";
    private static final String TWOSINGLEQUOTES = "''";
    private static final String TWODOUBLEQUOTES = "\"\"";

    private List<String> lines = new ArrayList<String>();

    /**
     * No-argument constructor creates an empty WebPage, useful because lines
     * can be appended to it using the appendLine method. This allows the use of
     * the powerful search capabilities of a WebPage on any arbitrary collection
     * of text.
     * 
     * @since 1.2
     */

    public WebPage() { // No-arg constructor has no work to do.
    }

    /**
     * Constructor for getting a <tt>WebPage</tt> (using the HTML <b>GET</b>
     * function). The single parameter must be a properly formed URL, including
     * the <b>http://</b> prefix. Any parameters needed for the <b>GET</b> must
     * also be included by suffixing a question mark "?" and the parameters in
     * the correct format.
     * 
     * @param inputURL
     *            A String containing the properly formed URL. No encoding or
     *            translation is performed on this parameter.
     * 
     * @exception Exception
     *                There are many types of specific Exception that can occur.
     *                This includes (but may not be limited to):
     *                <ul>
     *                <li>MalformedURLException</li>
     *                <li>IOException</li>
     *                <li>IllegalStateException</li>
     *                <li>UnknownServiceException</li>
     *                </ul>
     * 
     */

    public WebPage(String inputURL) throws Exception {
	this(inputURL, null, false);
    }

    /**
     * Constructor for getting a <tt>WebPage</tt> (using the HTML <b>GET</b> or
     * <b>POST</b> functions). The first parameter must be a properly formed
     * URL, including the <b>http://</b> prefix. Any parameters needed for the
     * <b>GET</b> must also be included by suffixing a question mark "?" and the
     * parameters in the correct format.
     * 
     * @param inputURL
     *            A <tt>String</tt> containing the properly formed URL. Must
     *            include the <b>http://</b> prefix. Any parameters needed for a
     *            <b>GET</b> must also be included by suffixing a question mark
     *            "?" and the parameters in the correct format and properly
     *            translated.
     * @param inputPostString
     *            A String containing the message for a <b>POST</b> function. If
     *            this parameter is <tt>null</tt>, then a <b>GET</b> will be
     *            performed. If this parameter is an empty string, then a
     *            <b>POST</b> will be performed with the empty string.
     * 
     * @exception Exception
     *                There are many types of specific Exception that can occur.
     *                This includes (but may not be limited to):
     *                <ul>
     *                <li>MalformedURLException</li>
     *                <li>IOException</li>
     *                <li>ProtocolException (for <tt>POST</tt>)</li>
     *                <li>IllegalStateException</li>
     *                <li>UnknownServiceException</li>
     *                <li>java.io.UnsupportedEncodingException (for
     *                <tt>POST</tt>)</li>
     *                </ul>
     * 
     */
    public WebPage(String inputURL, String inputPostString) throws Exception {
	this(inputURL, inputPostString, false);
    }

    /**
     * Constructor for getting a <tt>WebPage</tt> (using the HTML <b>GET</b> or
     * <b>POST</b> functions). The first parameter must be a properly formed
     * URL, including the <b>http://</b> prefix. Any parameters needed for the
     * <b>GET</b> must also be included by suffixing a question mark "?" and the
     * parameters in the correct format.
     * 
     * @param inputURL
     *            A <tt>String</tt> containing the properly formed URL. Must
     *            include the <b>http://</b> prefix. Any parameters needed for a
     *            <b>GET</b> must also be included by suffixing a question mark
     *            "?" and the parameters in the correct format.
     * @param inputPostString
     *            A String containing the message for a <b>POST</b> function. If
     *            this parameter is <tt>null</tt>, then a <b>GET</b> will be
     *            performed. If this parameter is an empty string, then a
     *            <b>POST</b> will be performed with the empty string.
     * @param translateGet
     *            A boolean that determines whether translation will be
     *            performed on the inputURL. If <tt>false</tt>, no translation
     *            will be performed and the string must already be properly
     *            translated. If <tt>true</tt>, then:
     *            <ul>
     *            <li>All characters up through the first question mark are left
     *            as is.</li>
     *            <li>All characters following the first question mark (except
     *            <tt>equals sign</tt> and <tt>ampersand</tt>) are translated by
     *            <tt>URLEncoder.encode</tt> using <b>UTF-8</b>.</li>
     *            </ul>
     * 
     * @exception Exception
     *                There are many types of specific Exception that can occur.
     *                This includes (but may not be limited to):
     *                <ul>
     *                <li>MalformedURLException</li>
     *                <li>IOException</li>
     *                <li>ProtocolException (for <tt>POST</tt>)</li>
     *                <li>IllegalStateException</li>
     *                <li>UnknownServiceException</li>
     *                <li>java.io.UnsupportedEncodingException (when encoding
     *                takes place for <tt>POST</tt> or optionally for
     *                <tt>GET</tt>).</li>
     *                </ul>
     * 
     */
    public WebPage(String inputURL, String inputPostString, boolean translateGet)
	    throws Exception {
	//
	// "throws Exception" is ugly, but there are so many Exceptions
	// that could occur! Thus, this shortcut is used.
	//
	URL url;
	if (translateGet) {
	    url = new URL(translateGet(inputURL));
	    // throws MalformedURLException
	} else {
	    url = new URL(inputURL);
	    // throws MalformedURLException
	}
	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	// throws IOException
	if (null != inputPostString) {
	    connection.setRequestMethod("POST");
	    // throws ProtocolException
	    connection.setDoOutput(true);
	    // throws IllegalStateException
	    // (a java.lang.RuntimeException; no need to declare?)
	    PrintWriter out = new PrintWriter(connection.getOutputStream());
	    // getOutputStream throws IOException
	    // getOutputStream throws UnknownServiceException,
	    // which extends IOException
	    //
	    // Send the POST message - must be encoded by
	    // URLEncoder.encode using UTF-8,
	    // BUT do NOT encode "=" signs and "&" (equal signs and ampersands)!
	    // translatePost will perform the required translation.
	    //
	    out.println(translatePost(inputPostString));
	    // translatePost throws java.io.UnsupportedEncodingException
	    out.close();
	}
	BufferedReader in = new BufferedReader(new InputStreamReader(
		connection.getInputStream()));
	String line;
	while ((line = in.readLine()) != null) {
	    this.lines.add(line);
	}
	in.close();
    }

    /**
     * Constructor for getting a <tt>WebPage</tt> from a character file using
     * the default character encoding.
     * 
     * @param inFile
     *            A File specifying an existing readable file.
     * 
     * @exception Exception
     *                There are many types of specific Exception that can occur.
     *                This includes (but may not be limited to):
     *                <ul>
     *                <li>FileNotFoundException</li>
     *                <li>IOException</li>
     *                </ul>
     * 
     * @since 1.1
     * 
     */

    public WebPage(File inFile) throws Exception {
	//
	// "throws Exception" is ugly, but there are so many Exceptions
	// that could occur! Thus, this shortcut is used.
	//
	BufferedReader in = new BufferedReader(new FileReader(inFile));
	String line;
	while ((line = in.readLine()) != null) {
	    this.lines.add(line);
	}
	in.close();
    }

    private String translatePost(String input)
	    throws java.io.UnsupportedEncodingException {
	int inLength = input.length();
	StringBuffer s = new StringBuffer(inLength);
	char c;
	for (int i = 0; i < inLength; i++) {
	    c = input.charAt(i);
	    if (('=' == c) || ('&' == c)) {
		s.append(c);
	    } else {
		s.append(URLEncoder.encode(Character.toString(c), "UTF-8"));
	    }
	}
	return s.toString();
    }

    private String translateGet(String input)
	    throws java.io.UnsupportedEncodingException {
	int inLength = input.length();
	StringBuffer s = new StringBuffer(inLength);
	char c;
	paramSearch: for (int i = 0; i < inLength; i++) {
	    c = input.charAt(i);
	    s.append(c);
	    if ('?' == c) {
		for (int j = i++; j < inLength; j++) {
		    c = input.charAt(j);
		    if (('=' == c) || ('&' == c)) {
			s.append(c);
		    } else {
			s.append(URLEncoder.encode(Character.toString(c),
				"UTF-8"));
		    }
		}
		break paramSearch;
	    } // end of "paramSearch loop.
	}
	// Continue here from "break paramSearch".
	return s.toString();
    }

    /**
     * Returns all of the source text of the web page as a <tt>List</tt>, with
     * each element of the <tt>List</tt> containing a line of the web page.
     * <p>
     * The elements of the <tt>List</tt> will be in the same order as the
     * lines of the web page. The elements are of type <tt>String</tt>.
     * 
     * @return A <tt>List</tt> (currently implemented as an ArrayList but not
     *         guaranteed to remain implemented this way) containing the lines
     *         of source text of the web page in order.
     */
    public List<String> getLines() {
	return new ArrayList<String>(this.lines);
    }

    /**
     * Finds a given string, starting from a given line and searching each line
     * starting from a given column.
     * 
     * @param str
     *            The <tt>String</tt> to search for.
     * @param fromLineIndex
     *            An <tt>int</tt> indicating the line on which to start the
     *            search. This index is zero-based.
     * @param fromColumnIndex
     *            An <tt>int</tt> indicating the column at which to start the
     *            search. This index is zero-based. This column is the starting
     *            point on every line that is searched.
     * 
     * @return A WPLocation representing the line and column where the first
     *         occurrence of <tt>str</tt> was found. If not found, the returned
     *         WPLocation will have invalid line and column values (both will be
     *         -1).
     * 
     */
    public WPLocation indexOf(String str, int fromLineIndex, int fromColumnIndex) {
	int size = this.lines.size();
	String candidate;
	int col;
	for (int i = fromLineIndex; i < size; i++) {
	    candidate = this.lines.get(i);
	    col = candidate.indexOf(str, fromColumnIndex);
	    if (col >= 0) {
		return new WPLocation(i, col); // Success, found it.
	    }
	}
	return new WPLocation(-1, -1); // Failure, return invalid WPLocation.
    }

    /**
     * Finds a given string, starting from the beginning of the web page.
     * 
     * @param str
     *            The <tt>String</tt> to search for.
     * 
     * @return A WPLocation representing the line and column where the first
     *         occurrence of <tt>str</tt> was found. If not found, the returned
     *         <tt>WPLocation</tt> will have invalid line and column values
     *         (both will be -1).
     * 
     */
    public WPLocation indexOf(String str) {
	return indexOf(str, 0, 0);
    }

    /**
     * Finds a given string, starting from a given line.
     * 
     * @param str
     *            The <tt>String</tt> to search for.
     * @param fromLineIndex
     *            An <tt>int</tt> indicating the line on which to start the
     *            search. This index is zero-based.
     * 
     * @return A WPLocation representing the line and column where the first
     *         occurrence of <tt>str</tt> was found. If not found, the returned
     *         <tt>WPLocation</tt> will have invalid line and column values
     *         (both will be -1).
     * 
     */
    public WPLocation indexOf(String str, int fromLineIndex) {
	return indexOf(str, fromLineIndex, 0);
    }

    /**
     * Provides a specific line by its line number.
     * 
     * @param index
     *            An <tt>int</tt> indicating the desired line. This index is
     *            zero-based.
     * 
     * @return A <tt>String</tt> that is the requested line.
     * 
     */
    public String line(int index) {
	return this.lines.get(index);
    }

    /**
     * Provides a specific line by the line number of a <tt>WPLocation</tt>.
     * 
     * @param loc
     *            A <tt>WPLocation</tt> indicating the desired line (the column
     *            value is ignored).
     * 
     * @return A <tt>String</tt> that is the requested line.
     * 
     */
    public String line(WPLocation loc) {
	return this.lines.get(loc.getLine());
    }

    /**
     * Provides a specific substring of a line by the line number and the column
     * number of a <tt>WPLocation</tt>.
     * 
     * @param loc
     *            A <tt>WPLocation</tt> indicating the desired line and column
     *            value.
     * 
     * @return A <tt>String</tt> that is a substring of the requested line,
     *         starting at the requested column.
     * 
     */
    public String substring(WPLocation loc) {
	return this.lines.get(loc.getLine()).substring(loc.getColumn());
    }

    /**
     * Provides a <tt>String</tt> representation of a <tt>WebPage</tt>,
     * providing the size of the <tt>ArrayList</tt> representation of its
     * contents.
     * 
     * @return A <tt>String</tt> representation of the <tt>WebPage</tt>.
     * 
     */
    @Override
    public String toString() {
	return ("WebPage of " + this.lines.size() + " lines");
    }

    /**
     * A powerful method that provides a specific substring of a line by its
     * <tt>WPLocation</tt> and by <tt>String</tt>s that prefix and suffix the
     * desired substring.
     * 
     * @param loc
     *            A <tt>WPLocation</tt> indicating the desired line and starting
     *            column value.
     * @param fromString
     *            A <tt>String</tt> to be found on the line specified by
     *            <tt>loc</tt>, with the search starting at the column specified
     *            by <tt>loc</tt>.
     * @param toString
     *            A <tt>String</tt> to be found on the line specified by
     *            <tt>loc</tt>, with the search starting at the column
     *            immediately after the last column matching <tt>fromString</tt>
     *            .
     * 
     * @return A <tt>String</tt> that is a substring of the requested line
     *         between <tt>fromString</tt> and <tt>toString</tt>.
     * 
     */
    public String subString(WPLocation loc, String fromString, String toString) {
	return subString(loc.getLine(), loc.getColumn(), fromString, toString);
    }

    /**
     * A powerful method that provides a specific substring of a line by the
     * line number and by <tt>String</tt>s that prefix and suffix the desired
     * substring.
     * 
     * @param lineIndex
     *            An <tt>int</tt> indicating the desired line.
     * @param fromString
     *            A <tt>String</tt> to be found on the line specified by
     *            <tt>lineIndex</tt>, with the search starting at the first
     *            character of the line specified by <tt>lineIndex</tt>.
     * @param toString
     *            A <tt>String</tt> to be found on the line specified by
     *            <tt>lineIndex</tt>, with the search starting at the column
     *            immediately after the last column matching <tt>fromString</tt>
     *            .
     * 
     * @return A <tt>String</tt> that is a substring of the requested line
     *         between <tt>fromString</tt> and <tt>toString</tt>.
     * 
     */
    public String subString(int lineIndex, String fromString, String toString) {
	String testLine = this.lines.get(lineIndex);
	int sstart = testLine.indexOf(fromString) + fromString.length();
	int send = testLine.indexOf(toString, sstart);
	return (testLine.substring(sstart, send));
    }

    /**
     * A powerful method that provides a specific substring of a line by the
     * line number and by <tt>String</tt>s that prefix and suffix the desired
     * substring.
     * 
     * @param lineIndex
     *            An <tt>int</tt> indicating the desired line.
     * @param columnIndex
     *            An <tt>int</tt> indicating the desired starting column value.
     * @param fromString
     *            A <tt>String</tt> to be found on the line specified by
     *            <tt>llineIndex</tt>, with the search starting at the column
     *            specified by <tt>columnIndex</tt>.
     * @param toString
     *            A <tt>String</tt> to be found on the line specified by
     *            <tt>lineIndex</tt>, with the search starting at the column
     *            immediately after the last column matching <tt>fromString</tt>
     *            .
     * 
     * @return A <tt>String</tt> that is a substring of the requested line
     *         between <tt>fromString</tt> and <tt>toString</tt>.
     * 
     */
    public String subString(int lineIndex, int columnIndex, String fromString,
	    String toString) {
	String testLine = this.lines.get(lineIndex);
	int sstart = testLine.indexOf(fromString, columnIndex)
		+ fromString.length();
	int send = testLine.indexOf(toString, sstart);
	return (testLine.substring(sstart, send));
    }

    /**
     * A method that allows a line of text (a <tt>String</tt>) to be appended to
     * the WebPage.
     * 
     * @param line
     *            A <tt>String</tt> to be appended to the WebPage.
     * 
     * @return The same <tt>String</tt> that was provided as an input parameter.
     * 
     * @since 1.2
     * 
     */
    public String appendLine(String line) {
	this.lines.add(line);
	return line;
    }

    /**
     * A method that returns hidden input fields of a form in the WebPage.
     * IMPLEMENTATION NOTE: The current implementation is simple: - There must
     * not be more than one form in the WebPage. - The HTML form directive must
     * look like &ltform followed by a space. - An HTML form directive preceded
     * by document.write(" is ignored. [Following line item added 31 Dec 2010 by
     * Ian Shef (ibs)] - An HTML form directive preceded by iframeDoc.write(" is
     * ignored. - The HTML input directive must look like &ltinput followed by a
     * space. - The HTML input directive must not extend beyond a line boundary.
     * - No more than one HTML input directive with a hidden input field per
     * line. - No parsing is performed to unravel cases where what looks like a
     * directive is actually contained within a comment or within text.
     * 
     * @return A <tt>Map</tt> where each Key is the name of a hidden input
     *         field, and each Value is the value of the hidden input field.
     * 
     * @since 1.3
     * 
     */
    public Map<String, String> getHiddenFields() {
	int current = 0; // Number of line under consideration.
	int start = 0; // Number of line of start of form.
	int end = 0; // Number of line of end of form.
	int formsCount = 0; // Number of start of forms found.
	int formsEndCount = 0; // Number of end of forms found.
	Map<String, String> hiddenInputFields;

	// Check preconditions...
	// Check number of forms, and locate the start.
	while (current >= 0) {
	    WPLocation loc = this.indexOf(FORM, current);
	    current = loc.getLine();
	    if (current < 0)
		break; // No more to find.
	    int startChar = loc.getColumn();
	    int docWriteLoc = this.line(current).indexOf("document.write(\"");
	    int docWrite2Loc = this.line(current).indexOf("iframeDoc.write(\"");
	    if ((current >= 0)
		    && ((docWriteLoc < 0) || (docWriteLoc > startChar))
		    // Following two lines added 31 Dec 2010 by Ian Shef (ibs).
		    && ((docWrite2Loc < 0) || (docWrite2Loc > startChar))) {
		formsCount++;
		start = current++; // Found acceptable start of form.
				   // Record its line number, and set up to
				   // continue the search.
	    } else {
		current++; // Not acceptable start of form, so
			   // set up to continue the search.
	    }
	}
	if (formsCount > 1) {
	    throw new UnacceptableFormsException("getHiddenFields found "
		    + formsCount + " forms in " + toString());
	}

	// Check number of ends of forms, and locate the end.
	current = start;
	while (current >= 0) {
	    current = this.indexOf(FORM_END, current).getLine();
	    // Following region updated 31 Dec 2010 by Ian Shef (ibs).
	    if (current >= 0) {
		if ((this.line(current).indexOf("document.write(\"") < 0)
			&& (this.line(current).indexOf("iframeDoc.write(\"") < 0)) {
		    formsEndCount++;
		    end = current;
		}
		current++;
	    }
	}
	// End of region updated 31 Dec 2010 by Ian Shef (ibs).

	if (formsEndCount != formsCount) {
	    throw new UnacceptableFormsException("getHiddenFields found "
		    + formsEndCount + " end_of_forms in " + toString()
		    + ", needed " + formsCount);
	}

	// Check that end of form follows start of form.
	if ((formsCount > 0) && (start > end)) {
	    throw new UnacceptableFormsException("getHiddenFields found "
		    + " start of form at line " + start
		    + ", follows end of form at line " + end + " in "
		    + toString());
	}

	// Passed precondition checks, so get the hidden input fields.
	hiddenInputFields = new LinkedHashMap<String, String>();
	current = start;
	while ((formsCount > 0) && (current >= 0) && (current <= end)) {
	    WPLocation loc = indexOf(INPUT, current);
	    current = loc.getLine();
	    // If the input field was found beyond end or not found, then quit.
	    if ((current > end) || (current < 0))
		break;

	    String text = substring(loc);
	    // If the input field is not a hidden field, continue to the next
	    // one.
	    if (text.contains(HIDDEN1)) {
		// Here if we have a hidden input field within the only form.
		// Get the name and the corresponding value.
		String name;
		String value;
		name = subString(loc, NAME1, CLOSE1);
		value = subString(loc, VALUE1, CLOSE1);
		hiddenInputFields.put(name, value);
	    } else if (text.contains(HIDDEN2)) {
		// Here if we have a hidden input field within the only form.
		// Get the name and the corresponding value.
		String name;
		String value;
		name = subString(loc, NAME2, CLOSE2);
		value = subString(loc, VALUE2, CLOSE2);
		hiddenInputFields.put(name, value);
	    }
	    current++;
	}
	hiddenInputFields.putAll(getWindowVariables());
	// System.out.println("getHiddenFields found " + hiddenInputFields) ;//
	// debug
	return hiddenInputFields;
    }

    /**
     * A method that returns updated window variables in the WebPage.
     * IMPLEMENTATION NOTE: The current implementation is simple: - No more than
     * one HTML window variable per line. - No parsing is performed to unravel
     * cases where what looks like a directive is actually contained within a
     * comment or within text.
     * 
     * @return A <tt>Map</tt> where each Key is the name of a window variable,
     *         and each Value is the value of the variable.
     * 
     * @since 1.3
     * 
     */
    /*
     * 
     * private static final String WINDOWVARIABLE = ".value=" ; 
     * private static final String WINDOWVARIABLESTART = "." ; 
     * private static final String WINDOWVARIABLEEND = ";" ; 
     * private static final String TWOSINGLEQUOTES = "''" ; 
     * private static final String TWODOUBLEQUOTES = "\"\"" ;
     */
    private Map<String, String> getWindowVariables() {
	int current = 0; // Number of line under consideration.
	int start = 0; // Number of column of start of variable name.
	int end = 0; // Number of column of end of variable name.
	Map<String, String> hiddenInputFields = 
	    new LinkedHashMap<String, String>();

	while (true) {
	    WPLocation loc = this.indexOf(WINDOWVARIABLE, current);
	    current = loc.getLine();
	    if (current < 0 ) break ;
	    end = loc.getColumn() ;
	    start = this.line(loc).lastIndexOf(WINDOWVARIABLESTART, end - 1) ;
	    start++ ;
	    String variableName = this.line(loc).substring(start, end ) ;
	    String variableValue = this.subString(current, WINDOWVARIABLE, WINDOWVARIABLEEND) ;
	    if ( variableValue.equals(TWOSINGLEQUOTES) ||
		    variableValue.equals(TWODOUBLEQUOTES)) {
		variableValue = "" ;
	    }
	    hiddenInputFields.put(variableName, variableValue) ;
	    current++ ;
	}
	return hiddenInputFields;
    }

    /**
     * @return The number of lines in this WebPage. If this WebPage contains
     *         more than Integer.MAX_VALUE lines, returns Integer.MAX_VALUE.
     * 
     * @since 1.4
     */
    public int size() {
	return lines.size();
    }
}
