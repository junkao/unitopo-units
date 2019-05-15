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

package io.frinx.unitopo.unit.xr6.interfaces.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.ifc.base.handler.AbstractInterfaceReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730.InterfaceProperties
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.properties.DataNodes
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.table.interfaces.Interface as UnderlayInterface

class InterfaceReader(underlayAccess: UnderlayAccess) : AbstractInterfaceReader<DataNodes>(underlayAccess) {

    override fun parseInterfaceIds(data: DataNodes): List<InterfaceKey> =
        data.dataNode.orEmpty()
        .flatMap { it.systemView?.interfaces?.`interface`.orEmpty() }
        .map { it.key }
        .map { InterfaceKey(it.interfaceName.value) }

    override fun getAllIds(instanceIdentifier: InstanceIdentifier<Interface>, readContext: ReadContext):
        List<InterfaceKey> = getInterfaceIds().filter { !Util.isSubinterface(it.name) }

    override val readIid: InstanceIdentifier<DataNodes> = DATA_NODES_ID

    override val readDSType: LogicalDatastoreType = LogicalDatastoreType.OPERATIONAL

    companion object {
        /**
         * Uses DATA_NODES_ID/interface-properties instead of IFC_CFGS because IFC_CFGS does not return un-configured
         * interfaces.
         */
        val IFC_CFGS = InstanceIdentifier.create(InterfaceConfigurations::class.java)!!
        val DATA_NODES_ID = InstanceIdentifier.create(InterfaceProperties::class.java).child(DataNodes::class.java)!!

        fun readInterfaceCfg(
            underlayAccess: UnderlayAccess,
            name: String,
            handler: (InterfaceConfiguration) -> Unit
        ) {
            underlayAccess.read(IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                ?.let { interfaceConfigurations ->
                    interfaceConfigurations.interfaceConfiguration.orEmpty()
                        .firstOrNull { it.interfaceName.value == name }
                        // Invoke handler with read value or use default
                        // XR returns no config data for interface that has no configuration but is up
                        .let { handler(it ?: Util.getDefaultIfcCfg(name)) }
                }
        }

        /**
         * Read interface properties
         */
        fun readInterfaceProps(
            underlayAccess: UnderlayAccess,
            name: String,
            handler: (UnderlayInterface) -> Unit
        ) {
            underlayAccess.read(DATA_NODES_ID, LogicalDatastoreType.OPERATIONAL)
                    .checkedGet()
                    .orNull()
                    ?.let { dataNodes ->
                        dataNodes.dataNode.orEmpty()
                                .flatMap { it.systemView?.interfaces?.`interface`.orEmpty() }
                                .firstOrNull { it.interfaceName.value == name }
                                ?.let { handler(it) }
                    }
        }
    }
}