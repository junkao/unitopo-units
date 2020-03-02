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
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.isis.handler.IsisProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.Afs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.af.AfData
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisInstanceName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisSubAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev190405.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.Redistribution
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.RedistributionBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.isis.redistribution.ext.config.redistributions.RedistributionKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.Af
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.AfKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.Af as UlAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.instance.afs.AfKey as UlAfKey

open class IsisRedistributionListReader(private val access: UnderlayAccess) :
    ConfigListReaderCustomizer<Redistribution, RedistributionKey, RedistributionBuilder> {

    override fun getAllIds(id: InstanceIdentifier<Redistribution>, readContext: ReadContext): List<RedistributionKey> {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val afKey = id.firstKeyOf(Af::class.java)
        if (vrfKey != NetworInstance.DEFAULT_NETWORK) {
            return emptyList()
        }
        val data = access.read(IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName(CiscoIosXrString(protKey.name))))
            .child(Afs::class.java)
            .child(UlAf::class.java, afKey.toUnderlay())
            .child(AfData::class.java)
        ).checkedGet().orNull()
        var rtn = mutableListOf<RedistributionKey>()
        data?.redistributions?.redistribution?.map {
            it.ospfOrOspfv3OrIsisOrApplication?.map {
                rtn.add(RedistributionKey(it.instanceName.value, "isis"))
            }
        }
        return rtn
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Redistribution>,
        builder: RedistributionBuilder,
        readContext: ReadContext
    ) {
        val key = id.firstKeyOf<Redistribution, RedistributionKey>(Redistribution::class.java)
        builder.setInstance(key.instance)
        builder.setProtocol(key.protocol)
    }
}

fun AfKey.toUnderlay(): UlAfKey {
    val afType = when (afiName) {
        IPV4::class.java -> IsisAddressFamily.Ipv4
        else -> IsisAddressFamily.Ipv6
    }
    val safType = when (safiName) {
        UNICAST::class.java -> IsisSubAddressFamily.Unicast
        else -> IsisSubAddressFamily.Multicast
    }
    return UlAfKey(afType, safType)
}