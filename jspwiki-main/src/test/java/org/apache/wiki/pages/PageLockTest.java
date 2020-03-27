package org.apache.wiki.pages;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;


public class PageLockTest {
    
    @Test
    public void testPageLockIsExpired() throws Exception {
        final TestEngine engine = new TestEngine( TestEngine.getTestProperties() );
        final Page page = Wiki.contents().page( engine, "test" );
        final PageLock lock1 = new PageLock( page, "user", new Date( System.currentTimeMillis() - 10000 ), new Date( System.currentTimeMillis() - 5000 ) );
        final PageLock lock2 = new PageLock( page, "user", new Date( System.currentTimeMillis() - 10000 ), new Date( System.currentTimeMillis() + 5000 ) );
        Assertions.assertTrue( lock1.isExpired() );
        Assertions.assertFalse( lock2.isExpired() );
    }

}
