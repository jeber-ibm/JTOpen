///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                                 
//                                                                             
// Filename: ProfieTokenImplNative.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2003 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////


package com.ibm.as400.access;

import java.beans.PropertyVetoException;
import java.io.IOException;

import com.ibm.as400.security.auth.*;

/**
 * The ProfileTokenImplNative class provides an implementation for behavior 
 * delegated by a ProfileTokenCredential object.
 **/
public class ProfileTokenImplNative implements ProfileTokenImpl
{
    // Note: This class needs to be public since referenced by com.ibm.as400.security.auth.ProfileTokenCredential
    
    private static final String CLASSNAME = "com.ibm.as400.access.ProfileTokenImplNative";
    
    static
    {
        if (Trace.traceOn_) Trace.logLoadPath(CLASSNAME);
        NativeMethods.loadNativeLibraryQyjspart();
    }

    private AS400Credential credential_ = null;

    /**
     * Destroy or clear sensitive information maintained by the credential
     * implementation.
     * <p>
     * Subsequent requests may result in a NullPointerException.
     * <p>
     * This class will also attempt to remove the associated profile token from the
     * system.
     * 
     * @exception DestroyFailedException If errors occur while destroying or
     *                                   clearing credential data.
     **/
    @Override
    public void destroy() throws DestroyFailedException
    {
        nativeRemoveFromSystem(((ProfileTokenCredential)getCredential()).getToken());
        credential_ = null;
        if (Trace.isTraceOn()) Trace.log(Trace.INFORMATION, "Credential implementation destroyed >> " + toString());
    }

 
   /**
    * Generates and returns a new profile token based on
    * the provided information.
    *
    *
    * @param uid
    *   The name of the user profile for which the token
    *   is to be generated.
    *
    * @param pwd
    *   The user profile password.
    *
    * @param type
    *   The type of token.
    *   Possible types are defined as fields on the 
    *       ProfileTokenCredential class:
    *     <ul>
    *       <li>TYPE_SINGLE_USE
    *       <li>TYPE_MULTIPLE_USE_NON_RENEWABLE
    *       <li>TYPE_MULTIPLE_USE_RENEWABLE
    *     </ul>
    *   <p>
    *
    * @param timeoutInterval
    *    The number of seconds to expiration.
    *
    * @return
    *   The token bytes.
    *
    * @exception RetrieveFailedException
    *   If errors occur while generating the token.
    *
    */
    public byte[] generateToken(String uid, char[] pwd, int type, int timeoutInterval) throws RetrieveFailedException
    {
        if (pwd.length > 10)
        {
            Trace.log(Trace.ERROR, "User profile password exceeds allowed length");
            throw new ExtendedIllegalArgumentException("password", ExtendedIllegalArgumentException.LENGTH_NOT_VALID);
        }

        return nativeCreateTokenChar(uid.toUpperCase(), pwd, type, timeoutInterval);
    }

    @Override
    public byte[] generateToken(String uid, int pwdSpecialValue, int type, int timeoutInterval, boolean[] enhancedProfileToken) throws RetrieveFailedException {
        return generateToken(uid, pwdSpecialValue, null, AuthenticationIndicator.APPLICATION_AUTHENTICATION, null, null, 0, null, 0, type, timeoutInterval, enhancedProfileToken);
    }

