<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

   <DIV class="conflictnote">
      <P><B>Oops!  Someone modified the page while you were editing it!</B></P>

      <P>Since I am stupid and can't figure out what the difference
      between those pages is, you will need to do that for me.  I've
      printed here the text (in Wiki) of the new page, and the
      modifications you made.  You'll now need to copy the text onto a
      scratch pad (Notepad or emacs will do just fine), and then edit
      the page again.</P>

      <P>Note that when you go back into the editing mode, someone might have
      changed the page again.  So be quick.</P>

   </DIV>

      <P><font color="#0000FF">Here is the modified text (by someone else):</FONT></P>

      <P><HR></P>

      <TT>
        <%=pageContext.getAttribute("conflicttext",PageContext.REQUEST_SCOPE)%>
      </TT>      

      <P><HR></P>

      <P><FONT COLOR="#0000FF">And here's your text:</FONT></P>

      <TT>
        <%=pageContext.getAttribute("usertext",PageContext.REQUEST_SCOPE)%>
      </TT>

      <P><HR></P>

      <P>
       <I>Go edit <wiki:EditLink><wiki:PageName /></wiki:EditLink>.</I>
      </P>
