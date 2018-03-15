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
