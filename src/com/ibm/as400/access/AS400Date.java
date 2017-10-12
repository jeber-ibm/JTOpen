///////////////////////////////////////////////////////////////////////////////
//
// JTOpen (IBM Toolbox for Java - OSS version)
//
// Filename:  AS400Date.java
//
// The source code contained herein is licensed under the IBM Public License
// Version 1.0, which has been approved by the Open Source Initiative.
// Copyright (C) 2010-2010 International Business Machines Corporation and
// others.  All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 Provides a converter between a {@link java.sql.Date java.sql.Date} object and an IBM i <i>date</i> value such 
 as "12/31/97".
 In the IBM i programming reference, this type is referred to as the "<b>Date</b> Data Type", or 
 DDS data type <tt>L</tt>.  Note: Only date fields that are stored on the IBM i system as <b>EBCDIC characters</b> 
 are currently supported by this class. (Some IBM i "logical files" have date fields stored as zoned or 
 packed decimal values.)
 <p>
 An IBM i <i>date</i> value simply indicates a year/month/day, and does not indicate a contextual time zone.  
 Internally, this class interprets all date- and time-related strings as relative to the timezone of the server.
 <p>
 Suggestion: To avoid confusion and unexpected results when crossing time zones:
 <br>Whenever creating or interpreting instances of {@link java.sql.Date java.sql.Date}, {@link java.sql.Time java.sql.Time}, 
 or {@link java.sql.Timestamp java.sql.Timestamp}, always assume that the reference time zone for the object is the same
 as the server, and avoid using any deprecated methods.  If it is necessary to convert date/time values between 
 the server time zone and other time zones, use methods of <tt>Calendar</tt>.  Rather than using <tt>toString()</tt> 
 to display the value of a date/time object, use <tt>DateFormat.format()</tt> 
 after specifying the server TimeZone.
 For example:
 <code>
 import java.text.SimpleDateFormat;
 java.sql.Date date1;  // value to be generated by AS400Date
 SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
 // Set the formatter's time zone to GMT.
 formatter.setTimeZone(as400.getTimeZone());
 ...
 System.out.println("Date value: " + formatter.format(date1));
 </code>

 <p>
 Suggestion: To avoid ambiguity and confusion between different kinds of "Date" objects, fully qualify all references to classes <tt>java.<b>util</b>.Date</tt> and <tt>java.<b>sql</b>.Date</tt> (especially if you import both <tt>java.util.*</tt> and <tt>java.sql.*</tt>).
 <p>
 Note: In the descriptions of the "format" constants, all example dates represent the date <b>April 25, 1997</b>.

 @see AS400Time
 @see AS400Timestamp
 **/
public class AS400Date extends AS400AbstractTime
{
  /**
   * 
   */
  private static final long serialVersionUID = -2802488573427538664L;
  
  private java.sql.Date defaultValue_;
  private static Hashtable formatsMap_;

  /** Date format MDY (mm/dd/yy).
   <br>Example: 04/25/97
   <br>Range of years: 1940-2039
   <br>Default separator: '/' **/
  public static final int FORMAT_MDY = 0;

  /** Date format DMY (dd/mm/yy).
   <br>Example: 25/04/97
   <br>Range of years: 1940-2039
   <br>Default separator: '/' **/
  public static final int FORMAT_DMY = 1;

  /** Date format YMD (yy/mm/dd).
   <br>Example: 97/04/25
   <br>Range of years: 1940-2039
   <br>Default separator: '/' **/
  public static final int FORMAT_YMD = 2;

  /** Date format JUL (yy/ddd).
   <br>Example: 97/115
   <br>Range of years: 1940-2039
   <br>Default separator: '/' **/
  public static final int FORMAT_JUL = 3;

  /** Date format ISO (yyyy-mm-dd).
   <br>Example: 1997-04-25
   <br>Range of years: 0001-9999
   <br>Default separator: '-' **/
  public static final int FORMAT_ISO = 4;

  /** Date format USA (mm/dd/yyyy).
   <br>Example: 04/25/1997
   <br>Range of years: 0001-9999
   <br>Default separator: '/' **/
  public static final int FORMAT_USA = 5;

  /** Date format EUR (dd.mm.yyyy).
   <br>Example: 25.04.1997
   <br>Range of years: 0001-9999
   <br>Default separator: '.' **/
  public static final int FORMAT_EUR = 6;

