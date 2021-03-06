/**
 * Copyright (C) 2014 OpenTravel Alliance (info@opentravel.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opentravel.schemacompiler.diff;

import org.opentravel.schemacompiler.model.TLLibrary;

/**
 * Container for all change items identified during the comparison of two libraries, as well as the entity change sets
 * for the library members that existed in both versions of the library.
 */
public class LibraryChangeSet extends ChangeSet<TLLibrary,LibraryChangeItem> {

    /**
     * Constructor that assigns the old and new version of a library that was modified.
     * 
     * @param oldLibrary the old version of the library
     * @param newLibrary the new version of the library
     */
    public LibraryChangeSet(TLLibrary oldLibrary, TLLibrary newLibrary) {
        super( oldLibrary, newLibrary );
    }

    /**
     * @see org.opentravel.schemacompiler.diff.ChangeSet#getBookmarkId()
     */
    public String getBookmarkId() {
        return getBookmarkId( (getNewVersion() != null) ? getNewVersion() : getOldVersion() );
    }

}
