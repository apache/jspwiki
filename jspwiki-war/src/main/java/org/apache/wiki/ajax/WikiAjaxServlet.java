package org.apache.wiki.ajax;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wiki.plugin.SampleAjaxPlugin;

/**
 * An interface for a servlet that wants to use Ajax functionality.
 * See {@link SampleAjaxPlugin}
 * 
 * @since 2.10.2-svn12
 */
public interface WikiAjaxServlet {

	public String getServletMapping();
	
	public void service(HttpServletRequest request, HttpServletResponse response, String actionName, List<String> params) throws ServletException, IOException;

}
