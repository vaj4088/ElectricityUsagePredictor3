/**
* 
*    Copyright 2015 Ian Shef
* 
*    This file is part of the Personal Class Scheduler, also known as
*    ClassScheduler.
*
*    ClassScheduler is free software: you can redistribute it and/or modify
*    it under the terms of the GNU Affero General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    ClassScheduler is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU Affero General Public License for more details.
*
*    You should have received a copy of the GNU Affero General Public License
*    along with ClassScheduler.  If not, see <http://www.gnu.org/licenses/>.
* 
 */
package eup;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * Provides access (public methods) to preferences for non-privileged objects,
 * and provides for setting (default package access methods)
 * by privileged objects.
 * "Privileged" means included in this package.
 * All methods (individually) are thread-safe.
 * 
 * @author Ian Shef
 *
 */
public class CommonPreferences {
	
	private CommonPreferences() {
		// Intentionally empty, no publicly accessible constructors.
	}
	
	static {
		deleteUnnecessarySettings() ;
		createRequiredSettings() ;
	}

	// Lazy initialization holder class idiom for static fields
	//
	// based on the
	// article "More Effective Java With Google's Joshua Bloch"
	// by Janice J. Heiss, October 2008.
	// at URL
	// http://java.sun.com/developer/technicalArticles/
	// Interviews/bloch_effective_08_qa.html
	//
	private static class CommonPreferencesHolder {
		static final Preferences prefs ;
		static {
			Preferences p = null ;
			try {
				p =
					Preferences.userNodeForPackage(
							Class.forName(
			"eup.pref_version_1.PrefVersionPlaceHolder")
					) ;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				prefs = p ;
			}
		}
	}

	public static synchronized void set(Setting s, 
		CharSequence charSequence) {
	    if (Setting.settingsMap.containsKey(s.getName())) {
		Preferences prefs = CommonPreferencesHolder.prefs;
		prefs.put(s.getName(), charSequence.toString()) ;
		try {
		    prefs.flush() ;
		} catch (BackingStoreException e) {
		    Util.msg(Util.getCallerClassName() + "." +
		    Util.getCallerMethodName() + "(...) failed due "+
		    "to a Backing Store Exception.") ;
		}
	    } else {
		Util.msg(Util.getCallerClassName() + "." +
			Util.getCallerMethodName() + "(...) rejected for " +
			s.getName()) ;
	    }
	}
	
	public static synchronized void remove(Setting s) {
	    if (Setting.settingsMap.containsKey(s.getName())) {
		Preferences prefs = CommonPreferencesHolder.prefs;
		prefs.remove(s.getName()) ;
		try {
		    prefs.flush() ;
		} catch (BackingStoreException e) {
		    Util.msg(Util.getCallerClassName() + "." +
		    Util.getCallerMethodName() + "(...) failed due "+
		    "to a Backing Store Exception.") ;
		}
	    } else {
		Util.msg(Util.getCallerClassName() + "." +
			Util.getCallerMethodName() + "(...) rejected for " +
			s.getName()) ;
	    }
	}
	
	public static synchronized String get(Setting s) {
	    if (Setting.settingsMap.containsKey(s.getName())) {
		Preferences prefs = CommonPreferencesHolder.prefs;
		try {
		    prefs.sync() ;
		} catch (BackingStoreException e) {
		    Util.msg(Util.getCallerClassName() + "." +
			    Util.getCallerMethodName() + "() failed due "+
		    "to a Backing Store Exception.") ;
		}
		String v = prefs.get(s.getName(), s.getDefaultValue()) ;
		return v ;				
	    }  // else...
	    Util.msg("Invalid argument in call to "+ Util.getCallerClassName() + 
		    "." +
		    Util.getCallerMethodName() + "()"+" using " + s.getName()) ;
	    Util.msg("Returned an empty String.") ;
	    return "" ;
	}

	/**
	 * Gets all known preferences and their current value.
	 * If the current value for a preference is not available, then 
	 * the default value is returned.
	 * The result is returned as a Map, where each element is
	 * the name of a preference, and its value.
	 * The names and values are a subclass of String.
	 * Implementation note:  The returned Map is unmodifiable.
	 * 
	 * @return a Map of preference names and values.
	 */
	public static synchronized 
	  Map<? extends String, ? extends String> getPreferences() {
		Preferences prefs = CommonPreferencesHolder.prefs;
		Map <String, String> result ;
		try {
			prefs.sync();
		} catch (BackingStoreException e) {
			// Ignore if backing store is not available.
			Util.msg( Util.getCallerClassName() + "." +
					Util.getCallerMethodName() + "() failed to sync due "+
					"to a Backing Store Exception.") ;
		}
		result = Util.makeHashMap(Setting.settingsMap.size()) ;
		for (Setting s: Setting.settingsMap.values()) {
			result.put(s.getName(), 
					prefs.get(s.getName(), s.getDefaultValue())) ;
		}
		result = Collections.unmodifiableMap(result) ;
		return result ;
	}

	/**
	 * @param ncl
	 * @see java.util.prefs.Preferences#addNodeChangeListener(java.util.prefs.NodeChangeListener)
	 */
	static synchronized void 
	addNodeChangeListener(NodeChangeListener ncl) {
		CommonPreferencesHolder.prefs.addNodeChangeListener(ncl);
	}

