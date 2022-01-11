package org.apache.wiki.its;

import com.codeborne.selenide.junit5.ScreenShooterExtension;
import org.apache.wiki.its.environment.Env;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith( ScreenShooterExtension.class )
public class WithIntegrationTestSetup {

    @BeforeAll
    public static void setUp() {
        Env.setUp();
    }

}
