<?xml version="1.0" encoding="UTF-8"?>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="javax.servlet.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.providers.*" %>

<%!
    String message = null;
    String propertyResult = null;
    
    public static final String PROP_MASTERPWD = "jspwiki-s.auth.masterPassword";
    
    public String sanitizePath( String s )
    {
        s = TextUtil.replaceString(s, "\\", "\\\\" );
        return s.trim();
    }
    
    /**
     *  Simply sanitizes any URL which contains backslashes (sometimes Windows users may have them)
     */
    public String sanitizeURL( String s )
    {
        s = TextUtil.replaceString(s, "\\", "/" );
        return s.trim();
    }
    
    public String setProperty( String propertyfile, String key, String value )
    {
        int idx = 0;
        
        value = TextUtil.native2Ascii( value );
        
        while( (idx < propertyfile.length()) && ((idx = propertyfile.indexOf(key,idx)) != -1) )
        {
            int prevret = propertyfile.lastIndexOf("\n",idx);
            if( prevret != -1 )
            {
                // Commented lines are skipped
                if( propertyfile.charAt(prevret+1) == '#' ) 
                {
                    idx += key.length();
                    continue;
                }
            }
            int eqsign = propertyfile.indexOf("=",idx);
			
            if( eqsign != -1 )
            {
                int ret = propertyfile.indexOf("\n",eqsign);
				
                if( ret == -1 ) ret = propertyfile.length();
				
                propertyfile = TextUtil.replaceString( propertyfile, eqsign+1, ret, value );
                
                return propertyfile;
            }
            // idx += key.length();
        }

        //
        //  If it was not found, we'll add it here.
        //
        
        propertyfile += "\n"+key+" = "+value+"\n";
		
        return propertyfile;
    }
    
    public static String safeGetParameter( ServletRequest request, String name )
    {
        try
        {
            String res = request.getParameter( name );
            if( res != null ) 
            {
                res = new String(res.getBytes("ISO-8859-1"),
                                 "UTF-8" );
            }

            return res;
        }
        catch( UnsupportedEncodingException e )
        {
            // Should never happen
            return "";
        }

    }
    public void writeProperties( File propertyFile, String contents )
        throws IOException
    {
        byte[] bytes = contents.getBytes();
        OutputStream out = null;
        
        try
        {
            out = new FileOutputStream( propertyFile );
            
            FileUtil.copyContents( new ByteArrayInputStream( bytes ),
                                   out );
        }
        finally
        {
            if( out != null ) out.close();
        }
    }
    
    public File findPropertyFile( ServletContext context )
    {
        String path = context.getRealPath("/");
        
        File f = new File( path, WikiEngine.DEFAULT_PROPERTYFILE );
        
        return f;
    }
    
    public String readProperties( ServletContext context )
        throws IOException
    {
        File f = findPropertyFile( context );
        FileReader in = null;
        String contents = null;
        try
        {
            in = new FileReader( f );
            contents = FileUtil.readContents( in );
        }
        finally
        {
            if( in != null ) in.close();
        }
		
        return contents;
    }
%>

