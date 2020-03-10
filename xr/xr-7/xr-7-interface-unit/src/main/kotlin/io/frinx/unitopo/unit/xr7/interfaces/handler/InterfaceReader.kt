/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.interfaces.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.ifc.base.handler.AbstractInterfaceReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceReader(underlayAccess: UnderlayAccess) :
    AbstractInterfaceReader<InterfaceConfigurations>(underlayAccess) {

    override fun parseInterfaceIds(data: InterfaceConfigurations): List<InterfaceKey> =
        data.interfaceConfiguration
        .orEmpty()
        .map { InterfaceKey(it.interfaceName.value) }

    override val readIid: InstanceIdentifier<InterfaceConfigurations> = IFC_CFGS

    override fun getAllIds(instanceIdentifier: InstanceIdentifier<Interface>, readContext: ReadContext):
        List<InterfaceKey> = getInterfaceIds().filter { !Util.isSubinterface(it.name) }

    companion object {
        val IFC_CFGS = InstanceIdentifier.create(InterfaceConfigurations::class.java)!!

        fun readInterfaceCfg(
            underlayAccess: UnderlayAccess,
            name: String,
            handler: (InterfaceConfiguration) -> Unit
        ) {
            val configurations = underlayAccess.read(IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
            configurations?.let { interfaceConfigurations ->
                interfaceConfigurations.interfaceConfiguration.orEmpty()
                    .firstOrNull { it.interfaceName.value == name }
                    .let { handler(it ?: Util.getDefaultIfcCfg(name)) }
            }
        }
    }
}