/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.common.def

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.cli.registry.common.CompositeReader
import io.frinx.openconfig.network.instance.NetworInstance.DEFAULT_NETWORK
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.DEFAULTINSTANCE
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class DefaultConfigReader : ConfigReaderCustomizer<Config, ConfigBuilder>, CompositeReader.Child<Config, ConfigBuilder> {

    override fun getBuilder(p0: InstanceIdentifier<Config>): ConfigBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(instanceIdentifier: InstanceIdentifier<Config>,
                                       configBuilder: ConfigBuilder,
                                       readContext: ReadContext) {
        if (isDefault(instanceIdentifier)) {
            configBuilder.name = instanceIdentifier.firstKeyOf<NetworkInstance, NetworkInstanceKey>(NetworkInstance::class.java).getName()
            configBuilder.type = DEFAULTINSTANCE::class.java
        }
    }

    companion object {

        internal fun isDefault(instanceIdentifier: InstanceIdentifier<*>): Boolean {
            return instanceIdentifier.firstKeyOf<NetworkInstance, NetworkInstanceKey>(NetworkInstance::class.java) == DEFAULT_NETWORK
        }
    }

}
