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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730

import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip.iarm.datatypes.rev150107.Ipv6armPrefixLength
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730._interface.configurations._interface.configuration.Ipv6Network
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730._interface.configurations._interface.configuration.ipv6.network.Addresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730._interface.configurations._interface.configuration.ipv6.network.addresses.LinkLocalAddress
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730._interface.configurations._interface.configuration.ipv6.network.addresses.LinkLocalAddressBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730._interface.configurations._interface.configuration.ipv6.network.addresses.RegularAddresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730._interface.configurations._interface.configuration.ipv6.network.addresses.regular.addresses.RegularAddress
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730._interface.configurations._interface.configuration.ipv6.network.addresses.regular.addresses.RegularAddressBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev150730._interface.configurations._interface.configuration.ipv6.network.addresses.regular.addresses.RegularAddressKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.address.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.regex.Pattern

open class Ipv6ConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val subId = id.firstKeyOf(Subinterface::class.java).index
        if (subId != ZERO_SUBINTERFACE_ID) {
            throw WriteFailedException.CreateFailedException(id, dataAfter,
                    IllegalArgumentException("Unable to manage IP for subinterface: " + subId))
        }
        try {
            if (isLinkLocal(dataAfter.ip.value)) {
                val (underlayId, underlayCfg) = getLinkLocalData(id, dataAfter)
                underlayAccess.merge(underlayId, underlayCfg)
            } else {
                val (underlayId, underlayCfg) = getAddressData(id, dataAfter)
                underlayAccess.merge(underlayId, underlayCfg)
            }
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.read.ReadFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        try {
            if (isLinkLocal(dataBefore.ip.value)) {
                underlayAccess.delete(getId(id)
                    .child(LinkLocalAddress::class.java))
            } else {
                underlayAccess.delete(getAddressId(id, dataBefore.ip))
            }
        } catch (e: Exception) {
            throw ReadFailedException(id, e)
        }
    }

    private fun getId(id: InstanceIdentifier<Config>): InstanceIdentifier<Addresses> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)

        return Ipv6ConfigWriter.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
            .augmentation(InterfaceConfiguration1::class.java)
            .child(Ipv6Network::class.java)
            .child(Addresses::class.java)
    }

    private fun getLinkLocalData(id: InstanceIdentifier<Config>, dataAfter: Config):
        Pair<InstanceIdentifier<LinkLocalAddress>, LinkLocalAddress> {
        return Pair(getId(id).child(LinkLocalAddress::class.java),
            LinkLocalAddressBuilder().setZone("0").setAddress(IpAddressNoZone(dataAfter.ip)).build())
    }

    private fun getAddressData(id: InstanceIdentifier<Config>, dataAfter: Config):
        Pair<InstanceIdentifier<RegularAddress>, RegularAddress> {
        val regBuilder = RegularAddressBuilder()
            .setAddress(IpAddressNoZone(dataAfter.ip))
            .setPrefixLength(Ipv6armPrefixLength(dataAfter.prefixLength.toLong()))
        return Pair(getAddressId(id, dataAfter.ip), regBuilder.build())
    }

    private fun getAddressId(id: InstanceIdentifier<Config>, ip: Ipv6AddressNoZone):
        InstanceIdentifier<RegularAddress> {
        return getId(id).child(RegularAddresses::class.java)
            .child(RegularAddress::class.java, RegularAddressKey(IpAddressNoZone(ip)))
    }

    companion object {
        val IFC_CFGS = InstanceIdentifier.create(InterfaceConfigurations::class.java)!!

        val LINK_LOCAL = Pattern.compile("[Ff][Ee][89AaBb].*")

        val ZERO_SUBINTERFACE_ID = 0L

        fun isLinkLocal(ip: String): Boolean = LINK_LOCAL.matcher(ip).find()
    }
}