    /**
     * Generate a token using an ID and pwdSpecial value
     * @param uid
     * @param pwdSpecialValue
     * @param additionalAuthenticationFactor
     * @param authenticationIndicator
     * @param verificationId
     * @param remoteIpAddress
     * @param remotePort
     * @param localIpAddress
     * @param localPort
     * @param type
     * @param timeoutInterval
     * @param enhancedProfileToken -- input/output one element boolean array. On input if the first element is false, then an enhancedProfileToken will not be created.
     *                                on output, the first element indicates if an enhancedProfileToken was created. 
     * @return
     * @throws RetrieveFailedException
     */
    private byte[] generateToken(String uid, int pwdSpecialValue, char[] additionalAuthenticationFactor, int authenticationIndicator, 
            String verificationId, String remoteIpAddress, int remotePort, String localIpAddress, int localPort,
            int type, int timeoutInterval, boolean[] enhancedProfileToken) throws RetrieveFailedException 
    {
    	
        // Convert password special value from int to string
        String pwdSpecialVal;
        switch(pwdSpecialValue)
        {
            case ProfileTokenCredential.PW_NOPWD:
                pwdSpecialVal = ProfileTokenImpl.PW_STR_NOPWD;
                break;
            case ProfileTokenCredential.PW_NOPWDCHK:
                pwdSpecialVal = ProfileTokenImpl.PW_STR_NOPWDCHK;
                break;
            default:
                Trace.log(Trace.ERROR, "Password special value = " +  pwdSpecialValue + " is not valid.");
                throw new ExtendedIllegalArgumentException("password special value", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID);
        }
        
        if (Trace.isTraceOn())  Trace.log(Trace.DIAGNOSTIC, "ProfileTokenImplNative generating profile token w/special value for user: " + uid);


        // Call native method and return token bytes, we rely on the fact this class is only called if running on AS400.
        if (!enhancedProfileToken[0] || !ProfileTokenCredential.useEnhancedProfileTokens() || AS400.nativeVRM.getVersionReleaseModification() <= 0x00070500) {
        	enhancedProfileToken[0]=false;   /* Let the caller know this is NOT an enhanced profile token */ 
            return nativeCreateTokenChar(uid.toUpperCase(), pwdSpecialVal.toCharArray(), type, timeoutInterval);
        } else {
        	enhancedProfileToken[0]=true;   /* Let the caller know this is an enhanced profile token */ 
        return EnhancedProfileTokenImplNative.nativeCreateTokenSpecialPassword(uid.toUpperCase(), pwdSpecialVal.toCharArray(), 
                additionalAuthenticationFactor, authenticationIndicator, verificationId, remoteIpAddress, remotePort, localIpAddress, localPort, 
                type, timeoutInterval);
        }
    }

    @Override
    public ProfileTokenCredential generateToken(String uid, int pwdSpecialValue, ProfileTokenCredential profileTokenCred)
            throws RetrieveFailedException, PropertyVetoException 
    {
    	boolean [] enhancedProfileToken = new boolean[1]; 
    	enhancedProfileToken[0] = true; 
        byte[] token = generateToken(uid, pwdSpecialValue, 
                profileTokenCred.getAdditionalAuthenticationFactor(), 
                profileTokenCred.getAuthenticationIndicator(),
                profileTokenCred.getVerificationID(),              
                profileTokenCred.getRemoteIPAddress(), 
                profileTokenCred.getRemotePort(),
                profileTokenCred.getLocalIPAddress(),  
                profileTokenCred.getLocalPort(),         
                profileTokenCred.getTokenType(), 
                profileTokenCred.getTimeoutInterval(),
                enhancedProfileToken);
        
        try {
            profileTokenCred.setToken(token, enhancedProfileToken[0]);
            profileTokenCred.setTokenCreator(ProfileTokenCredential.CREATOR_NATIVE_API);
        } 
        catch (PropertyVetoException e)
        {
            try {
                nativeRemoveFromSystem(token);
                credential_ = null;
            } catch (DestroyFailedException e1) {
                Trace.log(Trace.ERROR, "Unexpected Exception during profile token destroy: ", e);
            }
            
            throw e;
        }
        
        return profileTokenCred;
    }
    

    @Override
    public byte[] generateTokenExtended(String uid, char [] pwd, int type, int timeoutInterval, boolean[] isEnhancedToken) throws RetrieveFailedException {
        return  generateTokenExtended(uid, pwd, null, null, null, 0, null, 0, type, timeoutInterval,isEnhancedToken);
    }
    
