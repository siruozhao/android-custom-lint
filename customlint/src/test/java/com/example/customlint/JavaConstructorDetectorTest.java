package com.example.customlint;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import java.util.Collections;
import java.util.List;
import org.intellij.lang.annotations.Language;

public class JavaConstructorDetectorTest extends LintDetectorTest {

    public void testNoArgConstructor() throws Exception {
        @Language("JAVA") final String SOURCE = ""
            + "package test.pkg;\n"
            + "public class NoArgConstructorTestObject {\n"
            + "  public NoArgConstructorTestObject() {\n"
            + "    System.out.println(\"This is a no argument constructor\");\n"
            + "  }\n"
            + "}";

        lint()
            .files(java(SOURCE))
            .allowMissingSdk(true)
            .run()
            .expect("No warnings.")
            .expectFixDiffs("");
    }

    public void testSingleParameterConstructor() throws Exception {
        @Language("JAVA") final String SOURCE = ""
            + "package test.pkg;\n"
            + "public class SingleParameterConstructorTestObject {\n"
            + "  public SingleParameterConstructorTestObject(int a) {\n"
            + "  }\n"
            + "}";

        lint()
            .files(java(SOURCE))
            .run()
            .expect("No warnings.")
            .expectFixDiffs("");
    }

    public void testThreeParametersConstructor() throws Exception {
        @Language("JAVA") final String SOURCE = ""
            + "package test.pkg;\n"
            + "public class ThreeParametersConstructorTestObject {\n"
            + "\tpublic ThreeParametersConstructorTestObject(int a, long b, boolean c) {\n"
            + "\t}\n"
            + "}";

        lint()
            .files(java(SOURCE))
            .run()
            .expect("src/test/pkg/ThreeParametersConstructorTestObject.java:3: Warning: Constructor has too many parameters. [TooManyParametersConstructor]\n" +
                        // warning line doesn't contain any tabs before the actual text. so the expected warning should be "SPACE" + line
                        " public ThreeParametersConstructorTestObject(int a, long b, boolean c) {\n" +
                        " ^\n" +
                        "0 errors, 1 warnings\n")
            .expectFixDiffs(""
                                + "Fix for src/test/pkg/ThreeParametersConstructorTestObject.java line 2: Replace with "
                                + "private ThreeParametersConstructorTestObject(Builder builder) {\n"
                                + "\t\tthis.a = builder.a;\n"
                                + "\t\tthis.b = builder.b;\n"
                                + "\t\tthis.c = builder.c;\n"
                                + "\t}\n"
                                + "\n"
                                + "\tpublic static class Builder {\n"
                                + "\t\tprivate int a;\n"
                                + "\t\tprivate long b;\n"
                                + "\t\tprivate boolean c;\n"
                                + "\n"
                                + "\t\tpublic Builder() {\n"
                                + "\t\t}\n"
                                + "\n"
                                + "\t\tpublic Builder a(int a) {\n"
                                + "\t\t\tthis.a = a;\n"
                                + "\t\t\treturn this;\n"
                                + "\t\t}\n"
                                + "\n"
                                + "\t\tpublic Builder b(long b) {\n"
                                + "\t\t\tthis.b = b;\n"
                                + "\t\t\treturn this;\n"
                                + "\t\t}\n"
                                + "\n"
                                + "\t\tpublic Builder c(boolean c) {\n"
                                + "\t\t\tthis.c = c;\n"
                                + "\t\t\treturn this;\n"
                                + "\t\t}\n"
                                + "\n"
                                + "\t\tpublic ThreeParametersConstructorTestObject build() {\n"
                                + "\t\t\treturn new ThreeParametersConstructorTestObject(this);\n"
                                + "\t\t}\n"
                                + "\n"
                                + "\t}\n"
                                + ":\n"
                                + "@@ -3 +3\n"
                                + "- \tpublic ThreeParametersConstructorTestObject(int a, long b, boolean c) {\n"
                                + "+ \tprivate ThreeParametersConstructorTestObject(Builder builder) {\n"
                                + "+ \t\tthis.a = builder.a;\n"
                                + "+ \t\tthis.b = builder.b;\n"
                                + "+ \t\tthis.c = builder.c;\n"
                                + "@@ -5 +8\n"
                                + "+\n"
                                + "+ \tpublic static class Builder {\n"
                                + "+ \t\tprivate int a;\n"
                                + "+ \t\tprivate long b;\n"
                                + "+ \t\tprivate boolean c;\n"
                                + "+\n"
                                + "+ \t\tpublic Builder() {\n"
                                + "+ \t\t}\n"
                                + "+\n"
                                + "+ \t\tpublic Builder a(int a) {\n"
                                + "+ \t\t\tthis.a = a;\n"
                                + "+ \t\t\treturn this;\n"
                                + "+ \t\t}\n"
                                + "+\n"
                                + "+ \t\tpublic Builder b(long b) {\n"
                                + "+ \t\t\tthis.b = b;\n"
                                + "+ \t\t\treturn this;\n"
                                + "+ \t\t}\n"
                                + "+\n"
                                + "+ \t\tpublic Builder c(boolean c) {\n"
                                + "+ \t\t\tthis.c = c;\n"
                                + "+ \t\t\treturn this;\n"
                                + "+ \t\t}\n"
                                + "+\n"
                                + "+ \t\tpublic ThreeParametersConstructorTestObject build() {\n"
                                + "+ \t\t\treturn new ThreeParametersConstructorTestObject(this);\n"
                                + "+ \t\t}\n"
                                + "+\n"
                                + "+ \t}\n"
                                + "+\n");
    }

    @Override
    protected Detector getDetector() {
        return new JavaConstructorDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(JavaConstructorDetector.TOO_MANY_PARAMETERS_ISSUE);
    }

    @Override
    protected boolean allowCompilationErrors() {
        return true;
    }
}
