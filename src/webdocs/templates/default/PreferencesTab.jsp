<%@ page errorPage="/Error.jsp" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>
<%
  /* see commonheader.jsp */
  String prefDateFormat     = (String) session.getAttribute("prefDateFormat");
  String prefTimeZone       = (String) session.getAttribute("prefTimeZone");
  String prefEditorType     = (String) session.getAttribute("prefEditorType"); //TODO

  WikiContext c = WikiContext.findContext( pageContext );
  pageContext.setAttribute( "skins", c.getEngine().getTemplateManager().listSkins(pageContext, c.getTemplate() ) );

%>

<h3><fmt:message key="prefs.heading"><fmt:param><wiki:Variable var="applicationname"/></fmt:param></fmt:message></h3>

<c:if test="${param.tab eq 'prefs'}" >
  <div class="formhelp">
    <wiki:Messages div="error" topic="prefs" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix.prefs")%>'/>
  </div>
</c:if>

<form action="<wiki:Link jsp='UserPreferences.jsp' format='url'><wiki:Param name='tab' value='prefs'/></wiki:Link>" 
       class="wikiform" 
        name="setCookie" id="setCookie"
      method="post" accept-charset="<wiki:ContentEncoding />" 
    onsubmit="var s=[];
             ['prefSkin','prefTimeFormat','prefTimeZone'].each(function(el){
               if($(el)) s.push($(el).getValue());
             });
             if(Wiki.PrefFontSize) s.push(Wiki.PrefFontSize);
             Cookie.set( 'JSPWikiUserPrefs', s.join(Wiki.DELIM), Wiki.BasePath); 
             return Wiki.submitOnce( this ); " >
