/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class SubinterfaceWriter : ListWriterCustomizer<Subinterface, SubinterfaceKey> {

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Subinterface>, dataBefore: Subinterface, writeContext: WriteContext) {
        // NOOP
    }

    override fun writeCurrentAttributes(id: InstanceIdentifier<Subinterface>, dataAfter: Subinterface, writeContext: WriteContext) {
        // NOOP
    }


}
