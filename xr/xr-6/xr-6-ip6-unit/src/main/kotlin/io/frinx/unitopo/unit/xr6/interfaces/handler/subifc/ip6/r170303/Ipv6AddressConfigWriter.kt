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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r170303

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip.iarm.datatypes.rev150107.Ipv6armPrefixLength
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303.InterfaceConfiguration1Builder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.Ipv6Network
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.Ipv6NetworkBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.Addresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.addresses.LinkLocalAddress
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.addresses.LinkLocalAddressBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.addresses.RegularAddresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.addresses.RegularAddressesBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.addresses.regular.addresses.RegularAddress
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.addresses.regular.addresses.RegularAddressBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.addresses.regular.addresses.RegularAddressKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.address.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.regex.Pattern

open class Ipv6AddressConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

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
        if (isLinkLocal(dataAfter.ip.value)) {
            val (underlayId,
                    underlayCfg) = getLinkLocalData(id, dataAfter)
            underlayAccess.merge(underlayId, underlayCfg)
        } else {
            val (underlayId,
                    underlayCfg) = getAddressData(id, dataAfter)
            underlayAccess.merge(underlayId, underlayCfg)
        }
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        if (isLinkLocal(dataBefore.ip.value)) {
            val iid = getIfcId(id)
                .augmentation(InterfaceConfiguration1::class.java)
                .child(Ipv6Network::class.java)
                .child(Addresses::class.java)
                .child(LinkLocalAddress::class.java)
            underlayAccess.delete(iid)
        } else {
            val iid = getIfcId(id)
                .augmentation(InterfaceConfiguration1::class.java)
                .child(Ipv6Network::class.java)
                .child(Addresses::class.java)
                .child(RegularAddresses::class.java)
                .child(RegularAddress::class.java, RegularAddressKey(IpAddressNoZone(dataBefore.ip)))
            underlayAccess.delete(iid)
        }
    }

    private fun getIfcId(id: InstanceIdentifier<Config>): InstanceIdentifier<InterfaceConfiguration> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)

        return Ipv6AddressConfigWriter.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
    }

    private fun getLinkLocalData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<InterfaceConfiguration>, InterfaceConfiguration> {
        var ifcId = getIfcId(id)
        val ifcCfgBuilder = InterfaceConfigurationBuilder()
            .setKey(ifcId.firstKeyOf(InterfaceConfiguration::class.java))
        val ifcCfgBuilder1 = InterfaceConfiguration1Builder().apply {
            ipv6Network = Ipv6NetworkBuilder().apply {
                addresses = AddressesBuilder().apply {
                    linkLocalAddress = LinkLocalAddressBuilder()
                        .setAddress(IpAddressNoZone(dataAfter.ip)).build()
                }.build()
            }.build()
        }

        return Pair(ifcId, ifcCfgBuilder.addAugmentation(InterfaceConfiguration1::class.java, ifcCfgBuilder1
            .build()).build())
    }

    private fun getAddressData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<InterfaceConfiguration>, InterfaceConfiguration> {
        var ifcId = getIfcId(id)
        val ifcCfgBuilder = InterfaceConfigurationBuilder()
                .setKey(ifcId.firstKeyOf(InterfaceConfiguration::class.java))
        val ifcCfgBuilder1 = InterfaceConfiguration1Builder().apply {
            ipv6Network = Ipv6NetworkBuilder().apply {
                addresses = AddressesBuilder().apply {
                    val regularAddressList = mutableListOf<RegularAddress>()
                    regularAddressList.add(RegularAddressBuilder()
                        .setAddress(IpAddressNoZone(dataAfter.ip))
                        .setPrefixLength(Ipv6armPrefixLength(dataAfter.prefixLength.toLong())).build())
                    regularAddresses = RegularAddressesBuilder().setRegularAddress(regularAddressList).build()
                }.build()
            }.build()
        }

        return Pair(ifcId, ifcCfgBuilder.addAugmentation(InterfaceConfiguration1::class.java, ifcCfgBuilder1
            .build()).build())
    }

    companion object {
        val IFC_CFGS = InstanceIdentifier.create(InterfaceConfigurations::class.java)!!

        val LINK_LOCAL = Pattern.compile("[Ff][Ee][89AaBb].*")

        val ZERO_SUBINTERFACE_ID = 0L

        fun isLinkLocal(ip: String): Boolean = LINK_LOCAL.matcher(ip).find()
    }
}