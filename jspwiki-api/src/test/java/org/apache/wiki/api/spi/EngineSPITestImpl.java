package org.apache.wiki.api.spi;

import org.apache.wiki.api.core.Engine;

import javax.servlet.ServletContext;
import java.util.Properties;


public class EngineSPITestImpl implements EngineSPI {

    @Override
    public Engine find( final ServletContext context, final Properties props ) {
        return null;
    }
}