  /** Date format JIS (yyyy-mm-dd).
   <br>Example: 1997-04-25
   <br>Range of years: 0001-9999
   <br>Default separator: '-' **/
  public static final int FORMAT_JIS = 7;

  // Externally defined date formats:

  /** Date format CYMD (cyy/mm/dd).
   <br>Example: 097/04/25
   <br>Range of years: 1900-2899
   <br>Default separator: '/' **/
  public static final int FORMAT_CYMD = 8;

  /** Date format CMDY (cmm/dd/yy).
   <br>Example: 004/25/97
   <br>Range of years: 1900-2899
   <br>Default separator: '/' **/
  public static final int FORMAT_CMDY = 9;

  /** Date format CDMY (cdd/mm/yy).
   <br>Example: 025/04/97
   <br>Range of years: 1900-2899
   <br>Default separator: '/' **/
  public static final int FORMAT_CDMY = 10;

  /** Date format LONGJUL (yyyy/ddd).
   <br>Example: 1997/115
   <br>Range of years: 0001-9999
   <br>Default separator: '/' **/
  public static final int FORMAT_LONGJUL = 11;

  /** Date format MY (mm/yy).
   <br>Example: 04/97
   <br>Range of years: 1940-2039
   <br>Default separator: '/' **/
  public static final int FORMAT_MY = 12;

  /** Date format YM (yy/mm).
   <br>Example: 97/04
   <br>Range of years: 1940-2039
   <br>Default separator: '/' **/
  public static final int FORMAT_YM = 13;

  /** Date format MYY (mm/yyyy).
   <br>Example: 04/1997
   <br>Range of years: 0001-9999
   <br>Default separator: '/' **/
  public static final int FORMAT_MYY = 14;

  /** Date format YYM (yyyy/mm).
   <br>Example: 1997/04
   <br>Range of years: 0001-9999
   <br>Default separator: '/' **/
  public static final int FORMAT_YYM = 15;


  // Note to maintenance programmer: Update these values when adding new formats.
  private static final int FORMAT_RANGE_MINIMUM = FORMAT_MDY;
  private static final int FORMAT_RANGE_MAXIMUM = FORMAT_YYM;

  /**
  Constructs an AS400Date object.
  Assumes the default GMT time zone. 
  Format {@link #FORMAT_ISO FORMAT_ISO} and separator '-' are used.
  **/
 public AS400Date()
 {
   this(FORMAT_ISO);
 }

  /**
   Constructs an AS400Date object.
   Format {@link #FORMAT_ISO FORMAT_ISO} and separator '-' are used.
   * @param timeZone 
   **/
  public AS400Date(TimeZone timeZone)
  {
    this(timeZone, FORMAT_ISO);
  }