<%
    String propertyString = readProperties( application );
    
    Properties props = new Properties();
    props.load( new ByteArrayInputStream(propertyString.getBytes()) );
    

    String appname   = safeGetParameter( request, "appname" );
    String baseurl   = safeGetParameter( request, "baseurl" );
    String dir       = safeGetParameter( request, "dir" );
    String logdir    = safeGetParameter( request, "logdir" );
    String workdir   = safeGetParameter( request, "workdir" );
    String password1 = safeGetParameter( request, "password1" );
    String password2 = safeGetParameter( request, "password2" );
    String password  = safeGetParameter( request, "password" );

    String oldpassword = props.getProperty( PROP_MASTERPWD, null );
    
    if( request.getParameter("submit") != null )
    {
        if( dir.length() == 0 )
        {
            message = "You must define the location where the files are stored!";
        }
        else if( appname.length() == 0 )
        {
            message = "You must define application name";
        }
        else if( workdir.length() == 0 )
        {
            message = "You must define a work directory";
        }
        else if( logdir.length() == 0 )
        {
            message = "You must define a log directory";
        }
        else if( password1 != null && !password1.equals(password2) )
        {
            message = "Password missing or password mismatch";
        }
        else if( oldpassword != null && !oldpassword.equals(password) )
        {
            message = "The password you gave does not match with the master password";
        }
        else
        {
            propertyString = setProperty( propertyString, WikiEngine.PROP_APPNAME, appname );
            propertyString = setProperty( propertyString, WikiEngine.PROP_BASEURL, sanitizeURL(baseurl) );
            propertyString = setProperty( propertyString, FileSystemProvider.PROP_PAGEDIR, sanitizePath(dir) );
            propertyString = setProperty( propertyString, BasicAttachmentProvider.PROP_STORAGEDIR, sanitizePath(dir) );
            propertyString = setProperty( propertyString, WikiEngine.PROP_WORKDIR, sanitizePath(workdir) );
            propertyString = setProperty( propertyString, "log4j.appender.FileLog.File", sanitizePath( logdir ) );
            
            if( password1 != null )
            {
                propertyString = setProperty( propertyString, PROP_MASTERPWD, password1 );
                oldpassword = password1;
            }

            //
            //  Some default settings for the easy setup
            //
            propertyString = setProperty( propertyString, PageManager.PROP_PAGEPROVIDER, "VersioningFileProvider" );
            propertyString = setProperty( propertyString, WikiEngine.PROP_ENCODING, "UTF-8" );
            
            try
            {
                writeProperties( findPropertyFile(application), propertyString );
                message = "Your new properties have been saved.  Please restart your container (unless this was your first install).  Scroll down a bit to see your new jspwiki.properties.";
            }
            catch( IOException e )
            {
                message = "Unable to write properties: "+e.getMessage()+
                          ".  Please copy the file below as your jspwiki.properties";
            }
            propertyResult = propertyString;
        }
    }
%>

<%    
    File propertyFile = findPropertyFile( application );

	if( appname == null ) appname = props.getProperty( WikiEngine.PROP_APPNAME, "MyWiki" );
    
	if( baseurl == null ) 
	{
		baseurl = HttpUtils.getRequestURL(request).toString();
		baseurl = baseurl.substring( 0, baseurl.lastIndexOf('/') )+"/";
        
        baseurl = props.getProperty( WikiEngine.PROP_BASEURL, baseurl );
	}
	
	if( dir == null ) dir = props.getProperty( FileSystemProvider.PROP_PAGEDIR, "Please configure me!" );
	
	if( logdir == null ) logdir = props.getProperty( "log4j.appender.FileLog.File", "/tmp/" );

	if( workdir == null ) workdir = props.getProperty( WikiEngine.PROP_WORKDIR, "/tmp/" );
    
    if( password1 == null ) password1 = "";
    if( password2 == null ) password2 = "";
    
    
	// FIXME: encoding as well.
    
    response.addHeader("Pragma", "no-cache");
    response.setHeader( "Expires", "-1" );
    response.setHeader("Cache-Control", "no-cache" );
    response.setContentType("text/html; charset=UTF-8");
%>

<!DOCTYPE html 
     PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>

<head>
   <title>JSPWiki configuration helper</title>
   <style>
	   .configopt { margin-top: 1ex;
	   				margin-bottom: 1ex; }
	   #content { width: 60%;
	   		  	padding: 12px;
	   	      	margin-left: auto;
	   	      	margin-right: auto; 
	   	      	background: #eeeeee; }
   </style>
</head>

