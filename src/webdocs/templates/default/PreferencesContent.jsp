<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<wiki:TabbedSection defaultTab="${param.tab}">

  <wiki:Tab id="prefs" titleKey="prefs.tab.prefs" accesskey="p">
     <wiki:Include page="PreferencesTab.jsp" />
  </wiki:Tab>

  <wiki:UserCheck status="authenticated">
  <wiki:Permission permission="editProfile">
  <wiki:Tab id="profile" titleKey="prefs.tab.profile" accesskey="o">
     <wiki:Include page="ProfileTab.jsp" />
  </wiki:Tab>
  </wiki:Permission>
  </wiki:UserCheck>
  
  <wiki:Permission permission="createGroups"> <!-- FIXME check right permissions -->
  <wiki:Tab id="group" titleKey="group.tab" accesskey="g">
    <wiki:Include page="GroupTab.jsp" />
  </wiki:Tab>
  </wiki:Permission>

</wiki:TabbedSection>