 /**
 Constructs an AS400Date object.
 The specified format's default separator is used.
 Assumes the default GMT timezone 
 @param format The date format.
 For a list of valid values, refer to {@link #AS400Date(int,Character) AS400Date(int,Character)}.
 **/
public AS400Date(int format)
{
  
  setFormat(format, defaultSeparatorFor(format));
}

  
  /**
   Constructs an AS400Date object.
   The specified format's default separator is used.
   * @param timeZone 
   @param format The date format.
   For a list of valid values, refer to {@link #AS400Date(int,Character) AS400Date(int,Character)}.
   **/
  public AS400Date(TimeZone timeZone, int format)
  {
    
    super(timeZone); 
    setFormat(format, defaultSeparatorFor(format));
  }

 
 /**
 Constructs an AS400Date object.
 The specified format's default separator is used.
 Assumes the default GMT timezone is used. 
 @param format The date format.
 <br>Valid values are:
 <ul>
 <li>{@link #FORMAT_MDY FORMAT_MDY}
 <li>{@link #FORMAT_DMY FORMAT_DMY}
 <li>{@link #FORMAT_YMD FORMAT_YMD}
 <li>{@link #FORMAT_JUL FORMAT_JUL}
 <li>{@link #FORMAT_ISO FORMAT_ISO}
 <li>{@link #FORMAT_USA FORMAT_USA}
 <li>{@link #FORMAT_EUR FORMAT_EUR}
 <li>{@link #FORMAT_JIS FORMAT_JIS}
 <li>{@link #FORMAT_CYMD FORMAT_CYMD}
 <li>{@link #FORMAT_CMDY FORMAT_CMDY}
 <li>{@link #FORMAT_CDMY FORMAT_CDMY}
 <li>{@link #FORMAT_LONGJUL FORMAT_LONGJUL}
 <li>{@link #FORMAT_MY FORMAT_MY}
 <li>{@link #FORMAT_YM FORMAT_YM}
 <li>{@link #FORMAT_MYY FORMAT_MYY}
 <li>{@link #FORMAT_YYM FORMAT_YYM}
 </ul>
 @param separator  The separator character.
 Valid values are:
 <tt>
 <ul>
 <li>'&amp;' <i>(ampersand)</i>
 <li>' ' <i>(blank)</i>
 <li>',' <i>(comma)</i>
 <li>'-' <i>(hyphen)</i>
 <li>'.' <i>(period)</i>
 <li>'/' <i>(slash)</i>
 <li>(null)
 </ul>
 </tt>
 A null value indicates "no separator".
 Refer to the IBM i programming reference to determine which separator characters are valid with each format.
 **/
public AS400Date(int format, Character separator)
{
  this(); 
  setFormat(format, separator);
}

  
  /**
   Constructs an AS400Date object.
   The specified format's default separator is used.
   * @param timeZone 
   @param format The date format.
   <br>Valid values are:
   <ul>
   <li>{@link #FORMAT_MDY FORMAT_MDY}
   <li>{@link #FORMAT_DMY FORMAT_DMY}
   <li>{@link #FORMAT_YMD FORMAT_YMD}
   <li>{@link #FORMAT_JUL FORMAT_JUL}
   <li>{@link #FORMAT_ISO FORMAT_ISO}
   <li>{@link #FORMAT_USA FORMAT_USA}
   <li>{@link #FORMAT_EUR FORMAT_EUR}
   <li>{@link #FORMAT_JIS FORMAT_JIS}
   <li>{@link #FORMAT_CYMD FORMAT_CYMD}
   <li>{@link #FORMAT_CMDY FORMAT_CMDY}
   <li>{@link #FORMAT_CDMY FORMAT_CDMY}
   <li>{@link #FORMAT_LONGJUL FORMAT_LONGJUL}
   <li>{@link #FORMAT_MY FORMAT_MY}
   <li>{@link #FORMAT_YM FORMAT_YM}
   <li>{@link #FORMAT_MYY FORMAT_MYY}
   <li>{@link #FORMAT_YYM FORMAT_YYM}
   </ul>
   @param separator  The separator character.
   Valid values are:
   <tt>
   <ul>
   <li>'&amp;' <i>(ampersand)</i>
   <li>' ' <i>(blank)</i>
   <li>',' <i>(comma)</i>
   <li>'-' <i>(hyphen)</i>
   <li>'.' <i>(period)</i>
   <li>'/' <i>(slash)</i>
   <li>(null)
   </ul>
   </tt>
   A null value indicates "no separator".
   Refer to the IBM i programming reference to determine which separator characters are valid with each format.
   **/
  public AS400Date(TimeZone timeZone, int format, Character separator)
  {
    this(timeZone); 
    setFormat(format, separator);
  }


  // Overrides method of superclass.
  /**
   Returns a Java object representing the default value of the data type.
   @return A <tt>java.sql.Date</tt> object with a value of January 1, 1970, 00:00:00 GMT.
   **/
  public Object getDefaultValue()
  {
    if (defaultValue_ == null) {
      defaultValue_ = new java.sql.Date(0L); // January 1, 1970 GMT
    }

    return defaultValue_;
  }


  // Overrides non-public method of superclass, making it public.
  /**
   Gets the format of this AS400Date object.
   @return format  The format for this object.
   For a list of possible values, refer to {@link #AS400Date(int,Character) AS400Date(int,Character)}.
   **/
  public int getFormat()
  {
    return super.getFormat();
  }

  /**
   Gets the separator character of this AS400Date object.
   @return separator  The separator character.
   For a list of possible values, refer to {@link #AS400Date(int,Character) AS400Date(int,Character)}.
   If the format contains no separators, null is returned.
   @see #setFormat(int,Character)
   **/
  public Character getSeparator()
  {
    return super.getSeparator();
  }

  // Implements abstract method of superclass.
  /**
   Returns {@link AS400DataType#TYPE_DATE TYPE_DATE}.
   @return <tt>AS400DataType.TYPE_DATE</tt>.
   **/
  public int getInstanceType()
  {
    return AS400DataType.TYPE_DATE;
  }

  // Implements abstract method of superclass.
  /**
   Returns the Java class that corresponds with this data type.
   @return <tt>java.sql.Date.class</tt>.
   **/
  public Class getJavaType()
  {
    return java.sql.Date.class;
  }


