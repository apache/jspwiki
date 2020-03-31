package org.apache.wiki.api.spi;

import org.apache.wiki.api.core.Acl;
import org.apache.wiki.api.core.AclEntry;


public class AclsSPITestImpl implements AclsSPI {

    @Override
    public Acl acl() {
        return null;
    }

    @Override
    public AclEntry entry() {
        return null;
    }

}
