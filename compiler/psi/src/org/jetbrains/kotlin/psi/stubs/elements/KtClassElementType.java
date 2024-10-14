/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtEnumEntry;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub;
import org.jetbrains.kotlin.psi.stubs.StubUtils;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeBean;
import org.jetbrains.kotlin.psi.stubs.impl.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KtClassElementType extends KtStubElementType<KotlinClassStub, KtClass> {
    public KtClassElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtClass.class, KotlinClassStub.class);
    }

    @NotNull
    @Override
    public KtClass createPsi(@NotNull KotlinClassStub stub) {
        return !stub.isEnumEntry() ? new KtClass(stub) : new KtEnumEntry(stub);
    }

    @NotNull
    @Override
    public KtClass createPsiFromAst(@NotNull ASTNode node) {
        return node.getElementType() != KtStubElementTypes.ENUM_ENTRY ? new KtClass(node) : new KtEnumEntry(node);
    }

    @NotNull
    @Override
    public KotlinClassStub createStub(@NotNull KtClass psi, StubElement parentStub) {
        FqName fqName = KtPsiUtilKt.safeFqNameForLazyResolve(psi);
        boolean isEnumEntry = psi instanceof KtEnumEntry;
        List<String> superNames = KtPsiUtilKt.getSuperNames(psi);
        ClassId classId = StubUtils.createNestedClassId(parentStub, psi);
        return new KotlinClassStubImpl(
                getStubType(isEnumEntry), (StubElement<?>) parentStub,
                StringRef.fromString(fqName != null ? fqName.asString() : null), classId,
                StringRef.fromString(psi.getName()),
                Utils.INSTANCE.wrapStrings(superNames),
                psi.isInterface(), isEnumEntry, psi.isLocal(), psi.isTopLevel(),
                null
        );
    }

    @Override
    public void serialize(@NotNull KotlinClassStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName == null ? null : fqName.asString());

        StubUtils.serializeClassId(dataStream, stub.getClassId());

        dataStream.writeBoolean(stub.isInterface());
        dataStream.writeBoolean(stub.isEnumEntry());
        dataStream.writeBoolean(stub.isLocal());
        dataStream.writeBoolean(stub.isTopLevel());

        List<String> superNames = stub.getSuperNames();
        dataStream.writeVarInt(superNames.size());
        for (String name : superNames) {
            dataStream.writeName(name);
        }

        if (stub instanceof KotlinClassStubImpl) {
            KotlinClassStubImpl stubImpl = (KotlinClassStubImpl) stub;
            KotlinValueClassRepresentation representation = stubImpl.getValueClassRepresentation();
            if (representation == null) {
                dataStream.writeVarInt(-1);
            }else {
                List<KotlinTypeBean> underlyingTypes = representation.getUnderlyingTypes();
                dataStream.writeVarInt(underlyingTypes.size());
                for (KotlinTypeBean typeBean : underlyingTypes) {
                    TypeBeanSerializationKt.serializeTypeBean(dataStream, typeBean);
                }

                dataStream.writeBoolean(representation.isInline());
            }
        }
    }

    @NotNull
    @Override
    public KotlinClassStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        StringRef qualifiedName = dataStream.readName();

        ClassId classId = StubUtils.deserializeClassId(dataStream);

        boolean isTrait = dataStream.readBoolean();
        boolean isEnumEntry = dataStream.readBoolean();
        boolean isLocal = dataStream.readBoolean();
        boolean isTopLevel = dataStream.readBoolean();

        int superCount = dataStream.readVarInt();
        StringRef[] superNames = StringRef.createArray(superCount);
        for (int i = 0; i < superCount; i++) {
            superNames[i] = dataStream.readName();
        }

        int typeBeanCount = dataStream.readVarInt();
        KotlinValueClassRepresentation representation = null;
        if (typeBeanCount >= 0) {
            List<KotlinTypeBean> typeBeans;
            if (typeBeanCount == 0) {
                typeBeans = Collections.emptyList();
            } else if (typeBeanCount == 1) {
                KotlinTypeBean value = TypeBeanSerializationKt.deserializeTypeBean(dataStream);
                typeBeans = Collections.singletonList(value);
            } else {
                typeBeans = new ArrayList<>(typeBeanCount);
                for (int i = 0; i < typeBeanCount; i++) {
                    typeBeans.add(TypeBeanSerializationKt.deserializeTypeBean(dataStream));
                }
            }

            boolean isInline = dataStream.readBoolean();
            representation = new KotlinValueClassRepresentation(isInline, typeBeans);
        }

        return new KotlinClassStubImpl(
                getStubType(isEnumEntry), (StubElement<?>) parentStub, qualifiedName,classId, name, superNames,
                isTrait, isEnumEntry, isLocal, isTopLevel,
                representation
        );
    }

    @Override
    public void indexStub(@NotNull KotlinClassStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexClass(stub, sink);
    }

    public static KtClassElementType getStubType(boolean isEnumEntry) {
        return isEnumEntry ? KtStubElementTypes.ENUM_ENTRY : KtStubElementTypes.CLASS;
    }
}