    private byte[] generateTokenExtended(String uid, char[] pwd, char[] additionalAuthenticationFactor,
            String verificationId, String remoteIpAddress, int remotePort, String localIpAddress, int localPort,
            int type, int timeoutInterval, boolean[] isEnhancedToken) throws RetrieveFailedException
    {
    	if (Trace.isTraceOn()) {
    		String pwdInfo ="null"; 
    		if (pwd != null) pwdInfo = "char["+pwd.length+"]"; 
    		String aafInfo = "null"; 
    		if (additionalAuthenticationFactor != null) aafInfo="char["+additionalAuthenticationFactor.length+"]"; 
    		Trace.log(Trace.INFORMATION, this, "generateTokenExtended("+uid+","+pwdInfo+","+
    				aafInfo+","+verificationId+","+remoteIpAddress+","+
    				remotePort+","+localIpAddress+","+localPort+","+type+","+timeoutInterval+")"); 
    	}
        AS400 sys = getCredential().getSystem();
        
        // Determine if we are using enhanced profile tokens
        boolean useEPT = false;
        try {
            useEPT = isEnhancedToken[0] && (ProfileTokenCredential.useEnhancedProfileTokens() && sys.getVRM() > 0x00070500);
        }
        catch (AS400SecurityException|IOException e) {
            Trace.log(Trace.ERROR, "Unexpected Exception: ", e);
            throw new RetrieveFailedException();
        }
        
        // The API QSYGENPT requires all parameters to be non-null. 
        boolean isAAFNull = (additionalAuthenticationFactor == null || additionalAuthenticationFactor.length == 0);
        if (isAAFNull) additionalAuthenticationFactor = new char[] { ' ' };
        
        boolean isVfyIDNull = (verificationId == null || verificationId.length() == 0);
        if (isVfyIDNull) verificationId = " ";

        boolean isRemoteIPNull =  (remoteIpAddress == null || remoteIpAddress.length() == 0);
        if (isRemoteIPNull) remoteIpAddress = " ";

        boolean isLocalIPNull =  (localIpAddress == null || localIpAddress.length() == 0);
        if (isLocalIPNull) localIpAddress = " ";

        // Setup parameters
        ProgramParameter[] parmlist = new ProgramParameter[useEPT ? 19 : 8];
      
        // Output: Profile token.
        parmlist[0] = new ProgramParameter(ProfileTokenCredential.TOKEN_LENGTH);

        // Input: User profile name. Uppercase, get bytes (ccsid 37).
        try {
            parmlist[1] = new ProgramParameter(SignonConverter.stringToByteArray(uid.toUpperCase()));
        }
        catch (AS400SecurityException se) {
            throw new RetrieveFailedException(se.getReturnCode());
        }
        
        // Input: User password. String to char[], char[] to byte[] (unicode).
        parmlist[2] = new ProgramParameter(BinaryConverter.charArrayToByteArray(pwd));

        // Input: Time out interval. Int to byte[].
        parmlist[3] = new ProgramParameter(BinaryConverter.intToByteArray(timeoutInterval));

        // Input: Profile token type. Int to string, get bytes.
        parmlist[4] = new ProgramParameter(CharConverter.stringToByteArray(sys, Integer.toString(type)));

        // Input/output: Error code. NULL.
        parmlist[5] = new ProgramParameter(BinaryConverter.intToByteArray(0));

        // Input: Length of user password. Int to byte[].
        parmlist[6] = new ProgramParameter(BinaryConverter.intToByteArray(parmlist[2].getInputData().length));

        // Input: CCSID of user password. Int to byte[]. Unicode = 13488.
        parmlist[7] = new ProgramParameter(BinaryConverter.intToByteArray(13488));
        
        // If enhanced profile tokens supported then set parameters
        if (useEPT)
        {   
            // Input: Additional authentication factor (unicode)
            parmlist[8] = new ProgramParameter(BinaryConverter.charArrayToByteArray(additionalAuthenticationFactor));
            
            // Input: Length of additional authentication factor
            parmlist[9] = new ProgramParameter(BinaryConverter.intToByteArray((isAAFNull) ? 0 : parmlist[8].getInputData().length));
            
            // Input: CCSID of additional authentication factor
            parmlist[10] = new ProgramParameter(BinaryConverter.intToByteArray(13488));

            // Input: Authentication indicator (for passwords, it is ignored)
            parmlist[11] = new ProgramParameter(BinaryConverter.intToByteArray(0));

            // Input: Verification ID - must be 30 in length, blank padded
            parmlist[12] = new ProgramParameter(CharConverter.stringToByteArray(sys, (verificationId + "                              ").substring(0, 30)));

            // Input: Remote IP address
            parmlist[13] = new ProgramParameter(CharConverter.stringToByteArray(sys, remoteIpAddress));
            
            // Input: Length of remote IP address
            parmlist[14] = new ProgramParameter(BinaryConverter.intToByteArray((isRemoteIPNull) ? 0 : parmlist[13].getInputData().length));
            
            // Input: Remote port
            parmlist[15] = new ProgramParameter(BinaryConverter.intToByteArray(remotePort));

            // Input: Local IP address
            parmlist[16] = new ProgramParameter(CharConverter.stringToByteArray(sys, localIpAddress));

            // Input: Length of local IP address
            parmlist[17] = new ProgramParameter(BinaryConverter.intToByteArray((isLocalIPNull) ? 0 : parmlist[16].getInputData().length));

            // Input: Local port
            parmlist[18] = new ProgramParameter(BinaryConverter.intToByteArray(remotePort));
            
            isEnhancedToken[0] = true; 
        } else { 
            isEnhancedToken[0] = false; 
        }

        ProgramCall programCall = new ProgramCall(sys);

        try
        {
            programCall.setProgram(QSYSObjectPathName.toPath("QSYS", "QSYGENPT", "PGM"), parmlist);
            programCall.suggestThreadsafe(); // Run on-thread if possible; allows app to use disabled profile.
            if (!programCall.run())
            {
                Trace.log(Trace.ERROR, "Call to QSYGENPT failed.");
                throw new RetrieveFailedException(programCall.getMessageList());
            }
        }
        catch (RetrieveFailedException e) {
            throw e;
        }
        catch (java.io.IOException|java.beans.PropertyVetoException|InterruptedException e) {
            Trace.log(Trace.ERROR, "Unexpected Exception: ", e);
            throw new InternalErrorException(InternalErrorException.UNEXPECTED_EXCEPTION);
        }
        catch (Exception e) {
            Trace.log(Trace.ERROR, "Unexpected Exception: ", e);
            throw new RetrieveFailedException();
        }
        byte[] profileToken = parmlist[0].getOutputData();
        if (Trace.isTraceOn()) {
        	Trace.log(Trace.INFORMATION, this, "generateTokenExtended returned ",profileToken);
        }
        return profileToken; 
    }
    
