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
package org.apache.wiki.parser;

/**
 *  This class is used to store the headings in a manner which
 *  allow the building of a Table Of Contents.
 *
 *  @since 2.4
 */
public class Heading
{
    /**
     *  Defines a small heading.
     */
    public static final int HEADING_SMALL  = 1;
    
    /**
     *  Defines a medium-size heading.
     */
    public static final int HEADING_MEDIUM = 2;
    
    /**
     *  Defines a large heading.
     */
    public static final int HEADING_LARGE  = 3;

    /**
     *  Denotes the level of the heading. Either HEADING_SMALL, HEADING_MEDIUM, or HEADING_LARGE.
     */
    public int    m_level;
    
    /**
     *  Contains the text of the heading.
     */
    public String m_titleText;
    
    /**
     *  Contains the anchor to the heading
     */
    public String m_titleAnchor;
    
    /**
     *  Contains a section link.
     */
    public String m_titleSection;
}
