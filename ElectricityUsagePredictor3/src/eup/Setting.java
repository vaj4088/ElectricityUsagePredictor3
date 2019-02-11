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

import java.util.List;
import java.util.Map;

/**
 * 
 * Represents a single preference. For each valid preference, there is an
 * instance of this class that holds its name (its key) and its default value.
 * Both the name and the default value are of type String. The class itself
 * holds a collection of all valid preferences. A Setting has no default name
 * and a default value of "" (the empty String).
 * 
 * @author Ian Shef
 * 
 *         Updated 5 Dec 2009 to add 16 settings (numbered 0 through f 
 *         [hexadecimal) for personal time entries.
 *         Updated 20 Jan 2019 for settings appropriate for predicting
 *         electricity usage.
 * 
 */
public class Setting {
    private String name;
    private String defaultValue;
    static Map<String, Setting> settingsMap = Util.makeHashMap();
/*
 *  public static final String PROXY_HOST = "proxyhost";
    public static final String PROXY_PORT = "proxyport";
    public static final String PROXY_ENABLE = "proxyenable";
    public static final String PROXY_NOPROXYFOR = "proxynoproxyfor";
    public static final String PERSONAL_TIME = "personaltime_";
    public static final String VIEW_COMPRESS_SCHEDULE_VIEW = 
	"viewcompressscheduleview";
 *
 */
    public static final String MOST_RECENT_BILL_DATE_YEAR  = 
	    "mostRecentBillDateYear" ;
    public static final String MOST_RECENT_BILL_DATE_MONTH = 
	    "mostRecentBillDateMonth" ;
    public static final String MOST_RECENT_BILL_DATE_DAY   = 
	    "mostRecentBillDateDay" ;
    public static final String MOST_RECENT_BILL_DATE_READING  = 
	    "mostRecentBillDateReaading" ;
    public static final String MOST_RECENT_BILL_DATE_VALID  = 
	    "mostRecentBillDateValid" ;
    public static final String NEXT_BILL_DATE_YEAR  = "nextBillDateYear" ;
    public static final String NEXT_BILL_DATE_MONTH  = "nextBillDateMonth" ;
    public static final String NEXT_BILL_DATE_DAY  = "nextBillDateDay" ;
    static {
	List<Setting> settingsList = Util.makeArrayList(20);
/*
 * 	settingsList.add(new Setting(PROXY_HOST));
	settingsList.add(new Setting(PROXY_PORT).setDefaultValue("80"));
	settingsList.add(new Setting(PROXY_ENABLE).setDefaultValue("false"));
	settingsList.add(new Setting(PROXY_NOPROXYFOR));
	for (int i = 0; i <= 15; i++) {
	    String s = String
		    .format(PERSONAL_TIME + "%1x", Integer.valueOf(i));
	    settingsList.add(new Setting(s));
	}
	settingsList.add(new Setting(VIEW_COMPRESS_SCHEDULE_VIEW).
		setDefaultValue("false"));
 *
 */
	settingsList.add(new Setting(MOST_RECENT_BILL_DATE_YEAR,"2019")) ;
	settingsList.add(new Setting(MOST_RECENT_BILL_DATE_MONTH,"1")) ;
	settingsList.add(new Setting(MOST_RECENT_BILL_DATE_DAY,"9")) ;
	settingsList.add(new Setting(MOST_RECENT_BILL_DATE_READING,"-1")) ;
	settingsList.add(new Setting(MOST_RECENT_BILL_DATE_VALID,"FALSE")) ;
	settingsList.add(new Setting(NEXT_BILL_DATE_YEAR,"2019")) ;
	settingsList.add(new Setting(NEXT_BILL_DATE_MONTH,"2")) ;
	settingsList.add(new Setting(NEXT_BILL_DATE_DAY,"8")) ;
	for (int i = 0; i < settingsList.size(); i++) {
	    Setting s = settingsList.get(i);
	    settingsMap.put(s.name, s);
	}
    }

    private Setting(String name, String defaultValue) {
	this.name = name;
	this.defaultValue = defaultValue;
    }

    private Setting() {
	this("undefined", "");
    }

    private Setting(String name) {
	this(name, "");
    }

    /**
     * @return the name
     */
    String getName() {
	return name;
    }

    /**
     * @param name
     *            the name to set
     */
    Setting setName(String name) {
	this.name = name;
	return this;
    }

    /**
     * @return the defaultValue
     */
    String getDefaultValue() {
	return defaultValue;
    }

    /**
     * @param defaultValue
     *            the defaultValue to set
     */
    Setting setDefaultValue(String defaultValue) {
	this.defaultValue = defaultValue;
	return this;
    }

    public static Map<String, Setting> getSettingsMap() {
	return settingsMap;
    }

    @Override
    public String toString() {
	return "Setting: " + getName() + "(default=" + getDefaultValue() + ")";
    }
}