    @Override
    public ProfileTokenCredential generateTokenExtended(String uid, char[] password,
            ProfileTokenCredential profileTokenCred) throws RetrieveFailedException, PropertyVetoException
    {
    	boolean[] enhancedProfileToken = new boolean[1]; 
    	enhancedProfileToken[0] = true; 
        byte[] token = generateTokenExtended(uid, password, 
                profileTokenCred.getAdditionalAuthenticationFactor(), 
                profileTokenCred.getVerificationID(),              
                profileTokenCred.getRemoteIPAddress(), 
                profileTokenCred.getRemotePort(),
                profileTokenCred.getLocalIPAddress(),  
                profileTokenCred.getLocalPort(),         
                profileTokenCred.getTokenType(), 
                profileTokenCred.getTimeoutInterval(),
                enhancedProfileToken);
        
        
        try {
            profileTokenCred.setToken(token,enhancedProfileToken[0]);
            profileTokenCred.setTokenCreator(ProfileTokenCredential.CREATOR_NATIVE_API);
        } 
        catch (PropertyVetoException e)
        {
            try {
                nativeRemoveFromSystem(token);
                credential_ = null;
            } catch (DestroyFailedException e1) {
                Trace.log(Trace.ERROR, "Unexpected Exception during profile token destroy: ", e);
            }
            
            throw e;
        }
        
        return profileTokenCred;
    }

    // Returns the credential delegating behavior to the implementation 
    // object.
    // @return  The associated credential.
    AS400Credential getCredential() {
        return credential_;
    }

