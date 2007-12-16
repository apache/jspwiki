import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Simple utility that shows you a sorted list of properties
 * that are missing in a i18n file 
 * (as diff from the default en properties). 
 * 
 * @author Christoph Sauer
 *
 */
public class MissingTranslations 
{

    // Change this to your settings...
    static String base = "C:/workspace/JSPWiki HEAD"; 
    static String suffix = "de_DE";
    
    public static void main(String[] args) throws IOException
    {
        diff ("/etc/i18n/CoreResources.properties", 
              "/etc/i18n/CoreResources_" + suffix + ".properties");

        diff ("/etc/i18n/templates/default.properties", 
              "/etc/i18n/templates/default_" + suffix + ".properties");

        diff ("/src/com/ecyrd/jspwiki/plugin/PluginResources.properties", 
              "/src/com/ecyrd/jspwiki/plugin/PluginResources_" + suffix + ".properties");
    }   
    
    public static void diff(String en, String other) throws FileNotFoundException, IOException {

        //Standard Properties
        Properties p = new Properties();
        p.load( new FileInputStream(new File(base + en)) );

        Properties p2 = new Properties();
        p2.load( new FileInputStream(new File(base + other)) );

        System.out.println("Missing Properties in " + other + ":");
        System.out.println("------------------------------------");
        Iterator iter = sortedNames(p).iterator();
        while(iter.hasNext()) 
        {
            String name = (String)iter.next();
            String value = p.getProperty(name);
            
            if (p2.get(name) == null) {
                System.out.println(name + " = " + value);
            }
        }
        System.out.println("");
    }
    
    private static List sortedNames(Properties p)
    {
        List list = new ArrayList();
        Enumeration iter = p.propertyNames();
        while(iter.hasMoreElements()) 
        {
            list.add(iter.nextElement());
        }        
        
        Collections.sort(list);
        return list;
    }
}