<body bgcolor="#FFFFFF">

	<div id="content">
	<h1>JSPWiki configuration</h1>
	
	<p>Welcome!  This little JSP page is here to help you do the first difficult stage of JSPWiki
	installation.  If you're seeing this page, you have already installed JSPWiki correctly
	inside your container.</p>
	
	<p>There are now some things that you should configure.  When you press submit, the "jspwiki.properties"
	-file from the distribution will be modified, or if it can't be found, a new one will be created.</p>

	<p>This setup system is really meant for people who just want to be up and running really quickly.
	If you want to integrate JSPWiki with an existing system, I would recommend that you go and edit
	the jspwiki.properties file directly.  You can find a sample config file from 
	<tt>yourwiki/WEB-INF/</tt>.</p>

    <%if(propertyFile!=null) {%>
        <p>(Read these properties from '<%=propertyFile.getAbsolutePath()%>')</p>
    <%}%>

    <%if( message != null ) { %>
    <div style="border:1px inset solid; color: red; padding:4px;">
       <%=message%>
    </div>
    <%}%>

	<form action="Install.jsp" method="post">

	<h3>Mandatory options</h3>
	
	<div class="configopt">
		<b>Application Name</b>: <input type="text" name="appname" size="20" value="<%=appname%>"/><br />
		<i>What should your wiki be called?  Try and make this a relatively short one.</i>
	</div>

	<div class="configopt">
		<b>Base URL</b>: <input type="text" name="baseurl" size="80" value="<%=baseurl%>"/><br />
		<i>Please tell me where your wiki is located.</i>
	</div>

	<div class="configopt">
		<b>File storage</b>: <input type="text" name="dir" size="80" value="<%=dir%>"/><br />
		<i>By default, I will use the VersioningFileProvider that will store files in a particular
		directory on your hard drive.  If you specify a directory that does not exist, I'll
		create one for you.  All attachments will also be put in the same directory.</i>
	</div>

	<div class="configopt">
		<b>Work directory</b>: <input type="text" name="workdir" size="80" value="<%=workdir%>"/><br />
		<i>This is the place where all caches and other runtime stuff is stored.</i>
	</div>


	<h3>Other useful settings</h3>

    <div class="configopt">
        <table border="0">
        <tr><td><b>Administrator password</b>:</td><td><input type="password" name="password1" size="30" value="<%=password1%>"/></td></tr>
        <tr><td><b>Repeat password</b>:</td><td><input type="password" name="password2" size="30" value="<%=password2%>"/></td></tr>
        </table>
        <% if( oldpassword != null ) { %>
        <i>If you want to change your current password, type it here.</i>
        <% } else { %>
        <i>Enter your new password here.  It's not mandatory, but anyone can access this setup page,
           unless you set a password.  <b>HIGHLY RECOMMENDED</b></i>
        <% } %>
    </div>
	
<%--	
	<div class="configopt">
		<b>URL format</b>: <input type="radio" name="urlfmt" value="relative" checked="checked"/>Relative
		<input type="radio" name="urlfmt" value="absolute"/>Absolute<br />
		<i>Would you like to use relative URLs whenever possible (they are shorter)?</i>
	</div>
	
	<div class="configopt">
		<b>Encoding</b>: <input type="radio" name="encoding" value="UTF-8"  checked="checked"/>UTF-8
		<input type="radio" name="encoding" value="ISO-8859-1"/>ISO-8859-1<br />
		<i>UTF-8 is the standard encoding in the internet these days, but ISO-8859-1 (aka Latin1) might
		   be useful for legacy applications.</i>
	</div>
--%>

	<div class="configopt">
		<b>Log files</b>: <input type="text" name="logdir" value="<%=logdir%>" size="80"/><br />
		<i>JSPWiki uses Jakarta Log4j for logging.  Please tell me where the log files should go to?</i>
	</div>

    <% if( oldpassword != null ) { %>
        <hr />
        <div class="configopt">
        <b>Current administrator password</b>: <input type="password" name="password" size="30" value=""/><br />
        <i>You must give the current administrator password for the change to take place.</i>
        </div>
    <% } %>
    
	<div style="width:100px; margin-left:auto;margin-right:auto;margin-top:2ex;">
		<input type="submit" name="submit" value="Configure!" />
	</div>
	
	</form>
    
    <%if(propertyResult != null) {%>
    <hr />
    <h3>Here is your new jspwiki.properties</h3>
    <pre>
<%=propertyResult%>
    </pre>
    <%}%>
	</div>
</body>
</html>