    @Override
    public int getTimeToExpiration() throws RetrieveFailedException {
    	byte[] token = ((ProfileTokenCredential)getCredential()).getToken();
    	if (Trace.isTraceOn()) {
    		Trace.log(Trace.INFORMATION, this, "getTimeToExpiration token=", token);
    	}
        return nativeGetTimeToExpiration(token);
    }

    @Override
    public int getVersion() {
        return 1; // mod 3.
    }

    @Override
    public boolean isCurrent()
    {
        try {
            return (!getCredential().isTimed() || getTimeToExpiration()>0);
        }
        catch (RetrieveFailedException e)
        {
            Trace.log(Trace.ERROR, "Unable to retrieve credential time to expiration", e);
            return false;
        }
    }

    /**
     * Generates and returns a new profile token based on a user profile and
     * password special value.
     * 
     * @param name                 The name of the user profile for which the token
     *                             is to be generated.
     * @param passwordSpecialValue The special value for the user profile password.
     *                             Possible values are:
     *                             <ul>
     *                             <li>ProfileTokenCredential.PW_NOPWD
     *                             <li>ProfileTokenCredential.PW_NOPWDCHK
     *                             </ul>
     * @param type                 The type of token. Possible types are defined as
     *                             fields on the ProfileTokenCredential class:
     *                             <ul>
     *                             <li>ProfileTokenCredential.TYPE_SINGLE_USE
     *                             <li>ProfileTokenCredential.TYPE_MULTIPLE_USE_NON_RENEWABLE
     *                             <li>ProfileTokenCredential.TYPE_MULTIPLE_USE_RENEWABLE
     *                             </ul>
     * @param timeoutInterval      The number of seconds to expiration.
     * 
     * @return The token bytes.
     * @exception RetrieveFailedException If errors occur while generating the
     *                                    token.
     *                                    
     * @deprecated Use {@link #nativeCreateToken(String,char[],int,int)}
     */
    @Deprecated
    native byte[] nativeCreateToken(
            String user, 
            String password, 
            int type,
            int timeoutInterval) throws RetrieveFailedException;

    // Generates and returns a new profile token based on a user profile and 
    // password special value.
    // @param  name  The name of the user profile for which the token is to 
    // be generated.
    // @param  passwordSpecialValue  The special value for the user profile 
    // password. Possible values are:
    // <ul>
    // <li> ProfileTokenCredential.PW_NOPWD
    // <li> ProfileTokenCredential.PW_NOPWDCHK
    // </ul>
    // @param  type  The type of token.  Possible types are defined as fields 
    // on the ProfileTokenCredential class:
    // <ul>
    // <li>ProfileTokenCredential.TYPE_SINGLE_USE
    // <li>ProfileTokenCredential.TYPE_MULTIPLE_USE_NON_RENEWABLE
    // <li>ProfileTokenCredential.TYPE_MULTIPLE_USE_RENEWABLE
    // </ul>
    // @param  timeoutInterval  The number of seconds to expiration.
    // @return  The token bytes.
    // @exception  RetrieveFailedException  If errors occur while generating
    // the token.
    
    native byte[] nativeCreateTokenChar(
            String user, 
            char[] password, 
            int type,
            int timeoutInterval) throws RetrieveFailedException;

    // Returns the number of seconds before the credential is due to expire.
    // @param  token  The token bytes.
    // @return  The number of seconds before expiration.
    // @exception  RetrieveFailedException  If errors occur while retrieving 
    // timeout information.
    native int nativeGetTimeToExpiration(
            byte[] token) throws RetrieveFailedException;

    // Updates or extends the validity period for the credential.
    // Based on the given <i>token</i>, <i>type</i> and <i>timeoutInterval</i>.
    // <p>The updated token is stored back into the token parm.
    // @param  token  The token bytes.
    // @param  type  The type of token.  Possible types are defined as fields 
    // on the ProfileTokenCredential class:
    // <ul>
    // <li>TYPE_SINGLE_USE
    // <li>TYPE_MULTIPLE_USE_NON_RENEWABLE
    // <li>TYPE_MULTIPLE_USE_RENEWABLE
    // </ul>
    // @param  timeoutInterval  The number of seconds before expiration.
    // @exception  RefreshFailedException  If errors occur during refresh.
    native void nativeRefreshToken(
            byte[] token, 
            int type,
            int timeoutInterval) throws RefreshFailedException;

