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
package org.apache.wiki.api.exceptions;



/**
 *  Marks an erroneus jspwiki.properties file.  Certain properties have been marked as "required", and if you 
 *  do not provide a good value for a property, you'll see this exception.
 *  <P>
 *  Check <TT>jspwiki.properties</TT> for the required properties.
 */
public class NoRequiredPropertyException extends WikiException {
	
    private static final long          serialVersionUID = 1L;

    /**
     *  Constructs an exception.
     *
     *  @param msg Message to show
     *  @param key The key of the property in question.
     */
    public NoRequiredPropertyException( String msg, String key ) {
        super( msg + ": key=" + key );
    }

}