  /**
   Sets the format of this AS400Date object.
   The specified format's default separator character is used.
   @param format  The format for this object.
   For a list of valid values, refer to {@link #AS400Date(int,Character) AS400Date(int,Character)}.
   **/
  public void setFormat(int format)
  {
    super.setFormat(format, defaultSeparatorFor(format));
  }


  // Method used by DateFieldDescription.
  /**
   Sets the format of this AS400Date object.
   @param format  The format for this object, expressed as a string.
   For a list of valid values, refer to {@link #toFormat(String) toFormat(String)}.
   **/
  void setFormat(String format)
  {
    super.setFormat(toFormat(format));
  }


  // Method used by DateFieldDescription.
  /**
   Sets the separator character of this AS400Date object.
   @param separator  The separator character.
   A null value indicates "no separator".
   **/
  void setSeparator(Character separator)
  {
    super.setSeparator(separator);
  }


  // Overrides non-public method of superclass, making it public.
  /**
   Sets the format of this AS400Date object.
   @param format The format for this object.
   For a list of valid values, refer to {@link #AS400Date(int,Character) AS400Date(int,Character)}.
   @param separator  The separator character.
   For a list of valid values, refer to {@link #AS400Date(int,Character) AS400Date(int,Character)}.
   A null value indicates "no separator".
   Refer to the IBM i programming reference to determine which separator characters are valid with each format.
   **/
  public void setFormat(int format, Character separator)
  {
    super.setFormat(format, separator);
  }


  /**
   Sets the format of this AS400Date object.
   @param format The format for this object.
   For a list of valid values, refer to {@link #AS400Date(int,Character) AS400Date(int,Character)}.
   @param separator  The separator character.
   @deprecated Use {@link #setFormat(int,Character) setFormat(int,Character)} instead.
   **/
  public void setFormat(int format, char separator)
  {
    super.setFormat(format, new Character(separator));
  }

  private static Hashtable getFormatsMap()
  {
    if (formatsMap_ == null) {
      synchronized (AS400Date.class) {
        if (formatsMap_ == null)
        {
          formatsMap_ = new Hashtable(12);
          formatsMap_.put("MDY",     new Integer(FORMAT_MDY));
          formatsMap_.put("DMY",     new Integer(FORMAT_DMY));
          formatsMap_.put("YMD",     new Integer(FORMAT_YMD));
          formatsMap_.put("JUL",     new Integer(FORMAT_JUL));
          formatsMap_.put("ISO",     new Integer(FORMAT_ISO));
          formatsMap_.put("USA",     new Integer(FORMAT_USA));
          formatsMap_.put("EUR",     new Integer(FORMAT_EUR));
          formatsMap_.put("JIS",     new Integer(FORMAT_JIS));
          formatsMap_.put("CYMD",    new Integer(FORMAT_CYMD));
          formatsMap_.put("CMDY",    new Integer(FORMAT_CMDY));
          formatsMap_.put("CDMY",    new Integer(FORMAT_CDMY));
          formatsMap_.put("LONGJUL", new Integer(FORMAT_LONGJUL));
          formatsMap_.put("MY",      new Integer(FORMAT_MY));
          formatsMap_.put("YM",      new Integer(FORMAT_YM));
          formatsMap_.put("MYY",     new Integer(FORMAT_MYY));
          formatsMap_.put("YYM",     new Integer(FORMAT_YYM));
        }
      }
    }
    return formatsMap_;
  }


  /**
   Returns the integer format value that corresponds to specified format name.
   If null is specified, the default format (FORMAT_ISO) is returned.
   This method is provided for use by the PCML infrastructure.
   @param formatName  The date format name.
   <br>Valid values are:
   <tt>
   <ul>
   <li>MDY
   <li>DMY
   <li>YMD
   <li>JUL
   <li>ISO
   <li>USA
   <li>EUR
   <li>JIS
   <li>CYMD
   <li>CMDY
   <li>CDMY
   <li>LONGJUL
   <li>MY
   <li>YM
   <li>MYY
   <li>YYM
   </ul>
   </tt>
   @return the format value.  For example, if formatName is "ISO", then {@link #FORMAT_ISO FORMAT_ISO} is returned.
   **/
  public static int toFormat(String formatName)
  {
    if (formatName == null || formatName.length() == 0) {
      if (Trace.traceOn_) {
        Trace.log(Trace.DIAGNOSTIC, "AS400Date.toFormat("+formatName+"): Returning default date format.");
      }
      return FORMAT_ISO;
    }

    Integer formatInt = (Integer)getFormatsMap().get(formatName.trim().toUpperCase());

    if (formatInt == null) {
      throw new ExtendedIllegalArgumentException("format ("+formatName+")", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID);
    }

    return formatInt.intValue();
  }

