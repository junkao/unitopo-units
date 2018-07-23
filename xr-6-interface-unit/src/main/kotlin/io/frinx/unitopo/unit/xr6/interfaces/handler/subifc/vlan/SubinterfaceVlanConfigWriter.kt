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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.vlan

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.getSubIfcName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.VlanSubConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.VlanSubConfigurationBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.vlan.sub.configuration.VlanIdentifierBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.Vlan
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.VlanTag
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.vlan.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.InterfaceConfiguration1 as VlanSubConfigurationAugmentation

class SubinterfaceVlanConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val underlayId = getId(id)
        writeContext.readAfter(RWUtils.cutId(id, IIDs.IN_IN_SU_SUBINTERFACE)).orNull()?.let {
            // Delete only if the subinterface stays. If we delete the VLAN tag for subifc and subinterface in a single TX,
            // XR fails the transaction
            underlayAccess.delete(underlayId)
        }
    }

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        val (ethServiceId, data) = getData(id, dataAfter)
        underlayAccess.put(ethServiceId, data)
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        deleteCurrentAttributes(id, dataBefore, writeContext)
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    // TODO this is really similar in all writers, try to extract some
    // common generalized behavior
    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<VlanSubConfiguration>, VlanSubConfiguration> {
        val underlayId = getId(id)

        val vlanIdBuilder = VlanIdentifierBuilder()
        vlanIdBuilder.firstTag = dataAfter.vlanId?.vlanId?.value
                ?.let { VlanTag(it.toLong()) }
        vlanIdBuilder.vlanType = Vlan.VlanTypeDot1q

        val builder = VlanSubConfigurationBuilder()
        builder.vlanIdentifier = vlanIdBuilder.build()

        return Pair(underlayId, builder.build())
    }

    private fun getId(id: InstanceIdentifier<Config>): InstanceIdentifier<VlanSubConfiguration> {
        // TODO supporting only "act" interfaces
        val interfaceActive = InterfaceActive("act")

        val ifcName = id.firstKeyOf(Interface::class.java).name
        val subifcIdx = id.firstKeyOf(Subinterface::class.java).index

        val subifcName = InterfaceName(getSubIfcName(ifcName, subifcIdx))

        val underlayIfcId = InterfaceReader.IFC_CFGS
                .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, subifcName))

        return underlayIfcId.augmentation(VlanSubConfigurationAugmentation::class.java)
                .child(VlanSubConfiguration::class.java)
    }
}