<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

      <P>
      This is a page which allows you to set up all sorts of interesting things.
      You need to have cookies enabled for this to work, though.
      </P>

      <FORM action="<wiki:Variable var="jspwiki.baseURL"/>UserPreferences.jsp" 
            method="POST"
            ACCEPT-CHARSET="UTF-8">

         <B>User name:</B> <INPUT type="text" name="username" size="30" value="<wiki:UserName/>">
         <I>This must be a proper WikiName, no punctuation.</I>
         <BR><BR>
         <INPUT type="submit" name="ok" value="Set my preferences!">
         <INPUT type="hidden" name="action" value="save">
      </FORM>

      <HR/>

      <H3>Removing your preferences</h3>

      <P>In some cases, you may need to remove the above preferences from the computer.
      Click the button below to do that.  Note that it will remove all preferences
      you've set up, permanently.  You will need to enter them again.</P>

      <DIV align="center">
      <FORM action="<wiki:Variable var="jspwiki.baseURL"/>UserPreferences.jsp"
            method="POST"
            ACCEPT-CHARSET="UTF-8">
      <INPUT type="submit" name="clear" value="Remove preferences from this computer" />
      </FORM>
      </DIV>
