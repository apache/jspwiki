<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<wiki:TabbedSection defaultTab="addcomment">
  <wiki:Tab id="pagecontent" title="Discussion page">
    <wiki:InsertPage/>
  </wiki:Tab>

  <wiki:Tab id="addcomment" title="Add comment">
    <wiki:Include page="editors/plain.jsp"/>
  </wiki:Tab>

  <wiki:Tab id="edithelp" title="Help">
    <wiki:NoSuchPage page="EditPageHelp">
      <div class="error">
         Ho hum, it seems that the EditPageHelp<wiki:EditLink page="EditPageHelp">?</wiki:EditLink>
         page is missing.  Someone must've done something to the installation...
      </div>
    </wiki:NoSuchPage>

    <wiki:InsertPage page="EditPageHelp" />
  </wiki:Tab>
</wiki:TabbedSection>
