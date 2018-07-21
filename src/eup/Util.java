/**
 * 
 */
package eup;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * @author Ian Shef
 **/

/**
 * 
 * An alternative to the below is 
 * 
 * new Object() {}.getClass().getEnclosingClass()
 * 
 * but this would need to be placed in every class where it is used.
 * The problem is that this generates a different anonymous inner class
 * at each of these places, creating a lot of overhead in space for
 * each such class.  Also requires Java version 1.5 or later.
 * 
 *  The method below does not create so much overhead, but the
 *  shortcoming is that the documentation allows some virtual
 *  machines to not provide this information, leaving it null.
 *  Thus, some extra checks have to be made and there is a possibility
 *  that the information will not be available when needed.
 *
 */
public class Util {
    
    private static final StackTraceElement UNAVAILABLE = 
        new StackTraceElement("unavailable", "unavailable", "unavailable", -1) ;
    
    public static String getCallerClassName() {
        StackTraceElement [] stArray = new Throwable().getStackTrace() ;
        StackTraceElement st ;
        if ((null!=stArray)&&(stArray.length>1)&&(null!=stArray[1])) {
            st = stArray[1] ;
        } else {
            st = UNAVAILABLE ;
        }
        return st.getClassName() ;
    }
    
    public static Class<?> getCallerClass() {
	StackTraceElement[] stArray = new Throwable().getStackTrace();
	StackTraceElement st;
	Class<?> result = Object.class;
	if ((null != stArray) && (stArray.length > 1) && (null != stArray[1])) {
	    st = stArray[1];
	    try {
		result = Class.forName(st.getClassName());
	    } catch (ClassNotFoundException e) {
		result = Object.class;
	    }
	}
	return result;
    }

    public static String getCallerMethodName() {
        StackTraceElement [] stArray = new Throwable().getStackTrace() ;
        StackTraceElement st ;
        if ((null!=stArray)&&(stArray.length>1)&&(null!=stArray[1])) {
            st = stArray[1] ;
        } else {
            st = UNAVAILABLE ;
        }
        return st.getMethodName() ;
    }
    
    public static String getCallerFileName() {
        StackTraceElement [] stArray = new Throwable().getStackTrace() ;
        StackTraceElement st ;
        if ((null!=stArray)&&(stArray.length>1)&&(null!=stArray[1])) {
            st = stArray[1] ;
        } else {
            st = UNAVAILABLE ;
        }
        return st.getFileName() ;
    }
    
    public static String getCallerLineNumber() {
        StackTraceElement [] stArray = new Throwable().getStackTrace() ;
        StackTraceElement st ;
        if ((null!=stArray)&&(stArray.length>1)&&(null!=stArray[1])) {
            st = stArray[1] ;
        } else {
            st = UNAVAILABLE ;
        }
        return String.valueOf(st.getLineNumber()) ;
    }
    
    public static String getCallerTraceInfo() {
        StackTraceElement [] stArray = new Throwable().getStackTrace() ;
        StackTraceElement st ;
        if ((null!=stArray)&&(stArray.length>1)&&(null!=stArray[1])) {
            st = stArray[1] ;
        } else {
            st = UNAVAILABLE ;
        }
        return st.toString() ;
    }
    
    /**
     * A convenience method for displaying a line of text on System.out.
     * 
     * @param ob  An <tt>Object</tt> or a <tt>String</tt> to be displayed
     *            on System.out.  If an <tt>Object</tt>, its toString()
     *            method will be called.
     */
    public static void msg(Object ob) {System.out.println(ob);} 
    
