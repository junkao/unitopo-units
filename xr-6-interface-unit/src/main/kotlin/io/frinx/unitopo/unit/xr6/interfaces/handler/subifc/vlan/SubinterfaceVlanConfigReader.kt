/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.vlan

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.getSubIfcName
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.VlanLogicalConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.VlanBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.vlan.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.vlan.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.types.rev170714.VlanId
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.InterfaceConfiguration2 as EthServiceAugment
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.InterfaceConfiguration1 as VlanSubConfig

class SubinterfaceVlanConfigReader(private val underlayAccess: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val subifcIndex = id.firstKeyOf(Subinterface::class.java).index

        val subIfcName = getSubIfcName(ifcName, subifcIndex)
        InterfaceReader.readInterfaceCfg(underlayAccess, subIfcName, { builder.fromUnderlay(it) })
    }

    override fun getBuilder(id: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as VlanBuilder).config = readValue
    }
}

private fun ConfigBuilder.fromUnderlay(underlay: InterfaceConfiguration) {
    underlay.getAugmentation(VlanSubConfig::class.java)
            ?.vlanSubConfiguration?.vlanIdentifier?.firstTag?.value
            ?.let { vlanId = VlanLogicalConfig.VlanId(VlanId(it.toInt()))}
}