<table>

  <tr>
  <td><label for="username"><fmt:message key="prefs.assertedname"/></label></td>
  <td> 
  <input type="text" id="assertedName" name="assertedName" size="20" value="<wiki:UserProfile property='wikiname' />" />
  <%-- CHECK THIS
  <input type="text" id="assertedName" name="assertedName" size="20" value="<wiki:UserProfile property='loginname'/>" />
  --%>
  </td>
  </tr>
  <wiki:UserCheck status="anonymous">
  <tr>
  <td />
  <td>
  <div class="formhelp">
    <fmt:message key="prefs.assertedname.description">
      <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
      <fmt:param>
        <a href="<wiki:Link jsp='Login.jsp' format='url'><wiki:Param name='tab' value='register'/></wiki:Link>">
          <fmt:message key="prefs.assertedname.create"/>
        </a>
      </fmt:param>
    </fmt:message>
  </td>
  </tr>
  </wiki:UserCheck>

  <tr>
  <td><label for="prefSkin"><fmt:message key="prefs.user.skin"/></label></td>
  <td>
  <select id="prefSkin" name="prefSkin">
    <c:forEach items="${skins}" var="i">
      <option value='<c:out value='${i}'/>' <c:if test='${i == prefSkinName}'>selected="selected"</c:if> ><c:out value="${i}"/></option>
    </c:forEach>
  </select>
  </td>
  </tr>

  <tr>
  <td><label for="prefFontSize" ><fmt:message key="prefs.user.fontsize"/></label></td>
  <td>
	<input type="button" value="-" onclick="Wiki.changeFontSize(-1);"
		  title="<fmt:message key='pref.fontsize.title.down' />"></input>
	<input type="button" value="<fmt:message key='pref.fontsize.reset' />"
	    onclick="Wiki.resetFontSize();" name="prefFontSize"
		  title="<fmt:message key='pref.fontsize.title.reset' />"></input>
	<input type="button" value="+" onclick="Wiki.changeFontSize(1);"
		  title="<fmt:message key='pref.fontsize.title.up' />"></input>
  </td>
  </tr>
  
  <%-- FIXME: temporary removed; "editor" session parameter to be set - see EditContent.jsp, editorManager.java 
  <tr>
  <td><label for="prefEditorType"><u>E</u>ditor Type</label></td>
  <td>
  <select id="prefEditorType" name="prefEditorType" >
    <option <%= ("plain".equals(prefEditorType)) ? "selected=\'selected\'" : "" %> value="plain">Standard wiki-markup editor</option>
    <option <%= ("WikiWizard".equals(prefEditorType)) ? "selected=\'selected\'" : "" %> value="WikiWizard">WikiWizard</option>
    <option <%= ("FCK".equals(prefEditorType)) ? "selected=\'selected\'" : "" %> value="FCK">FCK Editor</option>
  </select>
  </td>
  </tr>
  --%>
  
  <tr>
  <td><label for="prefTimeFormat"><fmt:message key="prefs.user.timeformat"/></label></td>
  <td>
  <select id="prefTimeFormat" name="prefTimeFormat" >
    <%
      String[] arrTimeFormat = 
      {"d/MM"
      ,"d/MM/yy"
      ,"d/MM/yyyy"
      ,"dd/MM/yy"
      ,"dd/MM/yyyy"
      ,"EEE, dd/MM/yyyy"
      ,"EEE, dd/MM/yyyy, Z"
      ,"EEE, dd/MM/yyyy, zzzz"
      ,"d/MM/yy hh:mm"
      ,"d/MM/yy HH:mm a"
      ,"d/MM/yy HH:mm a, Z"
      ,"dd-MMM"
      ,"dd-MMM-yy"
      ,"dd-MMM-yyyy"
      ,"EEE, dd-MMM-yyyy"
      ,"EEE, dd-MMM-yyyy, Z"
      ,"EEE, dd-MMM-yyyy, zzzz"
      ,"dd-MMM-yyyy hh:mm"
      ,"dd-MMM-yyyy HH:mm a"
      ,"dd-MMM-yyyy HH:mm a, Z"
      ,"MMMM dd, yyyy"
      ,"MMMM dd, yyyy hh:mm"
      ,"MMMM dd, yyyy HH:mm a"
      ,"MMMM, EEE dd,yyyy HH:mm a"
      ,"MMMM, EEEE dd,yyyy HH:mm a"
      } ;
      java.util.Date d = new java.util.Date() ;  // Now.
      for( int i=0; i < arrTimeFormat.length; i++ )
      {
        String f = arrTimeFormat[i];
        String selected = ( prefDateFormat.equals( f ) ? " selected='selected'" : "" ) ;
        try
        {
          java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat( f );
          java.util.TimeZone tz = java.util.TimeZone.getDefault();
          try 
          {
            tz.setRawOffset( Integer.parseInt( prefTimeZone ) );
          }
          catch( Exception e) { /* dont care */ } ;
          fmt.setTimeZone( tz );
    %>
          <option value="<%= f %>" <%= selected%> ><%= fmt.format(d) %></option>
   <%
        }
        catch( IllegalArgumentException e ) { } // skip parameter
      }
    %>
  </select>
  </td>
  </tr>

  <tr>
  <td><label for="prefTimeZone"><fmt:message key="prefs.user.timezone"/></label></td>
  <td>
  <select id='prefTimeZone' name='prefTimeZone' class='select'>
    <% 
       String[][] tzs = 
       { { "-43200000" , "(UTC-12) Enitwetok, Kwajalien" }
       , { "-39600000" , "(UTC-11) Nome, Midway Island, Samoa" }
       , { "-36000000" , "(UTC-10) Hawaii" }
       , { "-32400000" , "(UTC-9) Alaska" }
       , { "-28800000" , "(UTC-8) Pacific Time" }
       , { "-25200000" , "(UTC-7) Mountain Time" }
       , { "-21600000" , "(UTC-6) Central Time, Mexico City" }
       , { "-18000000" , "(UTC-5) Eastern Time, Bogota, Lima, Quito" }
       , { "-14400000" , "(UTC-4) Atlantic Time, Caracas, La Paz" }
       , { "-12600000" , "(UTC-3:30) Newfoundland" }
       , { "-10800000" , "(UTC-3) Brazil, Buenos Aires, Georgetown, Falkland Is." }
       , {  "-7200000" , "(UTC-2) Mid-Atlantic, Ascention Is., St Helena" }
       , {  "-3600000" , "(UTC-1) Azores, Cape Verde Islands" }
       , {         "0" , "(UTC) Casablanca, Dublin, Edinburgh, London, Lisbon, Monrovia" }
       , {   "3600000" , "(UTC+1) Berlin, Brussels, Copenhagen, Madrid, Paris, Rome" }
       , {   "7200000" , "(UTC+2) Kaliningrad, South Africa, Warsaw" }
       , {  "10800000" , "(UTC+3) Baghdad, Riyadh, Moscow, Nairobi" }
       , {  "12600000" , "(UTC+3.30) Tehran" }
       , {  "14400000" , "(UTC+4) Adu Dhabi, Baku, Muscat, Tbilisi" }
       , {  "16200000" , "(UTC+4:30) Kabul" }
       , {  "18000000" , "(UTC+5) Islamabad, Karachi, Tashkent" }
       , {  "19800000" , "(UTC+5:30) Bombay, Calcutta, Madras, New Delhi" }
       , {  "21600000" , "(UTC+6) Almaty, Colomba, Dhakra" }
       , {  "25200000" , "(UTC+7) Bangkok, Hanoi, Jakarta" }
       , {  "28800000" , "(UTC+8) Beijing, Hong Kong, Perth, Singapore, Taipei" }
       , {  "32400000" , "(UTC+9) Osaka, Sapporo, Seoul, Tokyo, Yakutsk" }
       , {  "34200000" , "(UTC+9:30) Adelaide, Darwin" }
       , {  "36000000" , "(UTC+10) Melbourne, Papua New Guinea, Sydney, Vladivostok" }
       , {  "39600000" , "(UTC+11) Magadan, New Caledonia, Solomon Islands" }
       , {  "43200000" , "(UTC+12) Auckland, Wellington, Fiji, Marshall Island" }
       };
       String servertz = Integer.toString( java.util.TimeZone.getDefault().getRawOffset() ) ;
       String selectedtz = servertz;
       for( int i=0; i < tzs.length; i++ )
       {
         if( prefTimeZone.equals( tzs[i][0] ) ) selectedtz = prefTimeZone;
       }
       for( int i=0; i < tzs.length; i++ )
       {
         String selected = ( selectedtz.equals( tzs[i][0] ) ? " selected='selected'" : "" ) ;
         String server = ( servertz.equals( tzs[i][0] ) ? " [SERVER]" : "" ) ;
    %>
        <option value="<%= tzs[i][0] %>" <%= selected%> ><%= tzs[i][1]+server %></option>
   <%
       }
    %>    
  </select>
  </td>
  </tr>

  <%-- user browser language only ;  why not allow to choose from all installed server languages on jspwiki ??   
  <tr>
  <td><label for="prefLanguage">Select Language</label></td>
  <td>
  <select id="prefLanguage" name="prefLanguage" >
    <option value="">English</option>
  </select>
  </td>
  </tr>
  
  <tr>
  <td><label for="prefShowQuickLinks">Show Quick Links</label></td>
  <td>
  <input class='checkbox' type='checkbox' id='prefShowQuickLinks' name='prefShowQuickLinks' 
         <%= (prefShowQuickLinks.equals("yes") ? "checked='checked'" : "") %> />
         <span class="quicklinks"><span 
               class='quick2Top'><a href='#wikibody' title='Go to Top' >&laquo;</a></span><span 
               class='quick2Prev'><a href='#' title='Go to Previous Section'>&lsaquo;</a></span><span 
               class='quick2Edit'><a href='#' title='Edit this section'>&bull;</a></span><span 
               class='quick2Next'><a href='#' title='Go to Next Section'>&rsaquo;</a></span><span 
               class='quick2Bottom'><a href='#footer' title='Go to Bottom' >&raquo;</a></span></span>
  </td>
  </tr>

  <tr>
  <td><label for="prefShowCalendar">Show Calendar</label></td>
  <td>
    <input class='checkbox' type='checkbox' id='prefShowCalendar' name='prefShowCalendar' 
            <%= (prefShowCalendar.equals("yes") ? "checked='checked'": "") %> >
  </td>
  </tr>
  --%>
 <tr>
  <td>
  </td>
  <td>
    <input type="submit" name="ok" value="Save User Preferences" style="display:none;"/>
    <input type="button" name="ox" value="Save User Preferences" onclick="this.form.ok.click();" />

    <input type="hidden" name="action" value="setAssertedName" />
    <wiki:UserCheck status="anonymous">
    </wiki:UserCheck>
    <div class="formhelp">Your choices will be saved in your browser as cookies.</div>
  </td>
  </tr>

</table>
</form>
  
<!-- Clearing the 'asserted name' and other perfs in the cookie -->
<%--wiki:UserCheck status="asserted"--%>

<h3>Removing User Preferences</h3>

<form action="<wiki:Link format='url' jsp='UserPreferences.jsp'><wiki:Param name='tab' value='prefs'/></wiki:Link>"
        name="clearCookie" id="clearCookie"
    onsubmit="Cookie.remove( 'JSPWikiUserPrefs' ); 
              return Wiki.submitOnce( this );" 
      method="POST" accept-charset="<wiki:ContentEncoding />" >

  <input type="submit" name="ok" value="Remove user preferences" style="display:none;" />
  <input type="button" name="ox" value="<fmt:message key='prefs.clear.submit'/>" onclick="this.form.ok.click();" />
  <input type="hidden" name="action" value="clearAssertedName" />

  <div class="formhelp"><fmt:message key="prefs.clear.description" /></div>

</form>
<%--/wiki:UserCheck--%>
