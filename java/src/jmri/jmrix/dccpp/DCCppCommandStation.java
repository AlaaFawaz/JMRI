/*
 * DCCppCommandStation.java
 */
package jmri.jmrix.dccpp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Defines the standard/common routines used in multiple classes related to the
 * a DCC++ Command Station, on a DCC++ network.
 *
 * @author	Bob Jacobsen Copyright (C) 2001 Portions by Paul Bender Copyright (C) 2003
 * @author	Mark Underwood Copyright (C) 2015
 * @version	$Revision$
 *
 * Based on LenzCommandStation by Bob Jacobsen and Paul Bender
 */
public class DCCppCommandStation implements jmri.jmrix.DccCommandStation, jmri.CommandStation {

    /* The First group of routines is for obtaining the Software and
     hardware version of the Command station */
    /**
     * We need to add a few data members for saving the version information we
     * get from the layout.
     *
     */
    private String baseStationType;
    private String codeBuildDate;
    private int registers[];

    /** ctor */
    public DCCppCommandStation() {
	super();
	registers = new int[DCCppConstants.MAX_MAIN_REGISTERS];
	// Zero out the register addresses.
	for (int i = 0; i < DCCppConstants.MAX_MAIN_REGISTERS; i++)
	    registers[i] = DCCppConstants.REGISTER_UNALLOCATED;
	
    }

    public void setBaseStationType(String s) {
	baseStationType = s;
    }
    
    public String getBaseStationType() {
	return baseStationType;
    }

    public void setCodeBuildDate(String s) {
	codeBuildDate = s;
    }

    public String getCodeBuildDate() {
	return codeBuildDate;
    }

    /**
     * Parses the DCC++ CS status response to pull out the base station version
     * and software version.
     *
     */
    protected void setCommandStationInfo(DCCppReply l) {
	String syntax = "iDCC\\+\\+\\ BASE\\ STATION v([a-zA-Z0-9_.]+): BUILD ((\\w{3}\\W\\d{1,2}\\W\\d{4})\\W(\\d{2}:\\d{2}:\\d{2}))";
	String s = l.toString();
	try {
	    Pattern p = Pattern.compile(syntax);
	    Matcher m = p.matcher(s);
	    if (!m.matches()) {
		log.error("DCC++ Status string does not match pattern. syntax= {} string= {}", syntax, s);
		return;
	    }
	    log.debug("DCC++ Status version = {} build date = {} time = {}",
		      m.group(1), m.group(2));
	    baseStationType = m.group(1);
	    codeBuildDate = m.group(2);
	} catch (PatternSyntaxException e) {
            log.error("Malformed DCC++ version syntax! " + syntax);
            return;
        } catch (IllegalStateException e) {
            log.error("Group called before match operation executed syntax=" + syntax + " string= " + s);
            return;
        } catch (IndexOutOfBoundsException e) {
            log.error("Index out of bounds " + syntax + " string= " + s);
            return;
        }
    }

    /**
     * Provides the version string returned during the initial check. This
     * function is not yet implemented...
     *
     */
    public String getVersionString() {
        return "<unknown>";
    }

    /* 
     * The next group of messages has to do with determining if the
     * command station has, and is currently in service mode 
     */
    /**
     * DCC++ does use a service mode
     */
    public boolean getHasServiceMode() {
        return true;
    }

    /**
     * If this command station has a service mode, is the command station
     * currently in that mode?
     */
    public boolean getInServiceMode() {
        //return mInServiceMode;
	return(true); // TODO: Verify we are effectively always in service mode.
	//TODO: Or just eliminate the concept of Service Mode entirely.
	// Perhaps not possible, since it is an NMRA standard.
    }

    /**
     * Remember whether or not in service mode
     *
     */
    boolean mInServiceMode = false;

    /**
     * DCC++ command station does provide Ops Mode.
     */
    public boolean isOpsModePossible() {
	return true;
    }

    // A few utility functions
    /**
     * Get the Lower byte of a locomotive address from the decimal locomotive
     * address
     */
    public static int getDCCAddressLow(int address) {
        /* For addresses below 128, we just return the address, otherwise,
         we need to return the upper byte of the address after we add the
         offset 0xC000. The first address used for addresses over 127 is 0xC080*/
        if (address < 128) {
            return (address);
        } else {
            int temp = address + 0xC000;
            temp = temp & 0x00FF;
            return temp;
        }
    }

    /**
     * Get the Upper byte of a locomotive address from the decimal locomotive
     * address
     */
    public static int getDCCAddressHigh(int address) {
        /* this isn't actually the high byte, For addresses below 128, we
         just return 0, otherwise, we need to return the upper byte of the
         address after we add the offset 0xC000 The first address used for
         addresses over 127 is 0xC080*/
        if (address < 128) {
            return (0x00);
        } else {
            int temp = address + 0xC000;
            temp = temp & 0xFF00;
            temp = temp / 256;
            return temp;
        }

    }

    /* To Implement the CommandStation Interface, we have to define the 
     sendPacket function */
    /**
     * Send a specific packet to the rails.
     *
     * @param packet  Byte array representing the packet, including the
     *                error-correction byte. Must not be null.
     * @param repeats Number of times to repeat the transmission.
     */
    public void sendPacket(byte[] packet, int repeats) {

        if (_tc == null) {
            log.error("Send Packet Called without setting traffic controller");
            return;
        }

	int reg = 1; // TODO: Fix this when I understand registers...
	DCCppMessage msg = DCCppMessage.getWriteDCCPacketMainMsg(reg, packet.length, packet);

        for (int i = 0; i < repeats; i++) {
            _tc.sendDCCppMessage(msg, null);
        }
    }

    /*
     * For the command station interface, we need to set the traffic 
     * controller.
     */
    public void setTrafficController(DCCppTrafficController tc) {
        _tc = tc;
    }

    private DCCppTrafficController _tc = null;

    public void setSystemConnectionMemo(DCCppSystemConnectionMemo memo) {
        adaptermemo = memo;
    }

    DCCppSystemConnectionMemo adaptermemo;

    public String getUserName() {
        if (adaptermemo == null) {
            return "DCC++";
        }
        return adaptermemo.getUserName();
    }

    public String getSystemPrefix() {
        if (adaptermemo == null) {
            return "DCCpp";
        }
        return adaptermemo.getSystemPrefix();
    }

    /*
     * We need to register for logging
     */
    static Logger log = LoggerFactory.getLogger(DCCppCommandStation.class.getName());

}


/* @(#)DCCppCommandStation.java */
