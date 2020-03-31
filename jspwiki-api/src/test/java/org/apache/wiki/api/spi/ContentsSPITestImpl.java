package org.apache.wiki.api.spi;

import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;


public class ContentsSPITestImpl implements ContentsSPI {

    @Override
    public Attachment attachment( final Engine engine, final String parentPage, final String fileName ) {
        return null;
    }

    @Override
    public Page page( final Engine engine, final String name ) {
        return null;
    }

}
