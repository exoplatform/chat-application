package org.benjp.chat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses(value = {
        UserTestCase.class,
        SpaceTestCase.class,
        ChatTestCase.class,
        NotificationTestCase.class
})


public class AllTests {
}
