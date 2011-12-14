package org.jetbrains.jet;

import com.intellij.lang.LanguageASTFactory;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.impl.source.tree.JavaASTFactory;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.plugin.JetLanguage;
import org.junit.Assert;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * @author Stepan Koltsov
 */
public class ReadClassDataTest extends UsefulTestCase {

    protected final Disposable myTestRootDisposable = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    private JetCoreEnvironment jetCoreEnvironment;
    private File tmpdir;
    
    private final File testFile;

    public ReadClassDataTest(@NotNull File testFile) {
        this.testFile = testFile;
        setName(testFile.getName());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tmpdir = new File("tmp/" + this.getClass().getSimpleName() + "." + this.getName());
        JetTestUtils.recreateDirectory(tmpdir);
    }

    @Override
    public void tearDown() throws Exception {
        Disposer.dispose(myTestRootDisposable);
    }

    private void createMockCoreEnvironment() {
        jetCoreEnvironment = new JetCoreEnvironment(myTestRootDisposable);

        final File rtJar = new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/rt.jar");
        jetCoreEnvironment.addToClasspath(rtJar);
        jetCoreEnvironment.addToClasspath(new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/annotations.jar"));
    }

    @Override
    public void runTest() throws Exception {
        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdk(myTestRootDisposable);

        LanguageASTFactory.INSTANCE.addExplicitExtension(JavaLanguage.INSTANCE, new JavaASTFactory());


        String text = FileUtil.loadFile(testFile);

        LightVirtualFile virtualFile = new LightVirtualFile(testFile.getName(), JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);

        GenerationState state = new GenerationState(jetCoreEnvironment.getProject(), ClassBuilderFactory.BINARIES);
        AnalyzingUtils.checkForSyntacticErrors(psiFile);
        BindingContext bindingContext = state.compile(psiFile);

        ClassFileFactory classFileFactory = state.getFactory();

        CompileEnvironment.writeToOutputDirectory(classFileFactory, tmpdir.getPath());
        
        NamespaceDescriptor namespaceFromSource = (NamespaceDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, psiFile.getRootNamespace());

        Assert.assertEquals("test", namespaceFromSource.getName());

        Disposer.dispose(myTestRootDisposable);


        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdk(myTestRootDisposable);

        jetCoreEnvironment.addToClasspath(tmpdir);

        JetSemanticServices jetSemanticServices = JetSemanticServices.createSemanticServices(jetCoreEnvironment.getProject());
        JavaSemanticServices semanticServices = new JavaSemanticServices(jetCoreEnvironment.getProject(), jetSemanticServices, new BindingTraceContext());

        JavaDescriptorResolver javaDescriptorResolver = semanticServices.getDescriptorResolver();
        NamespaceDescriptor namespaceFromClass = javaDescriptorResolver.resolveNamespace("test");
        
        compareNamespaces(namespaceFromSource, namespaceFromClass);
    }

    private void compareNamespaces(@NotNull NamespaceDescriptor nsa, @NotNull NamespaceDescriptor nsb) {
        Assert.assertEquals(nsa.getName(), nsb.getName());
        System.out.println("namespace " + nsa.getName());
        for (DeclarationDescriptor ad : nsa.getMemberScope().getAllDescriptors()) {
            if (ad instanceof ClassifierDescriptor) {
                ClassifierDescriptor bd = nsb.getMemberScope().getClassifier(ad.getName());
                compareClassifiers((ClassifierDescriptor) ad, bd);
            } else if (ad instanceof FunctionDescriptor) {
                Set<FunctionDescriptor> functions = nsb.getMemberScope().getFunctions(ad.getName());
                Assert.assertTrue(functions.size() >= 1);
                Assert.assertTrue("not implemented", functions.size() == 1);
                FunctionDescriptor bd = functions.iterator().next();
                compareFunctions((FunctionDescriptor) ad, bd);
            } else {
                throw new AssertionError("Unknown member: " + ad);
            }
        }
    }

    private void compareClassifiers(@NotNull ClassifierDescriptor a, @NotNull ClassifierDescriptor b) {
        Assert.assertEquals(a.getName(), b.getName());
        System.out.println("classifier " + a.getName());
        if (a instanceof ClassDescriptor || b instanceof ClassDescriptor) {
            compareClasses((ClassDescriptor) a, (ClassDescriptor) b);
        }
    }

    private void compareClasses(@NotNull ClassDescriptor a, @NotNull ClassDescriptor b) {
        System.out.println("... is class");
    }
    
    private void compareFunctions(@NotNull FunctionDescriptor a, @NotNull FunctionDescriptor b) {
        Assert.assertEquals(a.getName(), b.getName());
        
        Assert.assertEquals(a.getTypeParameters().size(), b.getTypeParameters().size());
        for (int i = 0; i < a.getTypeParameters().size(); ++i) {
            compareAnything(TypeParameterDescriptor.class, a.getTypeParameters().get(i), b.getTypeParameters().get(i));
        }
        
        Assert.assertEquals(a.getValueParameters().size(), b.getValueParameters().size());
        for (int i = 0; i < a.getValueParameters().size(); ++i) {
            compareAnything(ValueParameterDescriptor.class, a.getValueParameters().get(i), b.getValueParameters().get(i));
        }
        compareTypes(a.getReturnType(), b.getReturnType());
        System.out.println("fun " + a.getName() + "(...): " + a.getReturnType());
    }
    
    private void compareAnything(Object a, Object b) {
        if (a instanceof JetType || b instanceof JetType) {
            compareTypes((JetType) a, (JetType) b);
        } else if (a instanceof TypeConstructor && b instanceof TypeConstructor) {
            compareTypeConstructors((TypeConstructor) a, (TypeConstructor) b);
        } else if (a instanceof List<?> || b instanceof List<?>) {
            compareLists((List<?>) a, (List<?>) b);
        } else if (a instanceof TypeProjection || b instanceof TypeProjection) {
            compareTypeProjections((TypeProjection) a, (TypeProjection) b);
        } else if (a instanceof TypeParameterDescriptor || b instanceof TypeParameterDescriptor) {
            compareTypeParameterDescriptors((TypeParameterDescriptor) a, (TypeParameterDescriptor) b);
        } else {
            Assert.assertEquals(a, b);
        }
    }
    
    private void compareTypeParameterDescriptors(TypeParameterDescriptor a, TypeParameterDescriptor b) {
        compareAnything(TypeParameterDescriptor.class, a, b);
    }
    
    private void compareTypeProjections(TypeProjection a, TypeProjection b) {
        compareAnything(TypeProjection.class, a, b);
    }
    
    private void compareLists(List<?> a, List<?> b) {
        Assert.assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); ++i) {
            compareAnything(a.get(i), b.get(i));
        }
    }

    private void compareTypeConstructors(TypeConstructor a, TypeConstructor b) {
        //compareAnything(TypeConstructor.class, a, b);
        Assert.assertEquals(a.toString(), b.toString());
    }

    private void compareTypes(JetType a, JetType b) {
        compareAnything(JetType.class, a, b);
    }

    private <T> void compareAnything(Class<T> clazz, T a, T b) {
        System.out.println("Comparing " + clazz);
        Assert.assertNotNull(a);
        Assert.assertNotNull(b);
        
        for (Method method : clazz.getMethods()) {
            if (!isGetter(method)) {
                continue;
            }

            if (JetScope.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            if (method.getName().equals("getContainingDeclaration")) {
                continue;
            }
            
            if (method.getName().equals("isReified")) {
                // TODO
                continue;
            }
            
            if (clazz.equals(ValueParameterDescriptor.class)) {
                if (method.getName().equals("isRef") || method.getName().equals("getOriginal")) {
                    continue;
                }
            }
            
            if (method.getDeclaringClass().equals(DeclarationDescriptorImpl.class)
                    || method.getDeclaringClass().equals(DeclarationDescriptor.class))
            {
                if (method.getName().equals("getOriginal")) {
                    continue;
                }
            }
            
            System.out.println(method.getName());
            Object ap = invoke(method, a);
            Object bp = invoke(method, b);
            compareAnything(ap, bp);
        }
    }
    
    private static boolean isGetter(Method method) {
        if (method.getParameterTypes().length > 0) {
            return false;
        }
        if (method.getName().matches("is.+")) {
            return method.getReturnType().equals(boolean.class);
        } else if (method.getName().matches("get.+")) {
            return true;
        } else {
            return false;
        }
    }
    
    private static Object invoke(Method method, Object thiz, Object... args) {
        try {
            return method.invoke(thiz, args);
        } catch (Exception e) {
            throw new RuntimeException("failed to invoke " + method + ": " + e);
        }
    }

    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/readClass", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new ReadClassDataTest(file);
            }
        });
    }
    
}
