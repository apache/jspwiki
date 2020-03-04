package org.apache.wiki.util.comparators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.Principal;


public class PrincipalComparatorTest {

    @Test
    public void testComparatorOrder() {
        final Principal p1 = () -> "a";
        final Principal p2 = () -> "b";
        final PrincipalComparator pc = new PrincipalComparator();

        Assertions.assertEquals( -1, pc.compare( p1, p2 ) );
        Assertions.assertEquals( 0, pc.compare( p1, p1 ) );
        Assertions.assertEquals( 1, pc.compare( p2, p1 ) );

        Assertions.assertThrows( NullPointerException.class, () -> pc.compare( null, p1  ) );
        Assertions.assertThrows( NullPointerException.class, () -> pc.compare( p1, null  ) );
        Assertions.assertThrows( NullPointerException.class, () -> pc.compare( null, null  ) );
    }

}
