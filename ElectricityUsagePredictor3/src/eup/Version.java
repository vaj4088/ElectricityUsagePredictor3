/**
 * 
 */
package eup;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Ian Shef
 * @param <T>
 * 
 */
public final class Version<T> implements Serializable, Comparable<T> {
    /**
     *  
     */
    private static final String VERSION = "Revised on"
	    + " Date: 2019-02-10"
	    + " at 1527"
	    + " via manual intervention.";
    private static final long serialVersionUID = 1L;
    private static final Version<String> INSTANCE = new Version<String>();
    private final String version ;

    private Version() {
	version = VERSION ;
    }

    public Version(String v) {
	version = v;
    }

    public static Version<String> getInstance() {
	return INSTANCE;
    }

    /**
     * @return the version
     */
    public String getVersion() {
	return version;
    }

    @Override
    public boolean equals(Object ob) {
	if (!(ob instanceof Version))
	    return false;
	Version<?> v = (Version<?>) ob;
	return version.equals(v.getVersion());
    }

    @Override
    public int hashCode() {
	return version.hashCode();
    }

    @Override
    public String toString() {
	return "Version " + version.replace("$", "");
    }

    @Override
    public int compareTo(T arg0) {
	Version<?> arg = (Version<?>) arg0;
	String s = arg.getVersion();
	return version.compareTo(s);
    }

    //
    // Cause serialization system to emit an instance of SerializationProxy
    // instead of an instance of Version.
    //
    private Object writeReplace() {
	return new SerializationProxy(this);
    }

    //
    // Prevent attacker from fabricating an instance of Version and
    // inserting it into the serialization stream.
    //
    // We only want to see SerializationProxy, never Version.
    //
    /**
     * @param stream
     *            See description of java.io.Serializable
     */
    private void readObject(ObjectInputStream stream)
	    throws InvalidObjectException {
	throw new InvalidObjectException("SerializationProxy required");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
	//
	// Testing Version
	//
	Version<?> current = Version.getInstance();
	Version<?> earlier = new Version<Object>("20110806");
	Version<?> later = new Version<Object>("22221231");
	System.out.println("Current version is " + current.getVersion());
	System.out.println("Earlier version is " + earlier.getVersion());
	System.out.println("Later   version is " + later.getVersion());

	SortedSet<Version<?>> set = new TreeSet<Version<?>>();
	set.add(current);
	set.add(earlier);
	set.add(later);
	System.out.println(set.toString());
    }

    // Serialization proxy for the Version class.
    //
    // Based on Item 78 "Consider serialization proxies instead of
    // serialized instances" in _Effective Java_ by Joshua Bloch
    // Second Edition, 4th printing September 2008.
    //
    private static class SerializationProxy implements Serializable {
	private final String serializedVersion;

	@SuppressWarnings("synthetic-access")
	SerializationProxy(Version<?> v) {
	    serializedVersion = v.version;
	}

	private static final long serialVersionUID = 1L;

	//
	// Create an instance of Version using only its public API,
	// based on the instance of SerializationProxy that was deserialized.
	//
	private Object readResolve() {
	    return new Version<String>(serializedVersion);
	}
    }

}