  static boolean isYearWithinRange(int year, int format)
  {
    // Valid ranges for 'year' values:
    //     2-digit years (YMD,DMY,MDY,JUL,MY,YM):           1940 to 2039
    //     3-digit years (CYMD,CDMY,CMDY):                  1900 to 2899
    //     4-digit years (ISO,USA,EUR,JIS,LONGJUL,MYY,YYM): 0001 to 9999
    switch (format)
    {
      case FORMAT_MDY:
      case FORMAT_DMY:
      case FORMAT_YMD:
      case FORMAT_JUL:
      case FORMAT_MY:
      case FORMAT_YM:
        if (year < 1940 || year > 2039) return false;
        else return true;

      case FORMAT_CYMD:
      case FORMAT_CMDY:
      case FORMAT_CDMY:
        if (year < 1900 || year > 2899) return false;
        else return true;

      case FORMAT_ISO:
      case FORMAT_USA:
      case FORMAT_EUR:
      case FORMAT_JIS:
      case FORMAT_LONGJUL:
      case FORMAT_MYY:
      case FORMAT_YYM:
        if (year < 1 || year > 9999) return false;
        else return true;

      default:  // should never happen
        throw new InternalErrorException(InternalErrorException.UNKNOWN, "Unrecognized format: " + format, null);
    }
  }

  // Overrides method of superclass.  This allows us to be more specific in the javadoc.
  /**
   Converts the specified Java object into IBM i format in the specified byte array.
   @param javaValue The object corresponding to the data type.  It must be an instance of {@link java.sql.Date java.sql.Date}.  Hours, minutes, seconds, and milliseconds are disregarded.
   @param as400Value The array to receive the data type in IBM i format.  There must be enough space to hold the IBM i value.
   @param offset The offset into the byte array for the start of the IBM i value.  It must be greater than or equal to zero.
   @return The number of bytes in the IBM i representation of the data type.
   **/
  public int toBytes(Object javaValue, byte[] as400Value, int offset)
  {
    return super.toBytes(javaValue, as400Value, offset);
  }

  // Implements abstract method of superclass.
  /**
   Converts the specified IBM i data type to a Java object.
   @param as400Value The array containing the data type in IBM i format.  The entire data type must be represented.
   @param offset The offset into the byte array for the start of the IBM i value. It must be greater than or equal to zero.
   @return A {@link java.sql.Date java.sql.Date} object corresponding to the data type.
   The reference time zone for the object is GMT.
   **/
  public Object toObject(byte[] as400Value, int offset)
  {
    if (as400Value == null) throw new NullPointerException("as400Value");
    String dateString = getCharConverter().byteArrayToString(as400Value, offset, getLength());
    // Parse the string, and create a java.sql.Date object.
    return parse(dateString);
  }


  // Implements abstract method of superclass.
  /**
   Converts the specified Java object into a String representation that is consistent with the format of this data type.
   @param javaValue The object corresponding to the data type. This must be an instance of {@link java.sql.Date java.sql.Date}, and must be within the range specifiable by this data type.
   @return A String representation of the specified value, formatted appropriately for this data type.
   **/
  public String toString(Object javaValue)
  {
    if (javaValue == null) throw new NullPointerException("javaValue");
    java.sql.Date dateObj;
    try { dateObj = (java.sql.Date)javaValue; }
    catch (ClassCastException e) {
      Trace.log(Trace.ERROR, "javaValue is of type " + javaValue.getClass().getName());
      throw e;
    }

    // Verify that the 'year' value from the date is within the range of our format.
    int year, era;
    synchronized (this) {
      Calendar cal = getCalendar(dateObj);
      year = cal.get(Calendar.YEAR);
      era  = cal.get(Calendar.ERA);
    }
    if (era == 0) {  // we can't represent years BCE
      throw new ExtendedIllegalArgumentException("javaValue (era=0)", ExtendedIllegalArgumentException.RANGE_NOT_VALID);
    }
    if (!isYearWithinRange(year, getFormat())) {
      Trace.log(Trace.ERROR, "Year " + year + " is outside the range of values for AS400Date format " + getFormat());
      throw new ExtendedIllegalArgumentException("javaValue (year=" + year + ")", ExtendedIllegalArgumentException.RANGE_NOT_VALID);
    }

    String dateString = getDateFormatter().format(dateObj);

    // Depending on the format, prepend a "century" digit if needed.
    dateString = addCenturyDigit(dateString, dateObj);
    return dateString;
  }

