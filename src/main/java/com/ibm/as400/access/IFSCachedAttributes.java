///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                              
//                                                                             
// Filename: IFSCachedAttributes.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2002 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;
import java.io.Serializable;


/**
 * Store cached attributes.
 **/
class IFSCachedAttributes implements Serializable
{
    static final long serialVersionUID = 4L;

    static final int FA_READONLY = 0x01;
    static final int FA_HIDDEN = 0x02;

    long accessDate_;
    long creationDate_;
    int fixedAttributes_;
    boolean isDirectory_;
    boolean isFile_;
    boolean isSymbolicLink_;
    long modificationDate_;
    String name_;
    int objectType_;
    String parent_; // path of directory
    long size_;
    byte[] restartID_;

    int fileDataCCSID_;
    String ownerName_;
    String description_;
    int fileAsp_;
    int fileSystemType_;
    
    static final int UNINITIALIZED = -1;

    /**
     * Construct listCachedAttributes object from a list of attributes.
     **/
    IFSCachedAttributes(long accessDate, long creationDate, int fixedAttributes, long modificationDate, int objectType,
            long size, String name, String parent, boolean isDirectory, boolean isFile, byte[] restartID,
            boolean isSymbolicLink, int fileSystemType)
    {
        accessDate_ = accessDate;
        creationDate_ = creationDate;
        fixedAttributes_ = fixedAttributes;
        isDirectory_ = isDirectory;
        isFile_ = isFile;
        modificationDate_ = modificationDate;
        name_ = name;
        objectType_ = objectType;
        parent_ = parent;
        size_ = size;
        restartID_ = restartID;
        isSymbolicLink_ = isSymbolicLink;

        fileSystemType_ = fileSystemType;
        
        // These fields are special and we do not get from cache
        description_ = null;
        ownerName_ = null;
        fileAsp_ = UNINITIALIZED;
        fileDataCCSID_ = UNINITIALIZED;
    }

    /**
     * Return access date.
     **/
    long getAccessDate() {
        return accessDate_;
    }

    /**
     * Return creation date.
     **/
    long getCreationDate() {
        return creationDate_;
    }

    /**
     * Return fixed attributes.
     **/
    int getFixedAttributes() {
        return fixedAttributes_;
    }

    /**
     * Return isDir_
     **/
    boolean getIsDirectory() {
        return isDirectory_;
    }

    /**
     * Return isFile_
     **/
    boolean getIsFile() {
        return isFile_;
    }

    /**
     * Return modification date.
     **/
    long getModificationDate() {
        return modificationDate_;
    }

    /**
     * Return name.
     **/
    String getName() {
        return name_;
    }

    /**
     * Return object type.
     **/
    int getObjectType() {
        return objectType_;
    }

    /**
     * Return path of parent directory.
     **/
    String getParent() {
        return parent_;
    }

    /**
     * Return restart ID.
     **/
    byte[] getRestartID() {
        return restartID_;
    }

    /**
     * Return size.
     **/
    long getSize()
    {
        return size_;
    }

    /**
     * Return isSymbolicLink_
     **/
    boolean isSymbolicLink() {
        return isSymbolicLink_;
    }

    /**
     * Return File system type
     */
    int getFileSystemType() {
        return fileSystemType_;
    }

    /**
     * @return Owner name
     */
    String getOwnerName() {
        return ownerName_;
    }

    /**
     * @return File ASP
     */
    int getFileAsp() {
        return fileAsp_;
    }

    /**
     * @return File data CCSID
     */
    int getFileDataCcsid() {
        return fileDataCCSID_;
    }

    public String getDescription() {
        return description_;
    }
}
