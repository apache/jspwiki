/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.content;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.WikiException;

/**
 * Provides page renaming functionality. Note that there used to be a similarly named class in 2.6, but due to unclear copyright, the
 * class was completely rewritten from scratch for 2.8.
 *
 * @since 2.8
 */
public interface PageRenamer {

    /**
     *  Renames a page.
     *  
     *  @param context The current context.
     *  @param renameFrom The name from which to rename.
     *  @param renameTo The new name.
     *  @param changeReferrers If true, also changes all the referrers.
     *  @return The final new name (in case it had to be modified)
     *  @throws WikiException If the page cannot be renamed.
     */
    String renamePage( Context context, String renameFrom, String renameTo, boolean changeReferrers ) throws WikiException;

    /**
     * Fires a WikiPageRenameEvent to all registered listeners. Currently not used internally by JSPWiki itself, but you can use it for
     * something else.
     *
     * @param oldName the former page name
     * @param newName the new page name
     */
    void firePageRenameEvent( String oldName, String newName );

}
