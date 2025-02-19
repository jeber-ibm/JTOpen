///////////////////////////////////////////////////////////////////////////////
//
//JTOpen (IBM Toolbox for Java - OSS version)
//
//Filename: SQLDecFloat16.java
//
//The source code contained herein is licensed under the IBM Public License
//Version 1.0, which has been approved by the Open Source Initiative.
//Copyright (C) 2006 International Business Machines Corporation and
//others. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Blob;
import java.sql.Date;
/* ifdef JDBC40 */
import java.sql.RowId;
/* endif */ 
import java.sql.SQLException;
/* ifdef JDBC40 */
import java.sql.SQLXML;
/* endif */ 
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

//@DFA new class
final class SQLDecFloat16 extends SQLDataBase {
   
    private static final BigDecimal default_ = BigDecimal.valueOf(0);  

    //various min/max values used for comparisons in calculating truncation etc
    private static final BigDecimal BYTE_MAX_VALUE = BigDecimal.valueOf(Byte.MAX_VALUE);

    private static final BigDecimal BYTE_MIN_VALUE = BigDecimal.valueOf(Byte.MIN_VALUE);

    private static final BigDecimal SHORT_MAX_VALUE = BigDecimal.valueOf(Short.MAX_VALUE);

    private static final BigDecimal SHORT_MIN_VALUE = BigDecimal.valueOf(Short.MIN_VALUE);

    private static final BigDecimal INTEGER_MAX_VALUE = BigDecimal.valueOf(Integer.MAX_VALUE);

    private static final BigDecimal INTEGER_MIN_VALUE = BigDecimal.valueOf(Integer.MIN_VALUE);

    private static final BigDecimal LONG_MAX_VALUE = BigDecimal.valueOf(Long.MAX_VALUE);

    private static final BigDecimal LONG_MIN_VALUE = BigDecimal.valueOf(Long.MIN_VALUE);

    private static final BigDecimal FLOAT_MAX_VALUE = new BigDecimal(Float.MAX_VALUE);

    private static final BigDecimal FLOAT_MIN_VALUE = new BigDecimal(-Float.MAX_VALUE); //@PDC MIN_VALUE is positive

    private static final BigDecimal DOUBLE_MAX_VALUE = new BigDecimal(Double.MAX_VALUE);

    private static final BigDecimal DOUBLE_MIN_VALUE = new BigDecimal(-Double.MAX_VALUE); //@PDC MIN_VALUE is positive
    
    private static final BigDecimal DECFLOAT16_MAX_VALUE = new BigDecimal("9.999999999999999E384"); 
    
    private static final BigDecimal DECFLOAT16_MIN_VALUE = new BigDecimal("-9.999999999999999E384"); 
    
    static final int DECFLOAT16_MIN_EXP = -383;  


    private int precision_;  //16
       
    
    String specialValue_ = null; //since BigDecimal cannot hold "Infinity", "-Infinity" or "NaN", store them here as string

    private AS400DecFloat typeConverter_;

    private BigDecimal value_;

    private JDProperties properties_;

    private RoundingMode roundingMode;
    
    private String roundingModeStr;
    
    private int vrm_;

    SQLDecFloat16( SQLConversionSettings settings, int vrm, JDProperties properties) {
      super(settings);
        precision_ = 16;
        roundingModeStr = properties.getString(JDProperties.DECFLOAT_ROUNDING_MODE);
        typeConverter_ = new AS400DecFloat(precision_ );
        value_ = default_;  
        vrm_ = vrm;  
        properties_ = properties;  
         
        //parse property rounding mode and save its value as int (needed for BigDecimal, MathContext etc)
        // valid rounding modes are:
        //"half even"
        //"half up"
        //"down"
        //"ceiling"
        //"floor"
        //"half down"
        //"up"

        switch (roundingModeStr) {
            case JDProperties.DECFLOAT_ROUNDING_MODE_UP:
                roundingMode = RoundingMode.UP;
                break;
            case JDProperties.DECFLOAT_ROUNDING_MODE_DOWN:
                roundingMode = RoundingMode.DOWN;
                break;
            case JDProperties.DECFLOAT_ROUNDING_MODE_CEILING:
                roundingMode = RoundingMode.CEILING;
                break;
            case JDProperties.DECFLOAT_ROUNDING_MODE_FLOOR:
                roundingMode = RoundingMode.FLOOR;
                break;
            case JDProperties.DECFLOAT_ROUNDING_MODE_HALF_UP:
                roundingMode = RoundingMode.HALF_UP;
                break;
            case JDProperties.DECFLOAT_ROUNDING_MODE_HALF_DOWN:
                roundingMode = RoundingMode.HALF_DOWN;
                break;
            case JDProperties.DECFLOAT_ROUNDING_MODE_HALF_EVEN:
                roundingMode = RoundingMode.HALF_EVEN;
                break;
            default:
                break;
        }
       
        //for MathContext, methods take strings which are same as JDProperties rounding modes with "round" added.
        roundingModeStr = "ROUND_" + roundingModeStr.toUpperCase().replace(' ', '_');
            
    }

