/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.interfaces.handler

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceWriter : ListWriterCustomizer<Interface, InterfaceKey> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Interface>, dataAfter: Interface, writeContext: WriteContext) {
        // NOOP
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Interface>, dataBefore: Interface, writeContext: WriteContext) {
        // NOOP
    }

}