package org.schabi.newpipe.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.schabi.newpipe.UnusAnnusFirstVidMainPlayerTest;
import org.schabi.newpipe.UnusAnnusSearchChannelTest;
import org.schabi.newpipe.UnusAnnusSearchFirstVidTest;
import org.schabi.newpipe.UnusAnnusSubscribeChannelTest;

// Runs all unit tests.
@RunWith(Suite.class)
@Suite.SuiteClasses({
        UnusAnnusFirstVidMainPlayerTest.class,
        UnusAnnusSearchChannelTest.class,
        UnusAnnusSearchFirstVidTest.class,
        UnusAnnusSubscribeChannelTest.class})
public class UnusAnnusTestSuite {}