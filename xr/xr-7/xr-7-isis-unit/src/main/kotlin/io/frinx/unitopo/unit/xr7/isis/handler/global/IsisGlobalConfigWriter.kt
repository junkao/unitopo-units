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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.isis.handler.IsisProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.MaxLinkMetrics
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.MaxLinkMetricsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.max.link.metrics.MaxLinkMetricBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.max.link.metrics.MaxLinkMetricKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisInstanceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisGlobalConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisInternalLevel
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.global.base.Config
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisInternalLevel as UnderlayIILevel
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class IsisGlobalConfigWriter(private val access: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: IID<Config>, config: Config, wtx: WriteContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        require(ChecksMap.PathCheck.Protocol.ISIS.canProcess(id, wtx, false))
        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "IS-IS configuration should be set in default network: $vrfName"
        }
        val (underlayId, underlayData) = getData(id, config)
        access.safePut(underlayId, underlayData)
    }

    override fun updateCurrentAttributes(id: IID<Config>, dataBefore: Config, dataAfter: Config, wtx: WriteContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        require(ChecksMap.PathCheck.Protocol.ISIS.canProcess(id, wtx, false))
        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "IS-IS configuration should be set in default network: $vrfName"
        }
        val (underlayId, underlayData) = getData(id, dataAfter)
        val underlayBefore = access.read(underlayId).checkedGet().orNull()!!
        access.safeMerge(underlayId, underlayBefore, underlayId, underlayData)
    }

    override fun deleteCurrentAttributes(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        require(ChecksMap.PathCheck.Protocol.ISIS.canProcess(id, wtx, false))
        val (underlayId, _) = getData(id, dataBefore)
        val underlayBefore = access.read(underlayId).checkedGet().orNull()!!
        access.safeDelete(underlayId, underlayBefore)
    }

    private fun getData(id: IID<Config>, config: Config): Pair<IID<MaxLinkMetrics>, MaxLinkMetrics> {
        val protKey = id.firstKeyOf(Protocol::class.java)
        val underlayId = IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName(protKey.name)))
            .child(MaxLinkMetrics::class.java)
        val underlayData = MaxLinkMetricsBuilder().setMaxLinkMetric(
            config.getAugmentation(IsisGlobalConfAug::class.java)?.maxLinkMetric?.map {
                MaxLinkMetricBuilder().setKey(MaxLinkMetricKey(it.toUnderLay())).build()
            }?.toList()
        ).build()
        return Pair(underlayId, underlayData)
    }
}

fun IsisInternalLevel.toUnderLay(): UnderlayIILevel {
    return when (this) {
        IsisInternalLevel.LEVEL1 -> UnderlayIILevel.Level1
        IsisInternalLevel.LEVEL2 -> UnderlayIILevel.Level2
        IsisInternalLevel.NOTSET -> UnderlayIILevel.NotSet
    }
}