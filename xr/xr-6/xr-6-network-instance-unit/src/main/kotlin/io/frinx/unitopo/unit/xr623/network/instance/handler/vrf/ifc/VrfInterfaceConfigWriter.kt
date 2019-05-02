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

package io.frinx.unitopo.unit.xr623.network.instance.handler.vrf.ifc

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.ni.base.handler.vrf.ifc.AbstractVrfInterfaceConfigWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev161219.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev161219.InterfaceConfiguration1Builder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces._interface.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.regex.Pattern

class VrfInterfaceConfigWriter(underlayAccess: UnderlayAccess) :
    AbstractVrfInterfaceConfigWriter<InterfaceConfiguration>(underlayAccess) {

    override fun getUnderlayIid(vrfName: String, ifcName: String): InstanceIdentifier<InterfaceConfiguration> {
        return InstanceIdentifier.create(InterfaceConfigurations::class.java)
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(InterfaceActive("act"),
                InterfaceName(ifcName)))
    }

    override fun getData(vrfName: String, config: Config): InterfaceConfiguration {
        return InterfaceConfigurationBuilder()
            .setKey(InterfaceConfigurationKey(InterfaceActive("act"), InterfaceName(config.id)))
            .addAugmentation(InterfaceConfiguration1::class.java, InterfaceConfiguration1Builder()
                .setVrf(CiscoIosXrString(vrfName))
                .build())
            .build()
    }

    override fun deleteCurrentAttributes(iid: InstanceIdentifier<Config>, dataBefore: Config, wc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        if (vrfName == NetworInstance.DEFAULT_NETWORK_NAME || dataBefore.id == null) {
            return
        }
        val interfaceName = iid.firstKeyOf(Interface::class.java).id
        require(Util.isSubinterface(interfaceName)) {
            "Only vrf of sub-interface is supported to write."
        }
        underlayAccess.safeDelete(getUnderlayIid(vrfName, dataBefore.id), getData(vrfName, dataBefore))
    }

    override fun writeCurrentAttributes(iid: InstanceIdentifier<Config>, data: Config, wc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        val interfaceName = iid.firstKeyOf(Interface::class.java).id
        require(Util.isSubinterface(interfaceName)) {
            "Only vrf of sub-interface is supported to write."
        }
        val ifcName = findInterfaceName(data.id)
        val subifcIndex = findSubinterfaceName(data.id)

        val configurations = underlayAccess.read(InterfaceReader.IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()

        val subIfcKeys = getInterfaceKeys(configurations)
            .filter { Util.isSubinterface(it.name) }
            .filter { it.name.startsWith(ifcName) }
            .map { Util.getSubinterfaceKey(it.name) }
            .map { it.index }
        require(subIfcKeys.contains(subifcIndex)) {
            val subIfcName = ifcName + "." + data.id
            "Interface: $subIfcName does not exist, cannot add it to VRF".trimIndent()
        }
        underlayAccess.safePut(getUnderlayIid(vrfName, data.id), getData(vrfName, data))
    }

    companion object {
        private val SUBINTERFACE_NAME = Pattern.compile("(?<ifcId>.+)[.](?<subifcIndex>[0-9]+)")

        private fun findInterfaceName(id: String): String {
            val matcher = SUBINTERFACE_NAME.matcher(id)
            matcher.find()
            return matcher.group("ifcId")
        }

        private fun findSubinterfaceName(id: String): Long {
            val matcher = SUBINTERFACE_NAME.matcher(id)
            matcher.find()
            return matcher.group("subifcIndex").toLong()
        }

        private fun getInterfaceKeys(configurations: InterfaceConfigurations?): List<InterfaceKey> {
            return configurations
                ?.interfaceConfiguration.orEmpty()
                    .map {
                        InterfaceKey(it.interfaceName.value)
                    }
        }
    }
}