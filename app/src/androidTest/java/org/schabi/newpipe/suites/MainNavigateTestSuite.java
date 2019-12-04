package org.schabi.newpipe.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.schabi.newpipe.MainNavigateDownloadsTest;
import org.schabi.newpipe.MainNavigateSwitchServiceTest;
import org.schabi.newpipe.MainNavigateTabsTest;

// Runs all unit tests.
@RunWith(Suite.class)
@Suite.SuiteClasses({
        MainNavigateDownloadsTest.class,
        MainNavigateSwitchServiceTest.class,
        MainNavigateTabsTest.class})
public class MainNavigateTestSuite {}