  /**
   Converts a string representation of a date, to a Java object.
   @param source A date value expressed as a string in the format specified for this AS400Date object.
   @return A {@link java.sql.Date java.sql.Date} object representing the specified date.
   The reference time zone for the object is GMT.
   **/
  public java.sql.Date parse(String source)
  {
    if (source == null) throw new NullPointerException("source");
    try
    {
      // Some formats contain a century digit.
      // Deal with the 'century' digit, if the pattern for this format includes one.
      Integer centuryDigit = parseCenturyDigit(source, getFormat());
      if (centuryDigit != null) {
        // To avoid confusing SimpleDateFormat, remove the century digit. 
        source = source.substring(1);
      }
      else {
        // The formats MDY, DMY, YMD, and JUL represent the year as 2 digits.
        // For those formats, we need to deduce the century based on the 'yy' value.
        centuryDigit = disambiguateCentury(source);
      }
      SimpleDateFormat dateFormatter = getDateFormatter(centuryDigit);
      java.util.Date dateObj = dateFormatter.parse(source);
      return new java.sql.Date(dateObj.getTime());
    }
    catch (Exception e) {
      // Assume that the exception is because we got bad input.
      Trace.log(Trace.ERROR, e.getMessage(), source);
      Trace.log(Trace.ERROR, "Date string is expected to be in format: " + prependCentury(patternFor(getFormat(), getSeparator())));  // Prepend 'century' indicator if needed.
      throw new ExtendedIllegalArgumentException("source ("+source+")", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID);
    }
  }

  /**
  Converts the specified ISO representation of a date, to a Java object.
  This method is provided for use by the PCML infrastructure;
  in particular, when parsing 'init=' values for 'date' data elements.
  This assumes the reference timezone to be GMT
  @param source A date value expressed as a string in format <tt>yyyy-MM-dd</tt>.
  @return A {@link java.sql.Date java.sql.Date} object representing the specified date.
  **/
 public static java.sql.Date parseXsdString(String source)
 {
   return parseXsdString(source, AS400AbstractTime.TIMEZONE_GMT); 
 }

  
  /**
   Converts the specified ISO representation of a date, to a Java object.
   This method is provided for use by the PCML infrastructure;
   in particular, when parsing 'init=' values for 'date' data elements.
   @param source A date value expressed as a string in format <tt>yyyy-MM-dd</tt>.
   * @param timeZone 
   @return A {@link java.sql.Date java.sql.Date} object representing the specified date.
   The reference time zone must be passed as a parameter. 
   **/
  public static java.sql.Date parseXsdString(String source, TimeZone timeZone)
  {
    if (source == null) throw new NullPointerException("source");
    try
    {
      java.util.Date simpleDateObj = getDateFormatterXSD(timeZone).parse(source );
      return new java.sql.Date(simpleDateObj.getTime());
    }
    catch (ParseException e) {
      // Assume that the exception is because we got bad input.
      Trace.log(Trace.ERROR, e.getMessage(), source);
      Trace.log(Trace.ERROR, "Value is expected to be in standard XML Schema 'date' format: " + DATE_PATTERN_XSD);
      throw new ExtendedIllegalArgumentException("source ("+source+")", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID);
    }
  }

  /**
  Converts the specified Java object into an XML Schema string representation.
  This method is provided for use by the PCML infrastructure.
  This method assumes the use of the GMT timezone. 
  @param javaValue The object corresponding to the data type. This must be an instance of 
                   {@link java.sql.Date java.sql.Date}, 
                   and must be within the range specifiable by this data type.
  @return The date expressed as a string in format <tt>yyyy-MM-dd</tt>.
  **/
 public static String toXsdString(Object javaValue)
 {
   return toXsdString(javaValue, TIMEZONE_GMT); 
 } 
  
