/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.cli.registry.common.CompositeListReader
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.Vrfs
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstancesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class VrfReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder>,
        CompositeListReader.Child<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder> {

    override fun readCurrentAttributes(id: InstanceIdentifier<NetworkInstance>, builder: NetworkInstanceBuilder, ctx: ReadContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        builder.name = vrfName
    }

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<NetworkInstance>) {
        (builder as NetworkInstancesBuilder).networkInstance = readData
    }

    override fun getBuilder(id: InstanceIdentifier<NetworkInstance>): NetworkInstanceBuilder = NetworkInstanceBuilder()

    override fun getAllIds(id: InstanceIdentifier<NetworkInstance>, context: ReadContext): List<NetworkInstanceKey> {
        return getAllIds(this.underlayAccess)
    }

    companion object {
        val VRFS_ID = InstanceIdentifier.create(Vrfs::class.java)

        fun getAllIds(underlayAccess: UnderlayAccess): List<NetworkInstanceKey> {

            val parseIds = parseIds(underlayAccess)
            parseIds.add(NetworInstance.DEFAULT_NETWORK)
            return parseIds
        }

        private fun parseIds(underlayAccess: UnderlayAccess): MutableList<NetworkInstanceKey> {
            return underlayAccess.read(VRFS_ID)
                    .checkedGet()
                    .orNull()
                    ?.let {
                        it.vrf?.map { NetworkInstanceKey(it.vrfName.value) }
                                ?.toCollection(mutableListOf())
                    }.orEmpty()
                    .toMutableList()
        }
    }
}
