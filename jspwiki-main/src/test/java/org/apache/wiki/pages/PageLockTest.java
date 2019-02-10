package org.apache.wiki.pages;

import java.util.Date;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class PageLockTest {
    
    @Test
    public void testPageLockIsExpired() throws Exception {
        TestEngine engine = new TestEngine( TestEngine.getTestProperties() );
        WikiPage page = new WikiPage( engine, "test" );
        PageLock lock1 = new PageLock( page, "user", new Date( System.currentTimeMillis() - 10000 ), new Date( System.currentTimeMillis() - 5000 ) );
        PageLock lock2 = new PageLock( page, "user", new Date( System.currentTimeMillis() - 10000 ), new Date( System.currentTimeMillis() + 5000 ) );
        Assertions.assertTrue( lock1.isExpired() );
        Assertions.assertFalse( lock2.isExpired() );
    }

}