  /**
   Converts the specified Java object into an XML Schema string representation.
   This method is provided for use by the PCML infrastructure.
   @param javaValue The object corresponding to the data type. This must be an instance of 
                    {@link java.sql.Date java.sql.Date}, 
                    and must be within the range specifiable by this data type.
   @param timeZone The timezone used to evaluate the string. 
   @return The date expressed as a string in format <tt>yyyy-MM-dd</tt>.
   **/
  public static String toXsdString(Object javaValue, TimeZone timeZone)
  {
    if (javaValue == null) throw new NullPointerException("javaValue");
    java.sql.Date dateObj;
    try { dateObj = (java.sql.Date)javaValue; }
    catch (ClassCastException e) {
      Trace.log(Trace.ERROR, "javaValue is of type " + javaValue.getClass().getName());
      throw e;
    }

    return getDateFormatterXSD(timeZone).format(dateObj);
  }


  // If the format includes a "century" digit, prepend a 'C' to the pattern string.
  private String prependCentury(String pattern)
  {
    switch (getFormat())
    {
      case FORMAT_CYMD:
      case FORMAT_CMDY:
      case FORMAT_CDMY:
        return "C"+pattern;

      default:
        return pattern;
    }
  }


  // If the format includes a "century" digit, prepend the appropriate century value.
  private String addCenturyDigit(String dateString, java.sql.Date dateObj)
  {
    switch (getFormat())
    {
      case FORMAT_CYMD:
      case FORMAT_CMDY:
      case FORMAT_CDMY:
        int year = getCalendar(dateObj).get(Calendar.YEAR);
        int century = (year/100) - 19;  // IBM i convention: Years '19xx' are in century '0'
        // Assume that the caller has verified that date is within our year range, and that the era is not BCE.
        return Integer.toString(century) + dateString;

      default:
        return dateString;  // nothing to prepend, so return string unaltered
    }
  }


  // Determines the century, if the specified format has only a 2-digit year and no century digit.
  // Returns null if the century is unambiguous in the specified format.
  private Integer disambiguateCentury(String dateString)
  {
    int offsetToYear;  // offset within dateString, to the 'yy'
    switch (getFormat())
    {
      case FORMAT_YMD:                                // yy/mm/dd
      case FORMAT_JUL:                                // yy/ddd
      case FORMAT_YM:                                 // yy/mm 
        offsetToYear = 0;
        break;

      case FORMAT_MY:                                 // mm/yy
        if (getSeparator() == null) offsetToYear = 2;
        else                        offsetToYear = 3;
        break;

      case FORMAT_MDY:                                // mm/dd/yy
      case FORMAT_DMY:                                // dd/mm/yy
        if (getSeparator() == null) offsetToYear = 4;
        else                        offsetToYear = 6;
        break;

      default:
        return null;  // other formats don't have a 'century' digit
    }

    // Range of years representable in the above 2-digit year formats: 1940-2039
    int year, century;
    year = Integer.parseInt(dateString.substring(offsetToYear, offsetToYear+2));
    if (year == 0)      century = 0;   // century 0 is years 1901-2000
    else if (year < 40) century = 1;   // century 1 is years 2001-2100
    else                century = 0;   // century 0 is years 1901-2000

    return new Integer(century);
  }


  // Parses the leading 'century' digit, if the specified format contains one.
  // Returns null if the format has no century digit.
  static Integer parseCenturyDigit(String dateString, int format)
  {
    switch (format)
    {
      case FORMAT_CYMD:
      case FORMAT_CMDY:
      case FORMAT_CDMY:
        return Integer.valueOf(Character.toString(dateString.charAt(0)));

      default:
        return null;  // other formats don't have a 'century' digit
    }
  }


  // Implements abstract method of superclass.
  String patternFor(int format, Character separator)
  {
    String sep = ( separator == null ? "" : separator.toString());
    switch (format)
    {
      case FORMAT_MY:       return "MM"+sep+"yy";
      case FORMAT_YM:       return "yy"+sep+"MM";
      case FORMAT_MYY:      return "MM"+sep+"yyyy";
      case FORMAT_YYM:      return "yyyy"+sep+"MM";

      case FORMAT_MDY:      return "MM"+sep+"dd"+sep+"yy";
      case FORMAT_DMY:      return "dd"+sep+"MM"+sep+"yy";
      case FORMAT_YMD:      return "yy"+sep+"MM"+sep+"dd";
      case FORMAT_JUL:      return "yy"+sep+"DDD";

      case FORMAT_ISO:
      case FORMAT_JIS:      return "yyyy"+sep+"MM"+sep+"dd";
      case FORMAT_USA:      return "MM"+sep+"dd"+sep+"yyyy";
      case FORMAT_EUR:      return "dd"+sep+"MM"+sep+"yyyy";

      // For the following "FORMAT_C..." formats, we deal with the century digit separately.
      // SimpleDateFormat doesn't have a "century digit" pattern.
      case FORMAT_CYMD:     return "yy"+sep+"MM"+sep+"dd";
      case FORMAT_CMDY:     return "MM"+sep+"dd"+sep+"yy";
      case FORMAT_CDMY:     return "dd"+sep+"MM"+sep+"yy";

      case FORMAT_LONGJUL:  return "yyyy"+sep+"DDD";

      default:  // should never happen
        throw new InternalErrorException(InternalErrorException.UNKNOWN, "Unrecognized format: " + format, null);
    }
  }


