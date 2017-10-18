package com.example.customlint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomIssueRegistry extends IssueRegistry {
    private static final List<Issue> lintIssues;
    private static final int INITIAL_CAPACITY = 1;

    public CustomIssueRegistry() {
    }

    static {
        List<Issue> issues = new ArrayList<>(INITIAL_CAPACITY);

        issues.add(JavaConstructorDetector.TOO_MANY_PARAMETERS_ISSUE);
        lintIssues = Collections.unmodifiableList(issues);
    }

    @Override
    public List<Issue> getIssues() {
        return lintIssues;
    }
}
