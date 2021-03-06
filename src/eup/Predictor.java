/**
 * 
 */
package eup;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * @author Ian Shef
 * 
 *         1 July 2018 Ian Shef ibs
 *         See _ Effective Java, Second Edition _ by Joshua Bloch
 *         p. 14
 *
 */
public class Predictor {

    /**
     * @param args
     */
    private final LocalDate dateBillCurrent ;
    private final LocalDate dateCurrent ;
    private final LocalDate dateBillNext ;
    
    private final int meterReadingBillCurrent ;
    private final int meterReadingCurrent ;
    /* Will predict meterReadingNext and usageNext */

    	/**
	 * @return the dateBillCurrent
	 */
	public LocalDate getDateBillCurrent() {
	    return dateBillCurrent ;
	}
	
	/**
	 * @return the dateCurrent
	 */
	public LocalDate getDateCurrent() {
	    return dateCurrent ;
	}

	/**
	 * @return the dateBillNext
	 */
	public LocalDate getDateBillNext() {
	    return dateBillNext ;
	}
	
	/**
	 * @return the meterReadingBillCurrent
	 */
	public int getMeterReadingBillCurrent() {
	    return meterReadingBillCurrent;
	}

	/**
	 * @return the meterReadingCurrent
	 */
	public int getMeterReadingCurrent() {
	    return meterReadingCurrent ;
	}

	public int predictUsage() {
	    long billingDays = ChronoUnit.DAYS.between(getDateBillCurrent(),
		    getDateBillNext()) ;
	    long usageDays   = ChronoUnit.DAYS.between(getDateBillCurrent(),
		    getDateCurrent()) ;
	    if (usageDays<=0) {
		usageDays   = ChronoUnit.DAYS.between(
			(getDateBillCurrent()).minusMonths(1),
			    getDateCurrent()) ;
	    }
	    int usage = getMeterReadingCurrent() - getMeterReadingBillCurrent() ;
	    return Math.round(((float)billingDays/usageDays) * usage) ;
	}

	public int predictMeterReading() {
	    return predictUsage() + getMeterReadingBillCurrent() ;
	}

public static class Builder {
	// Required parameters
	private LocalDate dateBillCurrent;
	private LocalDate dateCurrent;
	private LocalDate dateBillNext;
	private int meterReadingBillCurrent;
	private int meterReadingCurrent;
	
	// Optional parameters initialized to default values - NONE
	
	public Builder currentBillDate(LocalDate date) {
	    dateBillCurrent = date ;  return this ;
	}
	public Builder currentDate(LocalDate date) {
	    dateCurrent = date ;  return this ;
	}
	public Builder nextBillDate(LocalDate date) {
	    dateBillNext = date ;  return this; 
	}
	public Builder currentBillMeterReading(int usage) {
	    meterReadingBillCurrent = usage ;  return this ;
	}
	public Builder currentMeterReading(int usage) {
	    meterReadingCurrent = usage ;  return this ;
	}
	
	@SuppressWarnings("synthetic-access")
	public Predictor build() {
	    return new Predictor(this) ;
	}
    }
    
    @SuppressWarnings("synthetic-access")
    private Predictor(Builder builder) {
	dateBillCurrent   = builder.dateBillCurrent ;
	dateCurrent       = builder.dateCurrent ;
	dateBillNext      = builder.dateBillNext ;
	meterReadingBillCurrent  = builder.meterReadingBillCurrent ;
	meterReadingCurrent      = builder.meterReadingCurrent ;
    }
    
    public static void main(String[] args) {
	/* Intentionally empty until I have something to put here. */
    }

}