    /**
     * A convenience method for displaying a message on System.out regarding
     * whether the current Thread is the Event Dispatch Thread.  The message
     * will be preceeded by ob.toString(), a colon and a single space.
     * 
     * @param ob  An <tt>Object</tt> or a <tt>String</tt> to be displayed
     *            on System.out.  If an <tt>Object</tt>, its toString()
     *            method will be called.  This will be followed by a colon,
     *            a space, and a message describing the current Thread and
     *            whether it is the Event Dispatch Thread.
     */
    public static void msgEDT(Object ob) {
	msg(ob.toString() + ": " +
		"Current thread is " + Thread.currentThread() +
		    " which is " + 
		    (SwingUtilities.isEventDispatchThread()?"":"NOT ") +
		    "the EDT.") ;
    }

    /**
     * A method similar to SwingUtilities.getRoot(Component c), but finds
     * the root component for a JMenuItem.
     * 
     * @param m The JMenuItem whose root is desired
     * 
     * @return the first ancestor of m that's a Window or the 
     *         last Applet ancestor
     */
    static Component getMenuRoot(final JMenuItem m) {
	Component r = m ;
	while ((r != null) && (!(r instanceof JPopupMenu))) r = r.getParent() ;
	if (null == r) return null ;
	r = ((JPopupMenu)r).getInvoker() ;
	r = SwingUtilities.getRoot(r) ;
	return r ;
    }
    
    public static boolean even(int n) {
        return (0==(n%2)) ;
    }

    public static boolean even(long n) {
	return (0==(n%2)) ;
    }
    
    public static void sleep(int milliseconds) {
	try {
	    Thread.sleep(milliseconds);
	} catch (InterruptedException e) {
	    e.printStackTrace();
	    // Restore the interrupted status
	    Thread.currentThread().interrupt();
	}
    }

    public static <K,V> HashMap<K,V> makeHashMap() {
    	return new HashMap<K,V>() ;
    }
    
    public static <K,V> HashMap<K,V> makeHashMap(int initialCapacity) {
    	return new HashMap<K,V>(initialCapacity) ;
    }
    
    public static <K> HashSet<K> makeHashSet(int initialCapacity) {
    	return new HashSet<K>(initialCapacity) ;
    }
    
    public static <K> ArrayList<K> makeArrayList(int initialCapacity) {
    	return new ArrayList<K>(initialCapacity) ;
    }
    
    public static <K> ArrayList<K> makeArrayList(Collection <K> c) {
    	return new ArrayList<K>(c) ;
    }
    
    public static <K, V> LinkedHashMap<K, V> makeLinkedHashMap(
	    int initialCapacity) {
	return new LinkedHashMap<K, V>(initialCapacity);
    }
    
    public static <K, V> TreeMap<K, V> makeTreeMap() {
	return new TreeMap<K, V>() ;
    }

    public static <K, V> Map<K, V> makeConcurrentSkipListMap() {
	return new ConcurrentSkipListMap<K, V>() ;
    }

    public static <T extends JComponent> T setMaxToPreferred(T jc) {
        Dimension pref = jc.getPreferredSize() ;
        jc.setMaximumSize(new Dimension(pref.width, pref.height));
        return jc ;
    }
    
    public static <K, V> Map<K, V> makeTreeMapCopy(Map<K, V> original) {
	Map<K, V> copy = makeTreeMap() ;
	for( K key : original.keySet()) copy.put(key, original.get(key)) ;
	return copy ;
    }
    
    public static <E> CopyOnWriteArrayList<E> makeCopyOnWriteArrayList() {
	return new CopyOnWriteArrayList<E>();
    }

    public static <E> CopyOnWriteArrayList<E> makeCopyOnWriteArrayList(
	    Collection<? extends E> c) {
	return new CopyOnWriteArrayList<E>(c);
    }
    
    public static <E> TreeSet<E> makeTreeSet() { return new TreeSet<E>() ;}
    
    public static <K, V> Map<K, V> makeLinkedHashMap() {
	return new LinkedHashMap<K, V>() ;
    }

    /**
     * @param num
     * @return an empty String if num is 1, else a String consisting of the
     *         letter s.
     */
    public String plural(int num) {
	return ((1 == num) ? "" : "s");
    }

}
