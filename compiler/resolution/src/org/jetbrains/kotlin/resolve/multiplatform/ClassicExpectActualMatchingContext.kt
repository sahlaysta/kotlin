/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.components.ClassicTypeSystemContextForCS
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext.AnnotationDelegate
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getKotlinTypeRefiner
import org.jetbrains.kotlin.resolve.descriptorUtil.isSourceAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.isTypeRefinementEnabled
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.castAll
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.keysToMap

class ClassicExpectActualMatchingContext(val platformModule: ModuleDescriptor) : ExpectActualMatchingContext<MemberDescriptor>,
    TypeSystemContext by ClassicTypeSystemContextForCS(platformModule.builtIns, KotlinTypeRefiner.Default)
{
    override val shouldCheckReturnTypesOfCallables: Boolean
        get() = true

    private fun DeclarationSymbolMarker.asDescriptor(): DeclarationDescriptor = this as DeclarationDescriptor
    private fun CallableSymbolMarker.asDescriptor(): CallableDescriptor = this as CallableDescriptor
    private fun FunctionSymbolMarker.asDescriptor(): FunctionDescriptor = this as FunctionDescriptor
    private fun PropertySymbolMarker.asDescriptor(): PropertyDescriptor = this as PropertyDescriptor
    private fun ValueParameterSymbolMarker.asDescriptor(): ValueParameterDescriptor = this as ValueParameterDescriptor
    private fun TypeParameterSymbolMarker.asDescriptor(): TypeParameterDescriptor = this as TypeParameterDescriptor
    private fun ClassLikeSymbolMarker.asDescriptor(): ClassifierDescriptorWithTypeParameters = this as ClassifierDescriptorWithTypeParameters
    private fun RegularClassSymbolMarker.asDescriptor(): ClassDescriptor = this as ClassDescriptor
    private fun TypeAliasSymbolMarker.asDescriptor(): TypeAliasDescriptor = this as TypeAliasDescriptor
    private inline fun <reified T : DeclarationDescriptor> DeclarationSymbolMarker.safeAsDescriptor(): T? = this as? T

    override val RegularClassSymbolMarker.classId: ClassId
        get() = (this as ClassifierDescriptor).classId!!
    override val TypeAliasSymbolMarker.classId: ClassId
        get() = (this as ClassifierDescriptor).classId!!
    override val CallableSymbolMarker.callableId: CallableId
        get() {
            val descriptor = asDescriptor()
            return when (val parent = descriptor.containingDeclaration) {
                is PackageFragmentDescriptor -> CallableId(parent.fqName, descriptor.name)
                is ClassifierDescriptor -> CallableId(parent.classId!!, descriptor.name)
                else -> error("Callable descriptor without callableId: $descriptor")
            }
        }
    override val TypeParameterSymbolMarker.parameterName: Name
        get() = asDescriptor().name
    override val ValueParameterSymbolMarker.parameterName: Name
        get() = asDescriptor().name

    override fun TypeAliasSymbolMarker.expandToRegularClass(): RegularClassSymbolMarker? {
        return asDescriptor().classDescriptor
    }

    override val RegularClassSymbolMarker.classKind: ClassKind
        get() = asDescriptor().kind
    override val RegularClassSymbolMarker.isCompanion: Boolean
        get() = safeAsDescriptor<ClassDescriptor>()?.isCompanionObject == true
    override val RegularClassSymbolMarker.isInner: Boolean
        get() = asDescriptor().isInner
    override val RegularClassSymbolMarker.isInline: Boolean
        get() = safeAsDescriptor<ClassDescriptor>()?.isInline == true
    override val RegularClassSymbolMarker.isValue: Boolean
        get() = safeAsDescriptor<ClassDescriptor>()?.isValue == true
    override val RegularClassSymbolMarker.isFun: Boolean
        get() = safeAsDescriptor<ClassDescriptor>()?.isFun == true
    override val ClassLikeSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
        get() = asDescriptor().declaredTypeParameters
    override val ClassLikeSymbolMarker.modality: Modality
        get() = asDescriptor().modality
    override val ClassLikeSymbolMarker.visibility: Visibility
        get() = asDescriptor().visibility.delegate
    override val CallableSymbolMarker.modality: Modality?
        get() = safeAsDescriptor<CallableMemberDescriptor>()?.modality
    override val CallableSymbolMarker.visibility: Visibility
        get() = asDescriptor().visibility.delegate
    override val RegularClassSymbolMarker.superTypes: List<KotlinTypeMarker>
        get() = asDescriptor().typeConstructor.supertypes.toList()
    override val CallableSymbolMarker.isExpect: Boolean
        get() = safeAsDescriptor<MemberDescriptor>()?.isExpect == true

    override val CallableSymbolMarker.isInline: Boolean
        get() = when (this) {
            is FunctionDescriptor -> isInline
            is PropertyDescriptor -> getter?.isInline == true
            else -> false
        }

    override val CallableSymbolMarker.isSuspend: Boolean
        get() = when (this) {
            is FunctionDescriptor -> isSuspend
            is PropertyDescriptor -> getter?.isSuspend == true
            else -> false
        }

    override val CallableSymbolMarker.isExternal: Boolean
        get() = safeAsDescriptor<MemberDescriptor>()?.isExternal == true
    override val CallableSymbolMarker.isInfix: Boolean
        get() = safeAsDescriptor<FunctionDescriptor>()?.isInfix == true
    override val CallableSymbolMarker.isOperator: Boolean
        get() = safeAsDescriptor<FunctionDescriptor>()?.isOperator == true
    override val CallableSymbolMarker.isTailrec: Boolean
        get() = safeAsDescriptor<FunctionDescriptor>()?.isTailrec == true
    override val PropertySymbolMarker.isVar: Boolean
        get() = asDescriptor().isVar
    override val PropertySymbolMarker.isLateinit: Boolean
        get() = asDescriptor().isLateInit
    override val PropertySymbolMarker.isConst: Boolean
        get() = asDescriptor().isConst
    override val PropertySymbolMarker.setter: FunctionSymbolMarker?
        get() = asDescriptor().setter

    @OptIn(UnsafeCastFunction::class)
    override fun createExpectActualTypeParameterSubstitutor(
        expectTypeParameters: List<TypeParameterSymbolMarker>,
        actualTypeParameters: List<TypeParameterSymbolMarker>,
        parentSubstitutor: TypeSubstitutorMarker?,
    ): TypeSubstitutorMarker {
        val expectParameters = expectTypeParameters.castAll<TypeParameterDescriptor>()
        val actualParameters = actualTypeParameters.castAll<TypeParameterDescriptor>()
        val substitutor = TypeSubstitutor.create(
            TypeConstructorSubstitution.createByParametersMap(expectParameters.keysToMap {
                actualParameters[it.index].defaultType.asTypeProjection()
            })
        )
        return when (parentSubstitutor) {
            null -> substitutor
            is TypeSubstitutor -> TypeSubstitutor.createChainedSubstitutor(parentSubstitutor.substitution, substitutor.substitution)
            else -> error("Unsupported substitutor type: $parentSubstitutor")
        }
    }

    override fun RegularClassSymbolMarker.collectAllMembers(isActualDeclaration: Boolean): List<DeclarationSymbolMarker> {
        return asDescriptor().getMembers(name = null)
    }

    override fun RegularClassSymbolMarker.getMembersForExpectClass(name: Name): List<DeclarationSymbolMarker> {
        return asDescriptor().getMembers(name)
    }

    private fun ClassDescriptor.getMembers(name: Name? = null): List<MemberDescriptor> {
        val nameFilter = if (name != null) { it -> it == name } else MemberScope.ALL_NAME_FILTER
        return defaultType.memberScope
            .getDescriptorsFiltered(nameFilter = nameFilter)
            .filterIsInstance<MemberDescriptor>()
            .filterNot(DescriptorUtils::isEnumEntry)
            .plus(constructors.filter { nameFilter(it.name) })
    }

    override fun RegularClassSymbolMarker.collectEnumEntryNames(): List<Name> {
        return asDescriptor()
            .unsubstitutedMemberScope
            .getDescriptorsFiltered()
            .filter(DescriptorUtils::isEnumEntry)
            .map { it.name }
    }

    override val CallableSymbolMarker.dispatchReceiverType: KotlinTypeMarker?
        get() = asDescriptor().dispatchReceiverParameter?.type
    override val CallableSymbolMarker.extensionReceiverType: KotlinTypeMarker?
        get() = asDescriptor().extensionReceiverParameter?.type
    override val CallableSymbolMarker.returnType: KotlinTypeMarker
        get() = asDescriptor().returnType!!
    override val CallableSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
        get() = asDescriptor().typeParameters
    override val FunctionSymbolMarker.valueParameters: List<ValueParameterSymbolMarker>
        get() = asDescriptor().valueParameters
    override val ValueParameterSymbolMarker.isVararg: Boolean
        get() = asDescriptor().varargElementType != null
    override val ValueParameterSymbolMarker.isNoinline: Boolean
        get() = asDescriptor().isNoinline
    override val ValueParameterSymbolMarker.isCrossinline: Boolean
        get() = asDescriptor().isCrossinline
    override val ValueParameterSymbolMarker.hasDefaultValue: Boolean
        get() = asDescriptor().declaresDefaultValue()

    override fun CallableSymbolMarker.isAnnotationConstructor(): Boolean {
        val descriptor = safeAsDescriptor<ConstructorDescriptor>() ?: return false
        return DescriptorUtils.isAnnotationClass(descriptor.constructedClass)
    }

    override val TypeParameterSymbolMarker.bounds: List<KotlinTypeMarker>
        get() = asDescriptor().upperBounds
    override val TypeParameterSymbolMarker.variance: Variance
        get() = asDescriptor().variance
    override val TypeParameterSymbolMarker.isReified: Boolean
        get() = asDescriptor().isReified

    override fun areCompatibleExpectActualTypes(expectType: KotlinTypeMarker?, actualType: KotlinTypeMarker?): Boolean {
        if (expectType == null) return actualType == null
        if (actualType == null) return false

        require(expectType is KotlinType && actualType is KotlinType)
        return if (platformModule.isTypeRefinementEnabled()) {
            areCompatibleTypesViaTypeRefinement(expectType, actualType)
        } else {
            areCompatibleTypesViaTypeContext(expectType, actualType)
        }
    }

    override val RegularClassSymbolMarker.defaultType: KotlinTypeMarker
        get() = asDescriptor().defaultType

    override fun actualTypeIsSubtypeOfExpectType(expectType: KotlinTypeMarker, actualType: KotlinTypeMarker): Boolean {
        shouldNotBeCalled("Checking for subtyping is used only in FIR and IR implementations")
    }

    @OptIn(TypeRefinement::class)
    private fun areCompatibleTypesViaTypeRefinement(a: KotlinType, b: KotlinType): Boolean {
        val typeRefinerForPlatformModule = platformModule.getKotlinTypeRefiner().let { moduleRefiner ->
            if (moduleRefiner is KotlinTypeRefiner.Default)
                KotlinTypeRefinerImpl.createStandaloneInstanceFor(platformModule)
            else
                moduleRefiner
        }

        return areCompatibleTypes(
            a, b,
            typeSystemContext = SimpleClassicTypeSystemContext,
            kotlinTypeRefiner = typeRefinerForPlatformModule,
        )
    }

    private fun areCompatibleTypesViaTypeContext(a: KotlinType, b: KotlinType): Boolean {
        val typeSystemContext = object : ClassicTypeSystemContext {
            override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
                require(c1 is TypeConstructor)
                require(c2 is TypeConstructor)
                return isExpectedClassAndActualTypeAlias(c1, c2, platformModule) ||
                        isExpectedClassAndActualTypeAlias(c2, c1, platformModule) ||
                        super.areEqualTypeConstructors(c1, c2)
            }
        }

        return areCompatibleTypes(
            a, b,
            typeSystemContext = typeSystemContext,
            kotlinTypeRefiner = KotlinTypeRefiner.Default,
        )
    }

    private fun areCompatibleTypes(
        a: KotlinType,
        b: KotlinType,
        typeSystemContext: ClassicTypeSystemContext,
        kotlinTypeRefiner: KotlinTypeRefiner,
    ): Boolean {
        with(NewKotlinTypeCheckerImpl(kotlinTypeRefiner)) {
            return createClassicTypeCheckerState(
                isErrorTypeEqualsToAnything = false,
                typeSystemContext = typeSystemContext,
                kotlinTypeRefiner = kotlinTypeRefiner,
            ).equalTypes(a.unwrap(), b.unwrap())
        }
    }

    // For example, expectedTypeConstructor may be the expected class kotlin.text.StringBuilder, while actualTypeConstructor
    // is java.lang.StringBuilder. For the purposes of type compatibility checking, we must consider these types equal here.
    // Note that the case of an "actual class" works as expected though, because the actual class by definition has the same FQ name
    // as the corresponding expected class, so their type constructors are equal as per AbstractClassTypeConstructor#equals
    private fun isExpectedClassAndActualTypeAlias(
        expectedTypeConstructor: TypeConstructor,
        actualTypeConstructor: TypeConstructor,
        platformModule: ModuleDescriptor
    ): Boolean {
        val expected = expectedTypeConstructor.declarationDescriptor
        val actual = actualTypeConstructor.declarationDescriptor
        return expected is ClassifierDescriptorWithTypeParameters &&
                expected.isExpect &&
                actual is ClassifierDescriptorWithTypeParameters &&
                findClassifiersFromModule(expected.classId, platformModule, moduleFilter = ALL_MODULES).any { classifier ->
                    // Note that it's fine to only check that this "actual typealias" expands to the expected class, without checking
                    // whether the type arguments in the expansion are in the correct order or have the correct variance, because we only
                    // allow simple cases like "actual typealias Foo<A, B> = FooImpl<A, B>", see DeclarationsChecker#checkActualTypeAlias
                    (classifier as? TypeAliasDescriptor)?.classDescriptor == actual
                }
    }

    fun findClassifiersFromModule(
        classId: ClassId?,
        module: ModuleDescriptor,
        moduleFilter: (ModuleDescriptor) -> Boolean
    ): Collection<ClassifierDescriptorWithTypeParameters> {
        if (classId == null) return emptyList()

        fun MemberScope.getAllClassifiers(name: Name): Collection<ClassifierDescriptorWithTypeParameters> =
            getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS) { it == name }
                .filterIsInstance<ClassifierDescriptorWithTypeParameters>()

        val segments = classId.relativeClassName.pathSegments()
        var classifiers = module.getPackage(classId.packageFqName).memberScope.getAllClassifiers(segments.first())
        classifiers = classifiers.applyFilter(moduleFilter)

        for (name in segments.subList(1, segments.size)) {
            classifiers = classifiers.mapNotNull { classifier ->
                (classifier as? ClassDescriptor)?.unsubstitutedInnerClassesScope?.getContributedClassifier(
                    name, NoLookupLocation.FOR_ALREADY_TRACKED
                ) as? ClassifierDescriptorWithTypeParameters
            }
        }

        return classifiers
    }

    override fun RegularClassSymbolMarker.isNotSamInterface(): Boolean {
        val descriptor = asDescriptor()
        return descriptor.isDefinitelyNotSamInterface || descriptor.defaultFunctionTypeForSamInterface == null
    }

    override fun CallableSymbolMarker.shouldSkipMatching(containingExpectClass: RegularClassSymbolMarker): Boolean {
        return safeAsDescriptor<CallableMemberDescriptor>()?.kind?.isReal == false
    }

    override val CallableSymbolMarker.hasStableParameterNames: Boolean
        get() = asDescriptor().hasStableParameterNames()

    override val DeclarationSymbolMarker.annotations: Iterable<AnnotationDelegate>
        get() = asDescriptor().annotations.map(::AnnotationDelegateImpl)

    override fun areArgumentsEqual(name: Name, annotation1: AnnotationDelegate, annotation2: AnnotationDelegate): Boolean {
        fun AnnotationDelegate.getDescriptor(): AnnotationDescriptor = (this as AnnotationDelegateImpl).annotationDescriptor

        return areAnnotationsEqual(annotation1.getDescriptor(), annotation2.getDescriptor())
    }

    private class AnnotationDelegateImpl(
        val annotationDescriptor: AnnotationDescriptor,
    ) : AnnotationDelegate {
        override val fqName: FqName?
            get() = annotationDescriptor.fqName

        override val allValueArgumentNames: Set<Name>
            get() = annotationDescriptor.allValueArguments.keys

        override val isRetentionSource: Boolean
            get() = annotationDescriptor.isSourceAnnotation
    }

    override val DeclarationSymbolMarker.hasSource: Boolean
        get() = DescriptorUtils.getContainingSourceFile(asDescriptor()) != SourceFile.NO_SOURCE_FILE
}
