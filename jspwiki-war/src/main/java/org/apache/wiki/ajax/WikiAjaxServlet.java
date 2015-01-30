package org.apache.wiki.ajax;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface WikiAjaxServlet {

	public String getServletMapping();
	
	public void service(HttpServletRequest request, HttpServletResponse response, String actionName, List<String> params) throws ServletException, IOException;

}
