package eup;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

import eup.Util;
import webPage.WPLocation;
import webPage.WebPage;

/**
 * A FieldManager manages the fields found in a WebPage. A field is an
 * input element of a web page, such as places where a value is submitted
 * to a form.  A FieldManager is not concerned with the value of a field,
 * only with the name of a field.
 * A FieldManager only manages fields found between each occurrence of the
 * begin String and the end String with which it is constructed.
 * 
 * Some typical values for String begin and String end are:
 * 
 *     begin = "<select name='";
 *     end = "' ";
 *     
 *     begin = "<input type='text' name='";
 *     end = "' ";
 *     
 *     begin = "<a name='";
 *     end = "'";
 *     
 *     A FieldManager cannot be constructed directly.  A FieldManager is 
 *     returned by using the static method FieldManager.makeFieldManager(...).
 *     
 *     The key methods are:
 *     
 *     addFields(WebPage) - This method is used to process a WebPage, looking
 *                          for names contained between String begin and 
 *                          String end and storing them for future usage.
 *                          
 *     getFieldGte(String) - This method returns a name (fields) known by
 *                           this FieldManager that is the same as the supplied
 *                           String parameter, or (if there is no exact match),
 *                           the next one greater (in an alphabetic sense) that
 *                           starts with the supplied String parameter.
 *                           Otherwise, null is returned.
 *                           
 *     getFieldGteOrAssert(String) - Same as getFieldGte(String) except that
 *                                   if the result would be null, an
 *                                   AssertionError is thrown first.  This is
 *                                   a useful debugging tool and often makes 
 *                                   this method more useful than 
 *                                   getFieldGte(String).
 *                                   
 *     Frequently, one is dealing with multiple FieldManager objects.  This is
 *     because fields show up in multiple contexts within web pages, with
 *     each context needing its own String begin and String end.
 *     
 *     Thus there is a static method to create a collection of
 *     FieldManager objects, called makeFmCollection(...) which takes
 *     zero or more parameters of type FieldManager.
 *     
 *     Also, each of the key methods above comes as a static method suitable
 *     for handling a collection of FieldManager objects:
 *     
 *     addFields(WebPage, Collection<FieldManager>)
 *     getFieldGte(String, Collection<FieldManager>)
 *     getFieldGteOrAssert(Sring, Collection<FieldManager>)
 *     
 * 
 * @author Ian Shef
 * @version 1.0
 * @since   1.0
 *
 */
class FieldManager {

    private String begin;
    private String end;

    private java.util.NavigableSet<String> fields = Util.makeTreeSet();

    private static final boolean debugFieldManager = false;

    private FieldManager() {/* No aceessible no-arg constructor. */
    }

    // No accessible constructors. Use static factory method.
    private FieldManager(String begin, String end) {
	this.begin = begin;
	this.end = end;
    }

    public static FieldManager makeFieldManager(String begin, String end) {
	FieldManager fm = new FieldManager(begin, end);
	msgDebug("Constructed " + fm.toString());
	return fm;
    }

    public static Collection<FieldManager> makeFmCollection(
	    FieldManager... fieldManagers) {
	Collection<FieldManager> result = Util
		.makeArrayList(fieldManagers.length);
	Collections.addAll(result, fieldManagers);
	return result;
    }
    
    public void addFields(WebPage wp) {
	String name = "";
	if (debugFieldManager) {
	    name = storeDetailedMessage(
		    "FieldManager#addFields on " + wp.toString(), wp);
	}
	msgDebug("FieldManager#addFields on " + wp.toString() + ", details in "
		+ name);
	int fromLineIndex = 0;
	while (fromLineIndex >= 0) {
	    WPLocation wpl = wp.indexOf(begin, fromLineIndex);
	    fromLineIndex = wpl.getLine();
	    if (fromLineIndex >= 0) {
		fromLineIndex++;
		addFieldsHelper1(wp, wpl);
	    }
	}
    }

