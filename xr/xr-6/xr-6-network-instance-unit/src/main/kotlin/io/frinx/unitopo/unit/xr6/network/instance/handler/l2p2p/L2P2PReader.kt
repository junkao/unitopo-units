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

package io.frinx.unitopo.unit.xr6.network.instance.handler.l2p2p

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.builder.BasicCheck
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroupKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.P2pXconnects
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L2P2PReader(private val underlayAccess: UnderlayAccess) :
    CompositeListReader.Child<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder> {

    override fun getCheck(): Check {
        return BasicCheck.emptyCheck()
    }

    override fun getBuilder(p0: InstanceIdentifier<NetworkInstance>): NetworkInstanceBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    override fun getAllIds(
        instanceIdentifier: InstanceIdentifier<NetworkInstance>,
        readContext: ReadContext
    ): List<NetworkInstanceKey> {
        return getAllIds(underlayAccess)
    }

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<NetworkInstance>,
        networkInstanceBuilder: NetworkInstanceBuilder,
        readContext: ReadContext
    ) {
        val name = instanceIdentifier.firstKeyOf(NetworkInstance::class.java).name
        networkInstanceBuilder.name = name
    }

    companion object {

        val UNDERLAY_P2PXCONNECT_ID = InstanceIdentifier.create(L2vpn::class.java).child(Database::class.java)
                .child(XconnectGroups::class.java)
                .child(XconnectGroup::class.java, XconnectGroupKey(CiscoIosXrString("frinx")))
                .child(P2pXconnects::class.java)

        fun getAllIds(underlayAccess: UnderlayAccess): List<NetworkInstanceKey> {
            return underlayAccess.read(UNDERLAY_P2PXCONNECT_ID).get()?.orNull()
                    ?.let { parseIds(it) } ?: emptyList()
        }

        fun parseIds(underlayP2pXconnects: P2pXconnects): MutableList<NetworkInstanceKey> {
            return underlayP2pXconnects.p2pXconnect?.map { NetworkInstanceKey(it.name.value) }.orEmpty().toMutableList()
        }
    }
}