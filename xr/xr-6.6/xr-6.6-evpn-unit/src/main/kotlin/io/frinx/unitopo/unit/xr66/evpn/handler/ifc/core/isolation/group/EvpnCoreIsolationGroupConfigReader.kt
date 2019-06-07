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

package io.frinx.unitopo.unit.xr7.evpn.handler.ifc.core.isolation.group

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.evpn.handler.group.EvpnGroupListReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.EvpnInterfaces
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.evpn.evpn.tables.evpn.interfaces.EvpnInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.CoreIsolationGroupBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.core.isolation.group.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.interfaces.interfaces._interface.core.isolation.group.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnCoreIsolationGroupConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Config>) = ConfigBuilder()

    override fun readCurrentAttributes(
        id: IID<Config>,
        builder: ConfigBuilder,
        readContext: ReadContext
    ) {
        var ifcName = id.firstKeyOf(Interface::class.java).name
        val underlayId = EvpnGroupListReader.EVPN_TABLES
            .child(EvpnInterfaces::class.java)

        underlayAccess.read(underlayId, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let {
                it.evpnInterface?.filter {
                    it.interfaceName.value == ifcName
                }?.get(0)?.let {
                    builder.fromUnderlay(it)
                }
            }
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as CoreIsolationGroupBuilder).config = config
    }
}

fun ConfigBuilder.fromUnderlay(ifc: EvpnInterface) {
    ifc.evpnCoreIsolationGroup?.let {
        id = it.value
    }
}