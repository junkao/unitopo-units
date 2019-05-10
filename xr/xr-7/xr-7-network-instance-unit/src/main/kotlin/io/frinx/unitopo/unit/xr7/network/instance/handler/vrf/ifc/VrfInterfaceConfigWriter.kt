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

package io.frinx.unitopo.unit.xr7.network.instance.handler.vrf.ifc

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.ni.base.handler.vrf.ifc.AbstractVrfInterfaceConfigWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev180615.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev180615.InterfaceConfiguration1Builder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces._interface.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class VrfInterfaceConfigWriter(underlayAccess: UnderlayAccess) :
    AbstractVrfInterfaceConfigWriter<InterfaceConfiguration>(underlayAccess) {

    override fun getData(vrfName: String, config: Config): InterfaceConfiguration =
        InterfaceConfigurationBuilder()
            .setKey(InterfaceConfigurationKey(InterfaceActive("act"), InterfaceName(config.id)))
            .addAugmentation(InterfaceConfiguration1::class.java, InterfaceConfiguration1Builder()
                .setVrf(CiscoIosXrString(vrfName))
                .build())
            .build()

    override fun getUnderlayIid(vrfName: String, ifcName: String): InstanceIdentifier<InterfaceConfiguration> {
        return InstanceIdentifier.create(InterfaceConfigurations::class.java)
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(InterfaceActive("act"),
                InterfaceName(ifcName)))
    }

    override fun deleteCurrentAttributes(iid: InstanceIdentifier<Config>, dataBefore: Config, wc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        if (vrfName == NetworInstance.DEFAULT_NETWORK_NAME || dataBefore.id == null) {
            return
        }
        val interfaceName = iid.firstKeyOf(Interface::class.java).id
        require(InterfaceReader.isSubinterface(interfaceName)) {
            "Only vrf of sub-interface is supported to write."
        }
        underlayAccess.safeDelete(getUnderlayIid(vrfName, dataBefore.id), getData(vrfName, dataBefore))
    }

    override fun writeCurrentAttributes(iid: InstanceIdentifier<Config>, dataAfter: Config, wc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        val interfaceName = iid.firstKeyOf(Interface::class.java).id
        require(InterfaceReader.isSubinterface(interfaceName)) {
            "Only vrf of sub-interface is supported to write."
        }
        val ifcName = findInterfaceName(dataAfter.id)
        val subifcIndex = findSubinterfaceName(dataAfter.id)

        val configurations = underlayAccess.read(InterfaceReader.IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()

        val subIfcKeys = InterfaceReader.getInterfaceIds(configurations)
            .filter { InterfaceReader.isSubinterface(it.name) }
            .filter { it.name.startsWith(ifcName) }
            .map { InterfaceReader.getSubinterfaceKey(it.name) }
            .map { it.index }

        require(subIfcKeys.contains(subifcIndex)) {
            val subIfcName = ifcName + "." + dataAfter.id
            "Interface: $subIfcName does not exist, cannot add it to VRF".trimIndent()
        }

        underlayAccess.safePut(getUnderlayIid(vrfName, dataAfter.id), getData(vrfName, dataAfter))
    }

    companion object {
        private fun findInterfaceName(id: String): String {
            val matcher = InterfaceReader.SUBINTERFACE_NAME.matcher(id)
            matcher.find()
            return matcher.group("ifcId")
        }

        private fun findSubinterfaceName(id: String): Long {
            val matcher = InterfaceReader.SUBINTERFACE_NAME.matcher(id)
            matcher.find()
            return matcher.group("subifcIndex").toLong()
        }
    }
}