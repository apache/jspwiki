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

package org.apache.wiki.api.engine;

import java.util.List;

import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.ui.admin.AdminBean;

public interface AdminBeanManager {

	void initialize();

	/**
	 *  Lists all administration beans which are currently known
	 *  and instantiated.
	 *
	 *  @return all AdminBeans known to the manager
	 */
	List<AdminBean> getAllBeans();

	/**
	 *  Locates a bean based on the AdminBean.getId() out of all
	 *  the registered beans.
	 *
	 *  @param id ID
	 *  @return An AdminBean, or null, if no such bean is found.
	 */
	AdminBean findBean(String id);

	/**
	 *  Unregisters AdminBeans upon SHUTDOWN event.
	 *
	 *  @param event the WikiEvent
	 */
	void actionPerformed(WikiEvent event);
	
	/**
     *  Returns the type identifier for a string type.
     *
     *  @param type A type string.
     *  @return A type value.
     */
    int getTypeFromString( String type );

}