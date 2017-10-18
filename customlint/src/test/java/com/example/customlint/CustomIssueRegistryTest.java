package com.example.customlint;

import com.android.tools.lint.detector.api.Issue;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CustomIssueRegistryTest {
    private CustomIssueRegistry customIssueRegistry;

    @Before
    public void setUp() throws Exception {
        customIssueRegistry = new CustomIssueRegistry();
    }

    @Test
    public void testNumberOfIssues() throws Exception {
        int size = customIssueRegistry.getIssues().size();
        assertEquals(1, size);
    }

    @Test
    public void testGetIssues() throws Exception {
        List<Issue> actual = customIssueRegistry.getIssues();
        assertTrue(actual.contains(JavaConstructorDetector.TOO_MANY_PARAMETERS_ISSUE));
    }
}