	/**
	 * @param pcl
	 * @see java.util.prefs.Preferences#addPreferenceChangeListener(java.util.prefs.PreferenceChangeListener)
	 */
	public static synchronized void 
	addPreferenceChangeListener(PreferenceChangeListener pcl) {
		CommonPreferencesHolder.prefs.addPreferenceChangeListener(pcl);
	}

	/**
	 * @param ncl
	 * @see java.util.prefs.Preferences#removeNodeChangeListener(java.util.prefs.NodeChangeListener)
	 */
	static synchronized void 
	removeNodeChangeListener(NodeChangeListener ncl) {
		CommonPreferencesHolder.prefs.removeNodeChangeListener(ncl);
	}

	/**
	 * @param pcl
	 * @see java.util.prefs.Preferences#removePreferenceChangeListener(java.util.prefs.PreferenceChangeListener)
	 */
	public static synchronized void 
	removePreferenceChangeListener(PreferenceChangeListener pcl) {
		CommonPreferencesHolder.prefs.removePreferenceChangeListener(pcl);
	}
	
	private static void deleteUnnecessarySettings() {
		Preferences prefs = CommonPreferencesHolder.prefs;
		Collection <String> keys = 
			Util.makeArrayList(Setting.settingsMap.size());
		try {
			prefs.sync() ;
			Collections.addAll(keys, prefs.keys()) ;
		} catch (BackingStoreException e) {
			Util.msg(Util.getCallerClassName() + "." +
					Util.getCallerMethodName() + "() failed to get keys due "+
			"to a Backing Store Exception.") ;
		}
		for (String k: keys) {
			if (!Setting.settingsMap.containsKey(k)) {
				prefs.remove(k) ;
				Util.msg(Util.getCallerClassName() + "." +
						Util.getCallerMethodName() + "() removed "+
				"setting " + k) ;
			}
		}
		try {
			prefs.sync() ;
		} catch (BackingStoreException e) {
			Util.msg(Util.getCallerClassName() + "." +
					Util.getCallerMethodName() + 
					"() failed to update changes due "+
			"to a Backing Store Exception.") ;
		}
	}
	
	private static void createRequiredSettings() {
		Preferences prefs = CommonPreferencesHolder.prefs;
		Set <String> prefsKeySet = Util.makeHashSet(10) ;
		try {
			prefs.sync() ;
			Collections.addAll(prefsKeySet, prefs.keys()) ;
		} catch (BackingStoreException e) {
			Util.msg(Util.getCallerClassName() + "." +
					Util.getCallerMethodName() + "() failed due "+
			"to a Backing Store Exception.") ;
		}
		for (Setting element : Setting.settingsMap.values()) {
			if (!prefsKeySet.contains(element.getName())) {
				prefs.put(element.getName(), element.getDefaultValue()) ;
			}
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Util.msg("Test of " + Util.getCallerClassName()) ;
		NodeChangeListener ncl = new NodeChangeListener() {
			@Override
			public void childAdded(NodeChangeEvent evt) {
				Util.msg("NodeChangeListener childAdded: " + evt) ;				
			}
			@Override
			public void childRemoved(NodeChangeEvent evt) {
				Util.msg("NodeChangeListener childRemoved: " + evt) ;				
			}
		} ;
		PreferenceChangeListener pcl = new PreferenceChangeListener() {
			@Override
			public void preferenceChange(PreferenceChangeEvent evt) {
				Util.msg("preferenceChangeListener preferenceChange: " + evt + 
						" key = " + evt.getKey() + ", new value = " +
						evt.getNewValue()) ;				
			}
		} ;
		addNodeChangeListener(ncl) ;
		addPreferenceChangeListener(pcl) ;
		
		displayPreferences("\n ##  Preferences prior to changes.") ;
		Util.msg("") ;

		set(Setting.settingsMap.get("proxyhost"), "proxyhost_test") ;
		set(Setting.settingsMap.get("proxynoproxyfor"), "proxynoproxyfor_test") ;
		set(Setting.settingsMap.get("proxyenable"), "true") ;
		set(Setting.settingsMap.get("proxyport"), "8765") ;
		displayPreferences("\n ##  Preferences after changes.") ;
		Util.msg("") ;

		set(Setting.settingsMap.get("proxyhost"), "") ;
		set(Setting.settingsMap.get("proxynoproxyfor"), "") ;
		set(Setting.settingsMap.get("proxyenable"), "false") ;
		set(Setting.settingsMap.get("proxyport"), "80") ;
		displayPreferences("\n ##  Preferences after restoration.") ;
		Util.msg("") ;

		
		removePreferenceChangeListener(pcl) ;
		removeNodeChangeListener(ncl) ;
	}

	/**
	 * 
	 */
	static void displayPreferences(Object s) {
		Util.msg(s) ;
		Map<? extends String, ? extends String> stdPrefs = getPreferences() ;
		for (Object k : stdPrefs.keySet()) {
			Object v = stdPrefs.get(k) ;
			Util.msg("Standard preference key = " + k +
					", Current value = " + v) ;
		}
	}

	public static char getElementSeparator() { return '\u0001' ; } // u0001 = SOH

}

