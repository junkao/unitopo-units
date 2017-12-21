/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.l2p2p

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.cli.registry.common.CompositeListReader
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

class L2P2PReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder>,
        CompositeListReader.Child<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder> {

    override fun getAllIds(instanceIdentifier: InstanceIdentifier<NetworkInstance>,
                           readContext: ReadContext): List<NetworkInstanceKey> {
        return getAllIds(underlayAccess)
    }

    override fun readCurrentAttributes(instanceIdentifier: InstanceIdentifier<NetworkInstance>,
                                       networkInstanceBuilder: NetworkInstanceBuilder,
                                       readContext: ReadContext) {
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
