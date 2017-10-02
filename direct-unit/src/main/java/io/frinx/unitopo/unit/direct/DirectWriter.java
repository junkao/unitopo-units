/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.direct;

import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.translate.write.Writer;
import io.frinx.unitopo.registry.spi.UnderlayAccess;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * A writer that delegates all write operations to underlay mountpoint
 */
final class DirectWriter implements Writer<DataObject> {

    private final UnderlayAccess underlayAccess;
    private final InstanceIdentifier<? extends DataObject> id;

    DirectWriter(UnderlayAccess underlayAccess, InstanceIdentifier<? extends DataObject> id) {
        this.underlayAccess = underlayAccess;
        this.id = id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processModification(@Nonnull InstanceIdentifier<? extends DataObject> id,
                                    @Nullable DataObject dataBefore,
                                    @Nullable DataObject dataAfter,
                                    @Nonnull WriteContext ctx) throws WriteFailedException {
        if (dataBefore == null) {
            underlayAccess.put(((InstanceIdentifier<DataObject>) id), dataAfter);
        } else if (dataAfter == null) {
            underlayAccess.delete(id);
        } else {
            underlayAccess.merge(((InstanceIdentifier<DataObject>) id), dataAfter);
        }
    }

    @Override
    public boolean supportsDirectUpdate() {
        return true;
    }

    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        return ((InstanceIdentifier<DataObject>) id);
    }
}
