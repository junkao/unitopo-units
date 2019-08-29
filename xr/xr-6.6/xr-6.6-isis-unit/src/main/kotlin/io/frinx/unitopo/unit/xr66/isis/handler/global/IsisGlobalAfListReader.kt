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
package io.frinx.unitopo.unit.xr66.isis.handler.global

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.isis.handler.IsisProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev170501.IsisAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev170501.IsisInstanceName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev170501.IsisSubAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.MULTICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.Af
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.AfBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.AfKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.af.ConfigBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.afs.Af as UlAf

open class IsisGlobalAfListReader(private val access: UnderlayAccess) :
    ConfigListReaderCustomizer<Af, AfKey, AfBuilder> {

    override fun getAllIds(id: InstanceIdentifier<Af>, readContext: ReadContext): List<AfKey> {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        if (vrfKey != NetworInstance.DEFAULT_NETWORK) {
            return emptyList()
        }

        val data = access.read(IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName(CiscoIosXrString(protKey.name)))))
            .checkedGet()
            .orNull()

        return data?.afs?.af?.map {
            it.toOpenConfig()
        }.orEmpty()
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Af>,
        builder: AfBuilder,
        readContext: ReadContext
    ) {
        val key = id.firstKeyOf(Af::class.java)
        builder.setKey(key)
        builder.afiName = key.afiName
        builder.safiName = key.safiName
        builder.config = ConfigBuilder().apply {
            afiName = key.afiName
            safiName = key.safiName
        }.build()
    }
}

fun UlAf.toOpenConfig(): AfKey {
    val afType = when (this.afName) {
        IsisAddressFamily.Ipv4 -> IPV4::class.java
        else -> IPV6::class.java
    }
    val safType = when (this.safName) {
        IsisSubAddressFamily.Unicast -> UNICAST::class.java
        else -> MULTICAST::class.java
    }
    return AfKey(afType, safType)
}