  // Implements abstract method of superclass.
  Character defaultSeparatorFor(int format)
  {
    if (!isValidFormat(format)) {
      throw new ExtendedIllegalArgumentException("format ("+format+")", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID);
    }
    switch (format)
    {
      case FORMAT_MDY:
      case FORMAT_DMY:
      case FORMAT_YMD:
      case FORMAT_JUL:
      case FORMAT_USA:
      case FORMAT_CYMD:
      case FORMAT_CMDY:
      case FORMAT_CDMY:
      case FORMAT_LONGJUL:
      case FORMAT_MY:
      case FORMAT_YM:
      case FORMAT_MYY:
      case FORMAT_YYM:
        return SLASH;  // '/'

      case FORMAT_ISO:
      case FORMAT_JIS:
        return HYPHEN;  // '-'

      case FORMAT_EUR:
        return PERIOD;  // '.'

      default:  // should never happen
        throw new InternalErrorException(InternalErrorException.UNKNOWN, "Unrecognized format: " + format, null);
    }
  }

  // Implements abstract method of superclass.
  boolean isValidFormat(int format)
  {
    return validateFormat(format);
  }

  /**
   Validates the specified format value.
   This method is provided for use by the PCML infrastructure.
   @param format The format.
   For a list of valid values, refer to {@link #AS400Date(int,Character) AS400Date(int,Character)}.
   @return true if the format is valid; false otherwise.
   **/
  public static boolean validateFormat(int format)
  {
    if (format < FORMAT_RANGE_MINIMUM || format > FORMAT_RANGE_MAXIMUM) return false;
    else return true;
  }


  /**
   Returns the number of bytes occupied on the IBM i system by a field of this type.
   This method is provided for use by the PCML infrastructure.
   @param format The format.
   For a list of valid values, refer to {@link #AS400Date(int,Character) AS400Date(int,Character)}.
   @param separator The separator character.
   For a list of valid values, refer to {@link #AS400Date(int,Character) AS400Date(int,Character)}.
   @return the number of bytes occupied.
   **/
  public static int getByteLength(int format, Character separator)
  {
    if (separator == null)  // the pattern contains no separators
    {
      switch (format)
      {
        case FORMAT_MY:
        case FORMAT_YM:
          return 4;

        case FORMAT_MDY:
        case FORMAT_DMY:
        case FORMAT_YMD:
        case FORMAT_MYY:
        case FORMAT_YYM:
          return 6;

        case FORMAT_LONGJUL:
          return 7;

        case FORMAT_JUL:
          return 5;

        case FORMAT_ISO:
        case FORMAT_USA:
        case FORMAT_EUR:
        case FORMAT_JIS:
          return 8;

        case FORMAT_CYMD:
        case FORMAT_CMDY:
        case FORMAT_CDMY:
          return 7;

        default:  // should never happen
          throw new InternalErrorException(InternalErrorException.UNKNOWN, "Unrecognized format: " + format, null);
      }
    }
    else  // the pattern contains separator(s)
    {
      switch (format)
      {
        case FORMAT_MY:
        case FORMAT_YM:
          return 5;

        case FORMAT_JUL:
          return 6;

        case FORMAT_MYY:
        case FORMAT_YYM:
          return 7;

        case FORMAT_MDY:
        case FORMAT_DMY:
        case FORMAT_YMD:
        case FORMAT_LONGJUL:
          return 8;

        case FORMAT_ISO:
        case FORMAT_USA:
        case FORMAT_EUR:
        case FORMAT_JIS:
          return 10;

        case FORMAT_CYMD:
        case FORMAT_CMDY:
        case FORMAT_CDMY:
          return 9;

        default:  // should never happen
          throw new InternalErrorException(InternalErrorException.UNKNOWN, "Unrecognized format: " + format,null);
      }
    }
  }


  // Implements abstract method of superclass.
  int lengthFor(int format)
  {
    return getByteLength(format, getSeparator());
  }

}
