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

package io.frinx.unitopo.unit.xr6.network.instance.vrf.ifc

import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.InterfaceConfiguration1Builder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces._interface.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class VrfInterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {
    override fun deleteCurrentAttributes(iid: IID<Config>, dataBefore: Config, wc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name

        if (vrfName == NetworInstance.DEFAULT_NETWORK_NAME || dataBefore.`id` == null) {
            return
        }

        val builder = underlayAccess.read(getInterfaceConfigurationIdentifier(dataBefore.`id`))
                .checkedGet()
                .or(InterfaceConfigurationBuilder().build())
                .let { InterfaceConfigurationBuilder(it) }
        builder.removeAugmentation(InterfaceConfiguration1::class.java)
        builder.interfaceModeNonPhysical = null

        underlayAccess.put(getInterfaceConfigurationIdentifier(dataBefore.`id`), builder.build())
    }

    override fun writeCurrentAttributes(iid: IID<Config>, data: Config, wc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        if (vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            return
        }

        val ifcExists = wc.readAfter(IIDs.INTERFACES.child(org.opendaylight.yang.gen.v1.http
            .frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface::class.java,
                org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top
                    .interfaces.InterfaceKey(data.id)))
                .isPresent
        Preconditions.checkArgument(ifcExists, "Interface: %s does not exist, cannot add it to VRF", data.id)

        val writeIid = getInterfaceConfigurationIdentifier(data.id)
        val ifConfig = InterfaceConfigurationBuilder()
                .setKey(InterfaceConfigurationKey(InterfaceActive("act"), InterfaceName(data.id)))
                .addAugmentation(InterfaceConfiguration1::class.java, InterfaceConfiguration1Builder()
                        .setVrf(CiscoIosXrString(vrfName))
                        .build())
                .build()

        underlayAccess.merge(writeIid, ifConfig)
    }

    companion object {
        public fun getInterfaceConfigurationIdentifier(ifaceName: String): IID<InterfaceConfiguration> {
            return IID.create(InterfaceConfigurations::class.java)
                    .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(InterfaceActive("act"),
                        InterfaceName(ifaceName)))
        }
    }
}