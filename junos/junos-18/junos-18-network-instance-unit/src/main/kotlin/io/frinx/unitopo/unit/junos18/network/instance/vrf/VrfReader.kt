/*
 * Copyright © 2018 Frinx and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.frinx.unitopo.unit.junos18.network.instance.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.RoutingInstances
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.Configuration1 as RoutingInstanceConfigurationAug

class VrfReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder>,
    CompositeListReader.Child<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder> {

    override fun readCurrentAttributes(
        id: InstanceIdentifier<NetworkInstance>,
        builder: NetworkInstanceBuilder,
        ctx: ReadContext
    ) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        builder.name = vrfName
    }

    override fun getBuilder(p0: InstanceIdentifier<NetworkInstance>): NetworkInstanceBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    override fun getAllIds(id: InstanceIdentifier<NetworkInstance>, context: ReadContext): List<NetworkInstanceKey> {
        return getAllIds(underlayAccess)
    }

    companion object {
        private val JUNOS_CFG = InstanceIdentifier.create(Configuration::class.java)!!
        private val JUNOS_RI_AUG = JUNOS_CFG.augmentation(RoutingInstanceConfigurationAug::class.java)!!
        val JUNOS_VRFS_ID = JUNOS_RI_AUG.child(RoutingInstances::class.java)!!

        fun getAllIds(underlayAccess: UnderlayAccess): List<NetworkInstanceKey> {
            return underlayAccess.read(JUNOS_VRFS_ID)
                .checkedGet()
                .orNull()
                ?.instance.orEmpty()
                .map { NetworkInstanceKey(it.name) }
                .toList()
        }
    }
}