<SCRIPT type="text/javascript">

    var IE4 = (document.all && !document.getElementById) ? true : false;
    var NS4 = (document.layers) ? true : false;
    var IE5 = (document.all && document.getElementById) ? true : false;
    var NS6 = (document.getElementById && !document.all) ? true : false;
    var IE  = IE4 || IE5;
    var NS  = NS4 || NS6;
    var Mac = (navigator.platform.indexOf("Mac") == -1) ? false : true;

    var sheet;

    if( NS4 )
    {
        sheet = "jspwiki_ns.css";
    }
    else if( Mac )
    {
        sheet = "jspwiki_mac.css";
    }
    else
    {
        // Let's assume all the rest of the browsers are sane
        // and standard's compliant.
        sheet = "jspwiki_ie.css";
    }

    document.write("<link rel=\"stylesheet\" href=\""+sheet+"\">");

</SCRIPT>
<NOSCRIPT>
    <!-- User has no JavaScript support.  Thus, it is unlikely that
         his stylesheets would work either.  So, no stylesheets. -->
</NOSCRIPT>