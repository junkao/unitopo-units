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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.apache.commons.net.util.SubnetUtils
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev150730.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev150730._interface.configurations._interface.configuration.Ipv4Network
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev150730._interface.configurations._interface.configuration.ipv4.network.Addresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev150730._interface.configurations._interface.configuration.ipv4.network.addresses.Primary
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev150730._interface.configurations._interface.configuration.ipv4.network.addresses.PrimaryBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class Ipv4AddressConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        try {
            val (underlayId, underlayCfg) = getData(id, dataAfter)
            underlayAccess.merge(underlayId, underlayCfg)
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
            underlayAccess.delete(getId(id))
        } catch (e: Exception) {
            throw ReadFailedException(id, e)
        }
    }

    private fun getId(id: InstanceIdentifier<Config>): InstanceIdentifier<Primary> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)

        return InstanceIdentifier.create(InterfaceConfigurations::class.java)
                .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
                .augmentation(InterfaceConfiguration1::class.java)
                .child(Ipv4Network::class.java)
                .child(Addresses::class.java)
                .child(Primary::class.java)
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<Primary>, Primary> {
        val netmask = SubnetUtils(dataAfter.ip.value + "/" + dataAfter.prefixLength).info.netmask
        val builder = PrimaryBuilder()
                .setAddress(dataAfter.ip)
                .setNetmask(Ipv4AddressNoZone(netmask))
        return Pair(getId(id), builder.build())
    }
}