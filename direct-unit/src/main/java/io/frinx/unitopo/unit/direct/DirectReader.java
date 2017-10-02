/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.direct;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.Reader;
import io.frinx.unitopo.registry.spi.UnderlayAccess;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * A reader that delegates all read operations to underlay mountpoint
 */
final class DirectReader implements Reader<DataObject, Builder<DataObject>> {

    private static final Builder<DataObject> FAKE_BUILDER = () -> () -> null;

    private UnderlayAccess underlayAccess;
    private InstanceIdentifier<? extends DataObject> id;

    DirectReader(UnderlayAccess underlayAccess, InstanceIdentifier<? extends DataObject> id) {
        this.underlayAccess = underlayAccess;
        this.id = id;
    }

    @Override
    public boolean isPresent(@Nonnull InstanceIdentifier instanceIdentifier,
                             @Nonnull DataObject dataObject,
                             @Nonnull ReadContext readContext) throws ReadFailedException {
        return read(instanceIdentifier, readContext).isPresent();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier instanceIdentifier,
                                      @Nonnull Builder builder,
                                      @Nonnull ReadContext readContext) throws ReadFailedException {
        // NOOP
    }

    @Nonnull
    @Override
    public Builder<DataObject> getBuilder(InstanceIdentifier instanceIdentifier) {
        return FAKE_BUILDER;
    }

    @Override
    public void merge(@Nonnull Builder builder, @Nonnull DataObject dataObject) {
        throw new UnsupportedOperationException("DIRECT READER");
    }

    @Nonnull
    @Override
    public Optional<? extends DataObject> read(@Nonnull InstanceIdentifier instanceIdentifier,
                                               @Nonnull ReadContext readContext) throws ReadFailedException {
        try {
            return underlayAccess.read(((InstanceIdentifier<? extends DataObject>) instanceIdentifier)).checkedGet();
        } catch (org.opendaylight.controller.md.sal.common.api.data.ReadFailedException e) {
            throw new ReadFailedException(instanceIdentifier, e);
        }
    }

    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        return ((InstanceIdentifier<DataObject>) id);
    }
}
