/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.isis.handler.global

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.isis.handler.IsisProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.IsisConfigurableLevels
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.IsisRedistProto
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.af.content.redistributions.redistribution.OspfOrOspfv3OrIsisOrApplication
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.af.content.redistributions.redistribution.OspfOrOspfv3OrIsisOrApplicationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.af.content.redistributions.redistribution.OspfOrOspfv3OrIsisOrApplicationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisInstanceName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.Redistribution
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.RedistributionKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.LevelType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.Af
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.Afs as UlAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.Af as UlAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.AfData as UlAfData
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.af.content.Redistributions as UlRedistributions
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.af.content.redistributions.Redistribution as UlRedistribution
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.af.content.redistributions.RedistributionKey as UlRedistributionKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class IsisRedistributionListWriter(private val access: UnderlayAccess)
    : ListWriterCustomizer<Redistribution, RedistributionKey> {

    override fun writeCurrentAttributes(id: IID<Redistribution>, data: Redistribution, writeContext: WriteContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        require(ChecksMap.PathCheck.Protocol.ISIS.canProcess(id, writeContext, false))
        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "IS-IS configuration should be set in default network: $vrfName"
        }
        val (underlayId, underlayData) = getData(id, data)
        access.safePut(underlayId, underlayData)
    }

    override fun deleteCurrentAttributes(
        id: IID<Redistribution>,
        dataBefore: Redistribution,
        writeContext: WriteContext
    ) {
        val (underlayId, _) = getData(id, dataBefore)
        val underlayBefore = access.read(underlayId).checkedGet().orNull()!!
        access.safeDelete(underlayId, underlayBefore)
    }

    override fun updateCurrentAttributes(
        id: IID<Redistribution>,
        dataBefore: Redistribution,
        dataAfter: Redistribution,
        writeContext: WriteContext
    ) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        require(ChecksMap.PathCheck.Protocol.ISIS.canProcess(id, writeContext, false))
        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "IS-IS configuration should be set in default network: $vrfName"
        }
        val (underlayId, underlayData) = getData(id, dataAfter)
        val underlayBefore = access.read(underlayId).checkedGet().orNull()!!
        access.safeMerge(underlayId, underlayBefore, underlayId, underlayData)
    }

    private fun getData(
        id: IID<Redistribution>,
        data: Redistribution
    ): Pair<IID<OspfOrOspfv3OrIsisOrApplication>, OspfOrOspfv3OrIsisOrApplication>
    {
        val protKey = id.firstKeyOf(Protocol::class.java)
        val afKey = id.firstKeyOf(Af::class.java)
        val ulAfKey = afKey.toUnderlay()
        val underlayId = IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName(protKey.name)))
            .child(UlAfs::class.java)
            .child(UlAf::class.java, ulAfKey)
            .child(UlAfData::class.java)
            .child(UlRedistributions::class.java)
            .child(UlRedistribution::class.java, UlRedistributionKey(IsisRedistProto.Isis))
            .child(OspfOrOspfv3OrIsisOrApplication::class.java,
                OspfOrOspfv3OrIsisOrApplicationKey(CiscoIosXrString(data.instance)))
        val underlayData = OspfOrOspfv3OrIsisOrApplicationBuilder().apply {
            levels = data.config?.level?.toUnderlay()
            routePolicyName = (CiscoIosXrString(data.config?.routePolicy))
            instanceName = IsisInstanceName(data.instance)
        }.build()
        return Pair(underlayId, underlayData)
    }
}

fun LevelType.toUnderlay(): IsisConfigurableLevels {
    return when (this) {
        LevelType.LEVEL1 -> IsisConfigurableLevels.Level1
        LevelType.LEVEL2 -> IsisConfigurableLevels.Level2
        else -> IsisConfigurableLevels.Level1And2
    }
}