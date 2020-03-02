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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.isis.handler.IsisProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.IsisConfigurableLevels
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.IsisRedistProto
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.Afs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.Af
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.AfData
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.af.content.Redistributions
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisInstanceName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.Redistribution
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.RedistributionKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.redistribution.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.redistribution.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.LevelType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.af.content.redistributions.Redistribution as UlRedistribution
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.af.content.redistributions.RedistributionKey as UlRedistributionKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.Af as ocAf

open class IsisRedistributionConfigReader(private val access: UnderlayAccess)
    : ConfigReaderCustomizer<Config, ConfigBuilder> {
    override fun readCurrentAttributes(
        id: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        ctx: ReadContext
    ) {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val afKey = id.firstKeyOf(ocAf::class.java)
        val redKey = id.firstKeyOf<Redistribution, RedistributionKey>(Redistribution::class.java)
        val data = access.read(IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName(CiscoIosXrString(protKey.name))))
            .child(Afs::class.java)
            .child(Af::class.java, afKey.toUnderlay())
            .child(AfData::class.java)
            .child(Redistributions::class.java)
            .child(UlRedistribution::class.java, UlRedistributionKey(IsisRedistProto.Isis))
        ).checkedGet().orNull()
        builder.setInstance(redKey.instance)
            .setProtocol(redKey.protocol)
        data?.ospfOrOspfv3OrIsisOrApplication?.filter {
            it.instanceName.value == redKey.instance
        }?.get(0)?.apply {
            builder.setLevel(levels?.toOpenConfig())
            builder.setRoutePolicy(routePolicyName?.value)
        }
    }
}

fun IsisConfigurableLevels.toOpenConfig(): LevelType {
    return when (this) {
        IsisConfigurableLevels.Level1 -> LevelType.LEVEL1
        IsisConfigurableLevels.Level2 -> LevelType.LEVEL2
        else -> LevelType.LEVEL12
    }
}