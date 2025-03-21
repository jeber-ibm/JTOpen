///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                                 
//                                                                             
// Filename: SSLOptions.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2003 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.Serializable;

import javax.net.ssl.SSLSocketFactory;

// Class to move SSL configuration options from proxy client to proxy server.
class SSLOptions implements Serializable
{
    private static final String copyright = "Copyright (C) 1997-2003 International Business Machines Corporation and others.";
    static final long serialVersionUID = 4L;
    
    SSLOptions() {
        super();
    }
    
    SSLOptions(SSLOptions sslOptions) {
        super();
        
        keyRingName_ = sslOptions.keyRingName_;
        keyRingPassword_ = sslOptions.keyRingPassword_;
        keyRingData_ = sslOptions.keyRingData_;
        proxyEncryptionMode_ = sslOptions.proxyEncryptionMode_;
        useSslight_ = sslOptions.useSslight_;
        sslSocketFactory_ = sslOptions.sslSocketFactory_;
    }
    

    // Package and class name of key ring object, initialized to default.
    // Kept to prevent serializable errors (but not usable) 
    String keyRingName_ = null;
    // Password for keyring class, initialized to default.
    String keyRingPassword_ = null;
    // Data from keyring class.
    String keyRingData_ = null;
    // Legs of proxy server communications that should be encrypted.  Default is to encrypt all legs.
    int proxyEncryptionMode_ = SecureAS400.CLIENT_TO_SERVER;
    // Sslight removed 
    boolean useSslight_ = false;
    SSLSocketFactory sslSocketFactory_ = null;
}
