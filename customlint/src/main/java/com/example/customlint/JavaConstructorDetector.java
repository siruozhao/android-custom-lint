package com.example.customlint;

import com.android.annotations.NonNull;
import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.ULiteralExpression;

public class JavaConstructorDetector extends Detector implements Detector.UastScanner {
    private static final String TOO_MANY_PARAMETERS_ISSUE_ID = "TooManyParametersConstructor";
    private static final String TOO_MANY_PARAMETERS_ISSUE_DESCRIPTION = "Constructor has too many parameters.";
    private static final String TOO_MANY_PARAMETERS_ISSUE_EXPLANATION = "Switching to build pattern improves readability and scalability.";
    private static final Category TOO_MANY_PARAMETERS_ISSUE_CATEGORY = Category.CORRECTNESS;
    private static final int TOO_MANY_PARAMETERS_ISSUE_PRIORITY = 6;
    private static final Severity TOO_MANY_PARAMETERS_ISSUE_SEVERITY = Severity.WARNING;
    private static final Implementation IMPLEMENTATION = new Implementation(JavaConstructorDetector.class, Scope.JAVA_FILE_SCOPE);

    /** Issue describing the problem and pointing to the detector implementation */
    public static final Issue TOO_MANY_PARAMETERS_ISSUE = Issue.create(
        // ID: used in @SuppressLint warnings etc
        TOO_MANY_PARAMETERS_ISSUE_ID,

        // Title -- shown in the IDE's preference dialog, as category headers in the
        // Analysis results window, etc
        TOO_MANY_PARAMETERS_ISSUE_DESCRIPTION,

        // Full explanation of the issue; you can use some markdown markup such as
        // `monospace`, *italic*, and **bold**.
        TOO_MANY_PARAMETERS_ISSUE_EXPLANATION,
        TOO_MANY_PARAMETERS_ISSUE_CATEGORY,
        TOO_MANY_PARAMETERS_ISSUE_PRIORITY,
        TOO_MANY_PARAMETERS_ISSUE_SEVERITY,
        IMPLEMENTATION);

    private static final int PARAMETERS_COUNT_LIMIT;

