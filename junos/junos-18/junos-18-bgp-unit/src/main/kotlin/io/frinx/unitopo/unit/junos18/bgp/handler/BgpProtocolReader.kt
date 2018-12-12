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

package io.frinx.unitopo.unit.junos18.bgp.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.handlers.bgp.BgpReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.Configuration1
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.RoutingInstances
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

// NOTE:
// This class is a ListReader but inherits BgpReader.BgpConfigReader.
// If it inherits BgpListReader.BgpConfigListReader, getAllIds method doesn't work.
// Normally, BgpHandlers are inherited by handlers which handle a child of NE_NE_PR_PROTOCOL.
// In this class, getAllIds method handles NE_NE_PR_PROTOCOL(not a child), so we cannot use BgpListReader's method.
class BgpProtocolReader(private val access: UnderlayAccess) :
        BgpReader.BgpConfigReader<Protocol, ProtocolBuilder>,
        CompositeListReader.Child<Protocol, ProtocolKey, ProtocolBuilder> {

    override fun getBuilder(id: IID<Protocol>): ProtocolBuilder {
        throw UnsupportedOperationException("Should not be invoked")
    }

    override fun getAllIds(id: IID<Protocol>, context: ReadContext): List<ProtocolKey> {
        return when (access.read(JUNOS_VRFS_ID).checkedGet().isPresent) {
            true -> BGP_PROTOCOL_DEFAULT_KEYS
            else -> emptyList()
        }
    }

    override fun readCurrentAttributesForType(id: IID<Protocol>, builder: ProtocolBuilder, ctx: ReadContext) {
        val key = id.firstKeyOf(Protocol::class.java)
        builder.name = key.name
        builder.identifier = key.identifier
    }

    companion object {
        private val BGP_PROTOCOL_DEFAULT_KEYS = listOf(ProtocolKey(BgpReader.TYPE, BgpReader.NAME))

        private val JUNOS_CFG = IID.create(Configuration::class.java)!!
        private val JUNOS_RI_AUG = JUNOS_CFG.augmentation(Configuration1::class.java)!!
        val JUNOS_VRFS_ID = JUNOS_RI_AUG.child(RoutingInstances::class.java)!!
    }
}