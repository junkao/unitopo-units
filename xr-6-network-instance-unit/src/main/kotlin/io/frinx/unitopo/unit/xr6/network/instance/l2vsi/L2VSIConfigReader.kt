/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.l2vsi

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.cli.registry.common.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L2VSI
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L2VSIConfigReader(private val underlayAccess: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder>,
        CompositeReader.Child<Config, ConfigBuilder> {

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(instanceIdentifier: InstanceIdentifier<Config>,
                                       configBuilder: ConfigBuilder,
                                       readContext: ReadContext) {
        if (isVrf(instanceIdentifier)) {
            configBuilder.name = instanceIdentifier.firstKeyOf<NetworkInstance, NetworkInstanceKey>(NetworkInstance::class.java).name
            configBuilder.type = L2VSI::class.java

            // TODO set other attributes i.e. description
        }
    }

    @Throws(ReadFailedException::class)
    private fun isVrf(id: InstanceIdentifier<Config>): Boolean {
        return L2VSIReader.getAllIds(underlayAccess).contains(id.firstKeyOf<NetworkInstance, NetworkInstanceKey>(NetworkInstance::class.java))
    }
}