    public Object clone() {
        return new SQLDecFloat16( settings_, vrm_, properties_); 
    }

    //---------------------------------------------------------//
    //                                                         //
    // CONVERSION TO AND FROM RAW BYTES //
    //                                                         //
    //---------------------------------------------------------//

    public void convertFromRawBytes(byte[] rawBytes, int offset, ConvTable ccisdConverter, boolean ignoreConversionErrors) 
            throws SQLException {
        try{
            value_ = ((BigDecimal) typeConverter_.toObject(rawBytes, offset));
            specialValue_ = null;
        } catch (ExtendedIllegalArgumentException e)  {
          
            //check for NAN and INF flag exception
            if ( (specialValue_ = getSpecialValue( e.toString())) == null ) {
              if (!ignoreConversionErrors) {   /*@Q2A*/
                throw e; 
              }
          }
        }
    }

    public void convertToRawBytes(byte[] rawBytes, int offset, ConvTable ccsidConverter) 
            throws SQLException {
        try {
            if ( specialValue_ == null)
                typeConverter_.toBytes(value_, rawBytes, offset);
            else
                typeConverter_.toBytes(specialValue_, rawBytes, offset); //Nan, Infinity, -Infinity
            
        } catch (ExtendedIllegalArgumentException e) {
            JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH, e);
        }
    }

    //---------------------------------------------------------//
    //                                                         //
    // SET METHODS //
    //                                                         //
    //---------------------------------------------------------//

    public void set(Object object, Calendar calendar, int scale) throws SQLException {
        BigDecimal bigDecimal = null;
        specialValue_ = null;
        
        if (object instanceof String) {
            try {
                if ( (specialValue_ = getSpecialValue( (String)object)) == null ) {
                    //not one of the special values, just store as BigDecimal
                	
                    // Because the string may be using a comma for the decimal separator and we are going to
                    // store the object as a Java Bigdec, we much check and change to the default '.' notation.
                    if (((String)object).indexOf(',')!=-1) { 
                      bigDecimal = new BigDecimal (((String)object).replace(',', '.'));
                    } else { 
                      bigDecimal = new BigDecimal((String)object);
                    }
                }
            } catch (NumberFormatException e) {
                JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH, e);
            } catch (StringIndexOutOfBoundsException e) // jdk 1.3.x throws this
                                                        // instead of a NFE
            {
                JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
            }
        }

        else if (object instanceof Number) {
            if ( (specialValue_ = getSpecialValue( object.toString())) == null ) {
                //not one of the special values, just store as BigDecimal
                bigDecimal = new BigDecimal(object.toString());  //convert to string so all Number types can be passed in
            }
        }

        else if (object instanceof Boolean)
            bigDecimal = (((Boolean) object).booleanValue() == true) ? BigDecimal.valueOf(1) : BigDecimal.valueOf(0);

        else { 
          if (JDTrace.isTraceOn()) {
              if (object == null) { 
                  JDTrace.logInformation(this, "Unable to assign null object");
                } else { 
                    JDTrace.logInformation(this, "Unable to assign object("+object+") of class("+object.getClass().toString()+")");
                }
          }

            JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        }
        // Round by mode if necessary.   
        truncated_ = 0; outOfBounds_ = false; 
        
        //if one of the special values, then no need to check truncation
        if(specialValue_ != null)
        {
            //Nan, Infinity, -Infinity
            value_ = null;
            return;
        }
  
        //follow native and allow rounding mode to handle
        //@pdd int otherScale = bigDecimal.scale();
        //@pdd if(otherScale > ((-1) * DECFLOAT16_MIN_EXP)) //absolute of min_exp is max scale
        //@pdd truncated_ += otherScale + DECFLOAT16_MIN_EXP; //diff in scales
        
        //get precision from bigDecimal without 0's on right side   
        //@pdd int otherPrecision =  SQLDataFactory.getPrecisionForTruncation(bigDecimal, 16);
        
        
        //follow native and allow rounding mode to handle
        //@pddif(otherPrecision > precision_)
        //@pdd{
        //@pdd    int digits = otherPrecision - precision_;
        //@pdd    truncated_ += digits;
        //@pdd}
        //@pddelse                                                             
        //@pdd    truncated_ = 0; outOfBounds_ = false;   // No left side truncation, report nothing       
        //@pdd                     // (even if there was right side truncation).     
 
        value_ = AS400DecFloat.roundByMode(bigDecimal, 16, roundingMode);
        
        // Check the value is within range
        if (value_.compareTo(DECFLOAT16_MAX_VALUE) > 0) {
          truncated_ = 1;  outOfBounds_ = true; value_ = DECFLOAT16_MAX_VALUE;
        } else if (value_.compareTo(DECFLOAT16_MIN_VALUE) < 0) {
          truncated_ = 1;  outOfBounds_ = true; value_ = DECFLOAT16_MIN_VALUE;
        }
        
        
    }

    //---------------------------------------------------------//
    //                                                         //
    // DESCRIPTION OF SQL TYPE //
    //                                                         //
    //---------------------------------------------------------//

    public int getSQLType() {
        return SQLData.DECFLOAT;
    }

    public String getCreateParameters() {
        return null;
    }

    public int getDisplaySize() {
        return 23; //@pdc same as native
    }

    
    public String getJavaClassName() {
        return "java.math.BigDecimal";
    }

    public String getLiteralPrefix() {
        return null;
    }

    public String getLiteralSuffix() {
        return null;
    }

    public String getLocalName() {
        return "DECFLOAT";
    }

    public int getMaximumPrecision() {
        return precision_;
    }

    public int getMaximumScale() {
        return 0;
    }

    public int getMinimumScale() {
        return 0;
    }

    public int getNativeType() {
        return 996;  //same as in SQLDataFactory.java 
    }

    public int getPrecision() {
        return precision_;
    }

    public int getRadix() {
        return 10;  //decimal base (4 bits per digit)
    }

    public int getScale() {
        return 0;
    }

    public int getType() {
        return java.sql.Types.OTHER;         
    }

    public String getTypeName() {
        return "DECFLOAT";
    }

    public boolean isSigned() {
        return true;
    }

    public boolean isText() {
        return false;
    }

    public int getActualSize() {
        if(specialValue_ == null)
            return SQLDataFactory.getPrecision( value_ );
        else
            return specialValue_.length();
    }

    public int getTruncated() {
        return truncated_;
    }
    public boolean getOutOfBounds() {
      return outOfBounds_; 
    }

    //---------------------------------------------------------//
    //                                                         //
    // CONVERSIONS TO JAVA TYPES //
    //                                                         //
    //---------------------------------------------------------//


    public BigDecimal getBigDecimal(int scale) throws SQLException {
        truncated_ = 0; outOfBounds_ = false; 
        
        //NaN, Infinity, -Infinity
        //remove this when BigDecimal supports Nan, Inf, -Inf
        if(specialValue_ != null){
            JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
            return null;
        }
        
        if(scale >= 0)
        {
            if(scale >= value_.scale())
            {
                return value_.setScale(scale);
            }
            else
            {
                truncated_ = value_.scale() - scale;
                return value_.setScale(scale, roundingMode);
            }
        }
        else
            return value_;
    }

    public InputStream getBinaryStream() throws SQLException {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public Blob getBlob() throws SQLException {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public boolean getBoolean() throws SQLException {
        truncated_ = 0; outOfBounds_ = false; 
        
        //NaN, Infinity, -Infinity
        if(specialValue_ != null){
            return false; //false seems logical here
        }
        
        return (value_.compareTo(BigDecimal.valueOf(0)) != 0);
    }

    public byte getByte() throws SQLException {
        //this code is similar to SQLDouble, and then in inner iff, it is like
        // SQLDecimal
        truncated_ = 0; outOfBounds_ = false; 
        
        //NaN, Infinity, -Infinity
        if(specialValue_ != null){
          //@snan snan is not yet supported, return as regular nan
            if(specialValue_.indexOf("-SNaN") != -1)  //@snan
                return (Double.valueOf("-NaN")).byteValue();
            else if(specialValue_.indexOf("SNaN") != -1)  //@snan
                return (Double.valueOf("NaN")).byteValue();
            else 
                return (Double.valueOf(specialValue_)).byteValue();
        }

        if (value_.compareTo(BYTE_MAX_VALUE) > 0 || value_.compareTo(BYTE_MIN_VALUE) < 0) {
            if (value_.compareTo(SHORT_MAX_VALUE) > 0 || value_.compareTo(SHORT_MIN_VALUE) < 0) {
                if (value_.compareTo(INTEGER_MAX_VALUE) > 0 || value_.compareTo(INTEGER_MIN_VALUE) < 0) {
                    if (value_.compareTo(LONG_MAX_VALUE) > 0 || value_.compareTo(LONG_MIN_VALUE) < 0) {
                        truncated_ = 15;  //16 bytes - 1;
                        outOfBounds_ = true; 
                    } else {
                        truncated_ = 7; outOfBounds_ = true;
                    }
                } else {
                    truncated_ = 3;  outOfBounds_ = true; 
                }
            } else {
                truncated_ = 1;     outOfBounds_ = true; 
            }
        }
        return value_.byteValue();
    }

    public byte[] getBytes() throws SQLException {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }


    public Date getDate(Calendar calendar) throws SQLException {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public double getDouble() throws SQLException {
        truncated_ = 0; outOfBounds_ = false; 

        //NaN, Infinity, -Infinity
        if(specialValue_ != null){
            //@snan snan is not yet supported, return as regular nan
            if(specialValue_.indexOf("-SNaN") != -1)  //@snan
                return Double.parseDouble("-NaN");
            else if(specialValue_.indexOf("SNaN") != -1)  //@snan
                return Double.parseDouble("NaN");
            else 
                return Double.parseDouble(specialValue_);
        }
        
        if (value_.compareTo(DOUBLE_MAX_VALUE) > 0 || value_.compareTo(DOUBLE_MIN_VALUE) < 0) {
            truncated_ = 8;  //16 bytes - 8;
            outOfBounds_=true;
        }

        return value_.doubleValue();
    }

    public float getFloat() throws SQLException {
        truncated_ = 0; outOfBounds_ = false; 
        
        //NaN, Infinity, -Infinity
        if(specialValue_ != null){
            //@snan snan is not yet supported, return as regular nan
            if(specialValue_.indexOf("-SNaN") != -1)  //@snan
                return Float.parseFloat("-NaN");
            else if(specialValue_.indexOf("SNaN") != -1)  //@snan
                return Float.parseFloat("NaN");
            else
                return Float.parseFloat(specialValue_);
        }
        
        if (value_.compareTo(FLOAT_MAX_VALUE) > 0 || value_.compareTo(FLOAT_MIN_VALUE) < 0) {
            if (value_.compareTo(DOUBLE_MAX_VALUE) > 0 || value_.compareTo(DOUBLE_MIN_VALUE) < 0) {
                truncated_ = 12;  //16 bytes - 4;
            } else {
                truncated_ = 4;
            }
        }
        return value_.floatValue();
    }

    public int getInt() throws SQLException {
        truncated_ = 0; outOfBounds_ = false; 

        //NaN, Infinity, -Infinity
        //remove this when Integer supports Nan, Inf, -Inf
        if(specialValue_ != null){
            JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        }
        
        if (value_.compareTo(INTEGER_MAX_VALUE) > 0 || value_.compareTo(INTEGER_MIN_VALUE) < 0) {
            if (value_.compareTo(LONG_MAX_VALUE) > 0 || value_.compareTo(LONG_MIN_VALUE) < 0) {
                truncated_ = 12;  //16 bytes - 4;
                outOfBounds_=true;
            } else {
                truncated_ = 4; outOfBounds_=true;
            }
        }

        return value_.intValue();
    }

    public long getLong() throws SQLException {
        truncated_ = 0; outOfBounds_ = false; 
        
        //NaN, Infinity, -Infinity
        //remove this when Long supports Nan, Inf, -Inf
        if(specialValue_ != null){
            JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        }
        
        if( value_.compareTo(LONG_MAX_VALUE) > 0 || value_.compareTo(LONG_MIN_VALUE) < 0)
        {
            truncated_ = 8;  //16 bytes - 8;
            outOfBounds_=true;
        }
        return value_.longValue();
    }

    public Object getObject() throws SQLException {
        truncated_ = 0; outOfBounds_ = false; 
        
        //NaN, Infinity, -Infinity
        //in this case, return String object instead of throwing exception
        //remove this when BigDecimal supports Nan, Inf, -Inf
        if(specialValue_ != null){
            return specialValue_;
        }
        
        return value_;
    }

    public short getShort() throws SQLException {
        truncated_ = 0; outOfBounds_ = false; 

        //NaN, Infinity, -Infinity
        if(specialValue_ != null){
            JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        }
        
        if (value_.compareTo(SHORT_MAX_VALUE) > 0 || value_.compareTo(SHORT_MIN_VALUE) < 0) {
            if (value_.compareTo(INTEGER_MAX_VALUE) > 0 || value_.compareTo(INTEGER_MIN_VALUE) < 0) {
                if (value_.compareTo(LONG_MAX_VALUE) > 0 || value_.compareTo(LONG_MIN_VALUE) < 0) {
                    truncated_ = 14;  //16 bytes - 2;
                    outOfBounds_=true;
                } else {
                    truncated_ = 6; outOfBounds_=true;
                }
            } else {
                truncated_ = 2; outOfBounds_=true;
            }
        }

        return value_.shortValue();
    }

    public String getString() throws SQLException {
        truncated_ = 0; outOfBounds_ = false; 
        
        //NaN, Infinity, -Infinity
        if(specialValue_ != null){
            return specialValue_;
        }
        
        String stringRep = value_.toString();
        int decimal = stringRep.indexOf('.');
        if(decimal == -1)
            return stringRep;
        else
            return stringRep.substring(0, decimal)
            + settings_.getDecimalSeparator()
            + stringRep.substring(decimal+1);
     }

    public Time getTime(Calendar calendar) throws SQLException {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public Timestamp getTimestamp(Calendar calendar) throws SQLException {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    
    /** Helper method to return string value of special values that
     *  cannot be held in current BigDecimal objects.
     *  Valid inputs = "NaN", "NAN", "+NaN", "-NaN", "QNaN", "+QNaN", "-QNaN", "SNaN", "+SNaN", "-SNaN", "INF", "+INF", 
     *  "-INF", "Infinity", "+Infinity", "-Infinity"
     */
    private String getSpecialValue(String number){
        //use indexOf() so that we can use this method for exception text from AS400DecFloat also
        int startOfValue = -1;
        //@snan
        if ( (startOfValue = number.toUpperCase().indexOf("SNAN")) != -1 ){ 
            //check for sign
            if ( ((startOfValue > 0 ) && (number.charAt(startOfValue - 1) == '-') ) )
                return "-SNaN"; //no representaion in Double
            else
                return "SNaN"; //no representaion in Double
        }
        else if ( (startOfValue = number.toUpperCase().indexOf("NAN")) != -1 ){ 
            //check for sign
            if ( ((startOfValue > 0 ) && (number.charAt(startOfValue - 1) == '-') )
                    || ((startOfValue > 1 ) && (number.charAt(startOfValue - 2) == '-')) )
                return "-NaN"; //no representaion in Double
            return String.valueOf(Double.NaN);
        }
        else if ( (startOfValue = number.toUpperCase().indexOf("INF")) != -1){
            //check for sign
            if ( (startOfValue != 0 ) && (number.charAt(startOfValue - 1) == '-'))
                return String.valueOf(Double.NEGATIVE_INFINITY);
            else
                return String.valueOf(Double.POSITIVE_INFINITY);
        }
        else
            return null; //not a special value
    }
    
    
    
    //@pda jdbc40
    public String getNString() throws SQLException
    {
       return getString(); 
       
    }

    //@pda jdbc40
/* ifdef JDBC40 */
    public RowId getRowId() throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }
/* endif */ 
    //@pda jdbc40
/* ifdef JDBC40 */
    public SQLXML getSQLXML() throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }
/* endif */ 
    // @array
    
    public void saveValue() {
      savedValue_ = value_; 
   }
}