    // Removes the token from the system.
    // Note: The token is actually invalidated instead of being removed to
    // improve performance of the operation.
    // @param  token  The token bytes.
    // @exception  DestroyFailedException  If errors occur while removing 
    // the credential.
    native void nativeRemoveFromSystem(
            byte[] token) throws DestroyFailedException;

    // Attempt to swap the thread identity based on the given 
    // profile token.
    // @param  token  The token bytes.
    // @exception  SwapFailedException  If errors occur while swapping 
    // thread identity.
    native void nativeSwap(
            byte[] token) throws SwapFailedException;

    @Override
    public void refresh() throws RefreshFailedException {
       // Never called; ProfileTokenCredential relies exclusively on refresh(int, int).
    	throw new NullPointerException("INVALID CODEPATH");
    }

    @Override
    public byte[] refresh(int type, int timeoutInterval) throws RefreshFailedException
    {
    	if (Trace.isTraceOn()) { 
    		Trace.log(Trace.INFORMATION,"refresh() called"); 
    	}
        ProfileTokenCredential pt = ((ProfileTokenCredential)getCredential());
        
        byte[] token = pt.getToken();
        // native method will overwrite bytes passed in; create a copy 
        // to manipulate.
        byte[] bytes = new byte[ProfileTokenCredential.TOKEN_LENGTH];
        System.arraycopy(token, 0, bytes, 0, bytes.length);

    	
        if (!ProfileTokenCredential.useEnhancedProfileTokens() || AS400.nativeVRM.getVersionReleaseModification() <= 0x00070500) {
        	if (Trace.isTraceOn()) { 
        		Trace.log(Trace.INFORMATION,this,"refresh input",token); 
        	}

            nativeRefreshToken(bytes, type, timeoutInterval);
        }
        else
        {
            try {
            	if (Trace.isTraceOn()) { 
            		Trace.log(Trace.INFORMATION,this,"calling createTokenFromtoken(bytes,"+pt.getVerificationID()+","+
            				pt.getRemoteIPAddress()+","+type+","+timeoutInterval+")",token); 
            	}
                bytes = EnhancedProfileTokenImplNative.nativeCreateTokenFromToken(bytes, pt.getVerificationID(), pt.getRemoteIPAddress(), type, timeoutInterval);
            } catch (RetrieveFailedException e) {
            	RefreshFailedException refreshFailed = new RefreshFailedException(e.getAS400MessageList());
                throw refreshFailed;
            }
        }
        
    	if (Trace.isTraceOn()) { 
    		Trace.log(Trace.INFORMATION,this,"refresh output",token); 
    	}
        return bytes;
    }

    @Override
    public void setCredential(AS400Credential credential)
    {
        if (credential == null) {
            Trace.log(Trace.ERROR, "Parameter 'credential' is null.");
            throw new NullPointerException("credential");
        }
        credential_ = credential;
    }

    @Override
    public AS400Credential swap(boolean genRtnCr) throws SwapFailedException
    {
        ProfileHandleCredential ph = null;
        if (genRtnCr)
        {
            try {
                ph = new ProfileHandleCredential();
                ph.setSystem(((ProfileTokenCredential)getCredential()).getSystem());
                ph.setHandle();
            }
            catch (Exception e) {
                Trace.log(Trace.ERROR, "Unable to obtain current profile handle", e);
            }
        }
    	ProfileTokenCredential cred = (ProfileTokenCredential) getCredential();
        if (!cred.isEnhancedProfileToken()) { 
             nativeSwap(cred.getToken());
        } else {
        	EnhancedProfileTokenImplNative.nativeSwap(cred.getToken(),
        			cred.getVerificationID(),
        			cred.getRemoteIPAddress());
        }
        return ph;
    }
}