    static {
        int parametersCount = 0;

        String countValue = System.getenv("ANDROID_LINT_CONSTRUCTOR_PARAMETERS_COUNT_LIMIT");
        if (countValue != null) {
            try {
                parametersCount = Integer.parseInt(countValue);
            } catch (NumberFormatException e) {
                // pass: set to default below
            }
        }

        if (parametersCount <= 0) {
            parametersCount = 2;
        }

        PARAMETERS_COUNT_LIMIT = parametersCount;
    }

    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Collections.singletonList(UClass.class);
    }

    @Override
    public UElementHandler createUastHandler(final JavaContext context) {
        final String PUBLIC = "public";
        final String PRIVATE = "private";
        final String STATIC = "static";
        final String BUILD = "build";
        final String BUILDER = "Builder";
        final String NEW = "new";
        final String CLASS = "class";
        final String SPACE = " ";
        final String LEFT_PRENTHESIS = "(";
        final String RIGHT_PRENTHESIS = ")";
        final String LEFT_CURLY_BRACKET = "{";
        final String RIGHT_CURLY_BRACKET = "}";
        final String NEW_LINE = "\n";
        final String THIS = "this";
        final String DOT = ".";
        final String EQUALS = "=";
        final String TAB = "\t";
        final String SEMICOLON = ";";
        final String RETURN = "return";

        // Not: Visiting UAST nodes is a pretty general purpose mechanism;
        // Lint has specialized support to do common things like "visit every class
        // that extends a given super class or implements a given interface", and
        // "visit every call site that calls a method by a given name" etc.
        // Take a careful look at UastScanner and the various existing lint check
        // implementations before doing things the "hard way".
        // Also be aware of context.getJavaEvaluator() which provides a lot of
        // utility functionality.
        return new UElementHandler() {

            @Override
            public void visitClass(UClass uClass) {
                if (uClass != null) {
                    final PsiMethod[] constructors = uClass.getConstructors();
                    if (constructors.length > 0) {
                        for (PsiMethod constructor : constructors) {
                            final PsiParameterList parameters = constructor.getParameterList();
                            if (parameters.getParametersCount() > PARAMETERS_COUNT_LIMIT) {
                                final LintFix fix = fix()
                                    .replace()
                                    .text(constructor.getText())
                                    .with(getBuilderPatternText(constructor))
                                    .reformat(false)
                                    .build();

                                context.report(TOO_MANY_PARAMETERS_ISSUE,
                                               constructor,
                                               context.getLocation(constructor),
                                               TOO_MANY_PARAMETERS_ISSUE.getBriefDescription(TextFormat.TEXT),
                                               fix);
                            }
                        }
                    }
                }
            }

            private String getBuilderPatternText(final PsiMethod constructor) {
                final StringBuilder stringBuilder = new StringBuilder();
                final String objectName = constructor.getName();
                final PsiParameterList parameterList = constructor.getParameterList();
                final PsiParameter[] parameters = parameterList.getParameters();

                stringBuilder.append(getBuilderConstructorText(objectName, parameters));

                stringBuilder.append(getStaticFactoryText(objectName, parameters));

                return stringBuilder.toString();
            }

            private String getStaticFactoryText(@NonNull final String objectName, @NonNull final PsiParameter[] parameters) {
                final StringBuilder stringBuilder = new StringBuilder();
                //    public static class Builder {
                //        private type fieldName;
                //        ...
                //
                //        public Builder() {
                //        }

                //        public Builder fieldName(type fieldName) {
                //            this.fieldName = fieldName;
                //            return this;
                //        }
                //        ...

                //        public ObjectName build() {
                //            return new ObjectName(this);
                //        }
                //    }

                //    public static class Builder {
                stringBuilder.append(TAB);
                stringBuilder.append(PUBLIC);
                stringBuilder.append(SPACE);
                stringBuilder.append(STATIC);
                stringBuilder.append(SPACE);
                stringBuilder.append(CLASS);
                stringBuilder.append(SPACE);
                stringBuilder.append(BUILDER);
                stringBuilder.append(SPACE);
                stringBuilder.append(LEFT_CURLY_BRACKET);
                stringBuilder.append(NEW_LINE);

                stringBuilder.append(getStaticFactoryFieldsText(parameters));

                stringBuilder.append(getStaticFactoryBuilderText());

                stringBuilder.append(getStaticFactorySettersText(parameters));

                stringBuilder.append(getStaticFactoryBuildMethodText(objectName));

                //    }
                stringBuilder.append(TAB);
                stringBuilder.append(RIGHT_CURLY_BRACKET);
                stringBuilder.append(NEW_LINE);

                return stringBuilder.toString();
            }

            private String getStaticFactoryBuilderText() {
                final StringBuilder stringBuilder = new StringBuilder();

                //        public Builder() {
                stringBuilder.append(TAB);
                stringBuilder.append(TAB);
                stringBuilder.append(PUBLIC);
                stringBuilder.append(SPACE);
                stringBuilder.append(BUILDER);
                stringBuilder.append(LEFT_PRENTHESIS);
                stringBuilder.append(RIGHT_PRENTHESIS);
                stringBuilder.append(SPACE);
                stringBuilder.append(LEFT_CURLY_BRACKET);
                stringBuilder.append(NEW_LINE);

                //        }
                stringBuilder.append(TAB);
                stringBuilder.append(TAB);
                stringBuilder.append(RIGHT_CURLY_BRACKET);
                stringBuilder.append(NEW_LINE);

                stringBuilder.append(NEW_LINE);

                return stringBuilder.toString();
            }

            private String getStaticFactoryBuildMethodText(@NonNull final String objectName) {
                final StringBuilder stringBuilder = new StringBuilder();

                //        public ObjectName build() {
                stringBuilder.append(TAB);
                stringBuilder.append(TAB);
                stringBuilder.append(PUBLIC);
                stringBuilder.append(SPACE);
                stringBuilder.append(objectName);
                stringBuilder.append(SPACE);
                stringBuilder.append(BUILD);
                stringBuilder.append(LEFT_PRENTHESIS);
                stringBuilder.append(RIGHT_PRENTHESIS);
                stringBuilder.append(SPACE);
                stringBuilder.append(LEFT_CURLY_BRACKET);
                stringBuilder.append(NEW_LINE);

                //            return new ObjectName(this);
                stringBuilder.append(TAB);
                stringBuilder.append(TAB);
                stringBuilder.append(TAB);
                stringBuilder.append(RETURN);
                stringBuilder.append(SPACE);
                stringBuilder.append(NEW);
                stringBuilder.append(SPACE);
                stringBuilder.append(objectName);
                stringBuilder.append(LEFT_PRENTHESIS);
                stringBuilder.append(THIS);
                stringBuilder.append(RIGHT_PRENTHESIS);
                stringBuilder.append(SEMICOLON);
                stringBuilder.append(NEW_LINE);

                //        }
                stringBuilder.append(TAB);
                stringBuilder.append(TAB);
                stringBuilder.append(RIGHT_CURLY_BRACKET);
                stringBuilder.append(NEW_LINE);

                stringBuilder.append(NEW_LINE);

                return stringBuilder.toString();
            }

            private String getStaticFactorySettersText(@NonNull final PsiParameter[] parameters) {
                final StringBuilder stringBuilder = new StringBuilder();

                for (PsiParameter parameter : parameters) {
                    final String parameterName = parameter.getName();
                    final String parameterType = parameter.getType().getPresentableText();

                    //        public Builder fieldName(type fieldName) {
                    stringBuilder.append(TAB);
                    stringBuilder.append(TAB);
                    stringBuilder.append(PUBLIC);
                    stringBuilder.append(SPACE);
                    stringBuilder.append(BUILDER);
                    stringBuilder.append(SPACE);
                    stringBuilder.append(parameterName);
                    stringBuilder.append(LEFT_PRENTHESIS);
                    stringBuilder.append(parameterType);
                    stringBuilder.append(SPACE);
                    stringBuilder.append(parameterName);
                    stringBuilder.append(RIGHT_PRENTHESIS);
                    stringBuilder.append(SPACE);
                    stringBuilder.append(LEFT_CURLY_BRACKET);
                    stringBuilder.append(NEW_LINE);

                    //            this.fieldName = fieldName;
                    stringBuilder.append(TAB);
                    stringBuilder.append(TAB);
                    stringBuilder.append(TAB);
                    stringBuilder.append(THIS);
                    stringBuilder.append(DOT);
                    stringBuilder.append(parameterName);
                    stringBuilder.append(SPACE);
                    stringBuilder.append(EQUALS);
                    stringBuilder.append(SPACE);
                    stringBuilder.append(parameterName);
                    stringBuilder.append(SEMICOLON);
                    stringBuilder.append(NEW_LINE);

                    //            return this;
                    stringBuilder.append(TAB);
                    stringBuilder.append(TAB);
                    stringBuilder.append(TAB);
                    stringBuilder.append(RETURN);
                    stringBuilder.append(SPACE);
                    stringBuilder.append(THIS);
                    stringBuilder.append(SEMICOLON);
                    stringBuilder.append(NEW_LINE);

                    //        }
                    stringBuilder.append(TAB);
                    stringBuilder.append(TAB);
                    stringBuilder.append(RIGHT_CURLY_BRACKET);
                    stringBuilder.append(NEW_LINE);
                    stringBuilder.append(NEW_LINE);
                }

                return stringBuilder.toString();
            }

            private String getStaticFactoryFieldsText(@NonNull final PsiParameter[] parameters) {
                final StringBuilder stringBuilder = new StringBuilder();

                for (PsiParameter parameter : parameters) {
                    final String parameterName = parameter.getName();
                    final String parameterType = parameter.getType().getPresentableText();

                    //        private type servingSize;
                    stringBuilder.append(TAB);
                    stringBuilder.append(TAB);
                    stringBuilder.append(PRIVATE);
                    stringBuilder.append(SPACE);
                    stringBuilder.append(parameterType);
                    stringBuilder.append(SPACE);
                    stringBuilder.append(parameterName);
                    stringBuilder.append(SEMICOLON);
                    stringBuilder.append(NEW_LINE);
                }
                stringBuilder.append(NEW_LINE);

                return stringBuilder.toString();
            }

            private String getBuilderConstructorText(@NonNull final String objectName, @NonNull final PsiParameter[] parameters) {

                //    private NutritionFacts(Builder builder) {
                //        fieldName  = builder.fieldName;
                //        ...
                //    }

                final StringBuilder stringBuilder = new StringBuilder();

                //    private ObjectName(Builder builder) {
                stringBuilder.append(PRIVATE);
                stringBuilder.append(SPACE);
                stringBuilder.append(objectName);
                stringBuilder.append(LEFT_PRENTHESIS);
                stringBuilder.append(BUILDER);
                stringBuilder.append(SPACE);
                stringBuilder.append(BUILDER.toLowerCase());
                stringBuilder.append(RIGHT_PRENTHESIS);
                stringBuilder.append(SPACE);
                stringBuilder.append(LEFT_CURLY_BRACKET);
                stringBuilder.append(NEW_LINE);

                for (PsiParameter parameter : parameters) {
                    //        this.fieldName = builder.fieldName;
                    final String parameterName = parameter.getName();
                    stringBuilder.append(TAB);
                    stringBuilder.append(TAB);
                    stringBuilder.append(THIS);
                    stringBuilder.append(DOT);
                    stringBuilder.append(parameterName);
                    stringBuilder.append(SPACE);
                    stringBuilder.append(EQUALS);
                    stringBuilder.append(SPACE);
                    stringBuilder.append(BUILDER.toLowerCase());
                    stringBuilder.append(DOT);
                    stringBuilder.append(parameterName);
                    stringBuilder.append(SEMICOLON);
                    stringBuilder.append(NEW_LINE);
                }

                //    }
                stringBuilder.append(TAB);
                stringBuilder.append(RIGHT_CURLY_BRACKET);
                stringBuilder.append(NEW_LINE);
                stringBuilder.append(NEW_LINE);

                return stringBuilder.toString();
            }
        };
    }
}
