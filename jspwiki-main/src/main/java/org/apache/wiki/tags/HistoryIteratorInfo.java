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
package org.apache.wiki.tags;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

/**
 *  Just provides the TEI data for HistoryIteratorTag.
 *
 *  @since 2.0
 */
public class HistoryIteratorInfo extends TagExtraInfo
{
    @Override
    public VariableInfo[] getVariableInfo(final TagData data)
    {

        return new VariableInfo[]{ new VariableInfo( data.getAttributeString("id"),
                                                 "org.apache.wiki.api.core.Page",
                                                 true,
                                                 VariableInfo.NESTED )
        };

    }
}
