/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.generated.cases.references;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.AnalysisApiFirStandaloneModeTestConfiguratorFactory;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisSessionMode;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode;
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.AbstractMultiModuleReferenceResolveWithResolveExtensionTest;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("analysis/analysis-api/testData/resolveExtensions/multiModule/referenceResolve")
@TestDataPath("$PROJECT_ROOT")
public class FirStandaloneNormalAnalysisSourceModuleMultiModuleReferenceResolveWithResolveExtensionTestGenerated extends AbstractMultiModuleReferenceResolveWithResolveExtensionTest {
    @NotNull
    @Override
    public AnalysisApiTestConfigurator getConfigurator() {
        return AnalysisApiFirStandaloneModeTestConfiguratorFactory.INSTANCE.createConfigurator(
            new AnalysisApiTestConfiguratorFactoryData(
                FrontendKind.Fir,
                TestModuleKind.Source,
                AnalysisSessionMode.Normal,
                AnalysisApiMode.Standalone
            )
        );
    }

    @Test
    public void testAllFilesPresentInReferenceResolve() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/resolveExtensions/multiModule/referenceResolve"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Nested
    @TestMetadata("analysis/analysis-api/testData/resolveExtensions/multiModule/referenceResolve/extendedModuleDependency")
    @TestDataPath("$PROJECT_ROOT")
    public class ExtendedModuleDependency {
        @Test
        public void testAllFilesPresentInExtendedModuleDependency() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/resolveExtensions/multiModule/referenceResolve/extendedModuleDependency"), Pattern.compile("^(.+)\\.kt$"), null, true);
        }

        @Test
        @TestMetadata("classMember.kt")
        public void testClassMember() throws Exception {
            runTest("analysis/analysis-api/testData/resolveExtensions/multiModule/referenceResolve/extendedModuleDependency/classMember.kt");
        }

        @Test
        @TestMetadata("extensionFunction.kt")
        public void testExtensionFunction() throws Exception {
            runTest("analysis/analysis-api/testData/resolveExtensions/multiModule/referenceResolve/extendedModuleDependency/extensionFunction.kt");
        }

        @Test
        @TestMetadata("shadowedDeclaration.kt")
        public void testShadowedDeclaration() throws Exception {
            runTest("analysis/analysis-api/testData/resolveExtensions/multiModule/referenceResolve/extendedModuleDependency/shadowedDeclaration.kt");
        }

        @Test
        @TestMetadata("shadowedJava.kt")
        public void testShadowedJava() throws Exception {
            runTest("analysis/analysis-api/testData/resolveExtensions/multiModule/referenceResolve/extendedModuleDependency/shadowedJava.kt");
        }

        @Test
        @TestMetadata("shadowedOverload.kt")
        public void testShadowedOverload() throws Exception {
            runTest("analysis/analysis-api/testData/resolveExtensions/multiModule/referenceResolve/extendedModuleDependency/shadowedOverload.kt");
        }

        @Test
        @TestMetadata("topLevelFunction.kt")
        public void testTopLevelFunction() throws Exception {
            runTest("analysis/analysis-api/testData/resolveExtensions/multiModule/referenceResolve/extendedModuleDependency/topLevelFunction.kt");
        }
    }
}
