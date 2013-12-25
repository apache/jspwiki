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