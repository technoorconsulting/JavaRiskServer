/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package technoor.service;

/**
 *
 * @author salman
 */
public interface RequestOptions {

    public int getInstrumentId();

    public int getNumOfDays();

    /**
     * @param numOfDays the numOfDays to set
     */
    public void setNumOfDays(int numOfDays);

    /**
     * @return the timePeriod
     */
    public int getTimePeriod();

    /**
     * @param timePeriod the timePeriod to set
     */
    public void setTimePeriod(int timePeriod);
}
