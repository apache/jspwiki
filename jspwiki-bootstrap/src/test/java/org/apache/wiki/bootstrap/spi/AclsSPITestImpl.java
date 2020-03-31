package org.apache.wiki.bootstrap.spi;

import org.apache.wiki.api.core.Acl;
import org.apache.wiki.api.core.AclEntry;
import org.apache.wiki.api.spi.AclsSPI;


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
