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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r170303

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.Ipv6Network
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.Addresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.addresses.AutoConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303._interface.configurations._interface.configuration.ipv6.network.addresses.AutoConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

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
        val (underlayId, underlayCfg) = getConfigData(id, dataAfter)
        underlayAccess.merge(underlayId, underlayCfg)
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        if (dataBefore.isEnabled) underlayAccess.delete(getId(id))
    }

    private fun getId(id: InstanceIdentifier<Config>): InstanceIdentifier<AutoConfiguration> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name).value
        val ifcIndex = id.firstKeyOf(Subinterface::class.java).index
        val subIfcName = when (ifcIndex) {
            ZERO_SUBINTERFACE_ID -> ifcName
            else -> getSubIfcName(ifcName, ifcIndex)
        }
        return Ipv6ConfigWriter.IFC_CFGS
            .child(InterfaceConfiguration::class.java,
                    InterfaceConfigurationKey(interfaceActive, InterfaceName(subIfcName)))
            .augmentation(InterfaceConfiguration1::class.java)
            .child(Ipv6Network::class.java)
            .child(Addresses::class.java)
            .child(AutoConfiguration::class.java)
    }

    private fun getConfigData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<AutoConfiguration>, AutoConfiguration> {
        val builder = AutoConfigurationBuilder()
        if (!dataAfter.isEnabled) {
            builder.setEnable(null)
        } else {
            builder.setEnable(true)
        }
        return Pair(getId(id), builder.build())
    }

    companion object {
        val IFC_CFGS = InstanceIdentifier.create(InterfaceConfigurations::class.java)!!

        val ZERO_SUBINTERFACE_ID = 0L

        fun getSubIfcName(ifcName: String, subifcIdx: Long) = ifcName + "." + subifcIdx
    }
}