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

package io.frinx.unitopo.unit.xr66.bgp.handler.neighbor

import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As
import io.frinx.unitopo.unit.xr66.bgp.UnderlayNeighbor
import io.frinx.unitopo.unit.xr66.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.DESCRIPTION
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.PASSWORD
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.REMOTEAS
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.SHUTDOWN
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.EncryptedPassword
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.EncryptedString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.PlainString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

const val PASSWORD_ENCRYPTED_PATTERN = "Encrypted[%s]"
const val PASSWORD_PLAIN_PREFIX = "!"

open class NeighborConfigReader(private val access: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun merge(parentBuilder: Builder<out DataObject>, config: Config) {
        (parentBuilder as NeighborBuilder).config = config
    }

    override fun getBuilder(p0: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val neighborKey = id.firstKeyOf(Neighbor::class.java)
        builder.neighborAddress = neighborKey.neighborAddress

        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val data = access.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java,
            InstanceKey(CiscoIosXrString(protKey.name))))
            .checkedGet()
            .orNull()

        parseNeighbor(data, vrfKey, neighborKey, builder)
    }

    companion object {

        fun parseNeighbor(
            underlayInstance: Instance?,
            vrfKey: NetworkInstanceKey,
            neighborKey: NeighborKey,
            builder: ConfigBuilder
        ) {
            val fourByteAs = BgpProtocolReader.getFirst4ByteAs(underlayInstance)

            if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                getNeighbor(fourByteAs, neighborKey)
                    ?.let { builder.fromUnderlay(it) }
            }
        }

        fun getVrfNeighbor(fourByteAs: FourByteAs?, vrfKey: NetworkInstanceKey, neighborKey: NeighborKey) =
            NeighborReader.getVrfNeighbors(fourByteAs, vrfKey)
                .find { it.neighborAddress == neighborKey.neighborAddress.toNoZone() }

        fun getNeighbor(fourByteAs: FourByteAs?, neighborKey: NeighborKey) =
            NeighborReader.getNeighbors(fourByteAs)
                .find { it.neighborAddress == neighborKey.neighborAddress.toNoZone() }
    }
}

private fun ConfigBuilder.fromUnderlay(neighbor: UnderlayNeighbor?) {
    neighbor?.let {
        neighborAddress = neighbor.neighborAddress.toIp()
        fromCommonUnderlay(neighbor)
    }
}

private fun <T> ConfigBuilder.fromCommonUnderlay(neighbor: T?)
    where T : REMOTEAS,
          T : SHUTDOWN,
          T : PASSWORD,
          T : DESCRIPTION {
    neighbor?.let {
        isEnabled = true
        it.isShutdown?.let {
            isEnabled = false
        }
        it.remoteAs?.let {
            peerAs = As.asFromDotNotation(neighbor.remoteAs.asXx.value, neighbor.remoteAs.asYy.value)
        }
        it.password?.let {
            authPassword = getEncryptedPassword(neighbor.password.password.value)
        }
        it.description?.let {
            description = neighbor.description
        }
    }
}

private fun getEncryptedPassword(password: String): EncryptedPassword {

    Preconditions.checkNotNull(password)
    return if (password.startsWith(PASSWORD_PLAIN_PREFIX))
            EncryptedPassword(PlainString(password.substring(PASSWORD_PLAIN_PREFIX.length)))
        else
            EncryptedPassword(EncryptedString(String.format(PASSWORD_ENCRYPTED_PATTERN, password)))
}

fun IpAddress.toNoZone(): IpAddressNoZone {
    return IpAddressNoZone(ipv4Address.value.toCharArray())
}