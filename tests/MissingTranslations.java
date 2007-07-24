import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Simple utility that shows you what properties
 * are missing in a i18n file from the default properties
 * 
 * @author Christoph Sauer
 *
 */
public class MissingTranslations
{

    // Change this to your settings...
    static String base = "C:/workspace/JSPWiki"; 
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
        Enumeration enm = p.propertyNames();        
        while(enm.hasMoreElements()) {
            String name = (String)enm.nextElement();
            String value = p.getProperty(name);
            
            if (p2.get(name) == null) {
                System.out.println(name + " = " + value);
            }
        }
        System.out.println("");

    }
}
