/*
 * Copyright © 2019 Frinx and others.
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

package io.frinx.unitopo.unit.xr623.network.instance.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev161219.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstancesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class VrfReader(private val underlayAccess: UnderlayAccess) :
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

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<NetworkInstance>) {
        (builder as NetworkInstancesBuilder).networkInstance = readData
    }

    override fun getBuilder(id: InstanceIdentifier<NetworkInstance>): NetworkInstanceBuilder = NetworkInstanceBuilder()

    override fun getAllIds(id: InstanceIdentifier<NetworkInstance>, context: ReadContext): List<NetworkInstanceKey> {
        val parseIds = parseIds(underlayAccess)
        parseIds.add(NetworInstance.DEFAULT_NETWORK)
        return parseIds
    }

    companion object {
        private fun parseIds(underlayAccess: UnderlayAccess): MutableList<NetworkInstanceKey> {
            // add vrf names defined in interface
            val list = underlayAccess.read(InterfaceReader.IFC_CFGS)
                .checkedGet()
                .orNull()
                ?.let {
                    it.interfaceConfiguration?.filter {
                        InterfaceReader.isSubinterface(it.interfaceName.value)
                    }?.map {
                        it.getAugmentation(InterfaceConfiguration1::class.java)?.vrf
                    }?.filterNotNull()?.map {
                        it.value
                    }
                }.orEmpty()
                .toMutableList()

            // TODO: add vrf names defined in ospf
//            underlayAccess.read(OspfProtocolReader.UNDERLAY_OSPF)
//                .checkedGet()
//                .orNull()
//                ?.let {
//                    it.process?.map {
//                        it?.vrfs?.vrf?.map {
//                            list.add(it.vrfName.value)
//                        }
//                    }
//                }

            // get a distinct list
            return list.distinct().map {
                NetworkInstanceKey(it)
            }.toMutableList()
        }
    }
}