    /**
     * @param wp
     * @param wpl
     */
    private void addFieldsHelper1(WebPage wp, WPLocation wpl) {
	try {
	    String s = wp.subString(wpl, begin, end);
	    boolean added = fields.add(s);
	    if (added) {
		msgDebug("Added " + s + " to " + toString());
	    } else {
		msgDebug("Could not add " + s + " to " + toString());
	    }
	} catch (StringIndexOutOfBoundsException e) {
	    e.printStackTrace();
	}
    }

    public String getFieldGte(String request) {
	String result = fields.ceiling(request);
	if ((result != null) && (!result.startsWith(request)))
	    result = null;
	msgDebug(
		"getFieldGTE requested " + request + " and returned " + result);
	return result;
    }

    public String getFieldGteOrAssert(String request) {
	String result = getFieldGte(request);
	if (result == null)
	    throw new AssertionError(Util.getCallerMethodName()
		    + "could not find " + request);
	return result;
    }

    private static void msgDebug(Object ob) {
	if (debugFieldManager) {
	    Util.msg(ob);
	}
    }

    public static String getFieldGte(String request,
	    Collection<FieldManager> fmCollection) {
	java.util.NavigableSet<String> all = Util.makeTreeSet();
	String result = null;
	for (FieldManager fm : fmCollection) {
	    all.addAll(fm.fields);
	}
	result = all.ceiling(request);
	if ((result != null) && (!result.startsWith(request)))
	    result = null;
	msgDebug("static method getFieldGTE requested " + request
		+ " and returned " + result);
	return result;
    }

    /**
     * @param request
     * @param fmCollection
     * @return
     */
    public static String getFieldGteOrAssert(String request,
	    Collection<FieldManager> fmCollection) {
	String result = getFieldGte(request, fmCollection);
	if (result == null)
	    throw new AssertionError(Util.getCallerMethodName()
		    + " could not find " + request + " in " + fmCollection);
	return result;
    }
    
    /**
     * @param wp
     * @param fmCollection
     * @return
     */
    public static void addFields(WebPage wp,
	    Collection<FieldManager> fmCollection) {
	for (FieldManager fm : fmCollection) {
	    fm.addFields(wp);
	}
    }
 

    /**
     * @param e
     * @param wp
     * @return
     */
    private String storeDetailedMessage(String message, WebPage wp) {
	Calendar now = Calendar.getInstance();
	long dateTime = now.get(Calendar.YEAR);
	dateTime = (dateTime * 100) + now.get(Calendar.MONTH) + 1;
	dateTime = (dateTime * 100) + now.get(Calendar.DATE);
	dateTime = (dateTime * 100) + now.get(Calendar.HOUR_OF_DAY);
	dateTime = (dateTime * 100) + now.get(Calendar.MINUTE);
	dateTime = (dateTime * 100) + now.get(Calendar.SECOND);
	String name = this.getClass().getSimpleName() + "_log_"
		+ Long.toString(dateTime) + ".txt";
	// ==========================
	// O P E N
	FileOutputStream fos = null;
	try {
	    fos = new FileOutputStream(name, false /* append */);
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	}
	final BufferedOutputStream bos = new BufferedOutputStream(fos, 16384);
	/* 16384 = 16K bytes, 25% of allocation is optimal */
	final OutputStreamWriter osw = new OutputStreamWriter(bos);
	final BufferedWriter bw = new BufferedWriter(osw, 24576);
	/* 24576 = 24K chars (48K bytes), 75% of allocation is optimal */
	final PrintWriter detail = new PrintWriter(bw, false);
	/* false = don't auto flush on println */

	// W R I T E

	detail.println(message);
	detail.println("==== Due to this web page content ====");
	for (String line : wp.getLines()) {
	    detail.println(line);
	}
	detail.println();
	detail.println("====   End   of  web page content ====");
	detail.println();

	// C L O S E
	detail.close();

	return name;
    }

    @Override
    public String toString() {
	StringBuilder s = new StringBuilder();
	s.append(getClass().getName());
	s.append(" from ");
	s.append(begin);
	s.append(" to ");
	s.append(end);
	s.append(" has contents ");
	s.append(fields.toString());
	return s.toString();
    }
}