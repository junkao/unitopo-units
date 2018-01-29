/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.ospf.common.OspfReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.OspfMetric
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfAreaInterfaceConfigReader(private val underlayAccess: UnderlayAccess):
        OspfReader.OspfConfigReader<Config, ConfigBuilder> {

    override fun getBuilder(id: IID<Config>): ConfigBuilder = ConfigBuilder()

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as InterfaceBuilder).`config` = config
    }

    override fun readCurrentAttributesForType(id: IID<Config>, config: ConfigBuilder, readContext: ReadContext) {
        val areaId = String(id.firstKeyOf(Area::class.java).identifier.value)
        val ifaceId = id.firstKeyOf(Interface::class.java).id
        config.metric = OspfMetric(getMetric(areaId, ifaceId))
    }

    private fun getMetric(area: String, iface: String): Int? {
        val optIface = underlayAccess.read(OspfProtocolReader.getInterfaceId(area, iface)).checkedGet()
        if (!optIface.isPresent) {
            return null
        }
        return optIface.get().metric?.uint16
    }
}
