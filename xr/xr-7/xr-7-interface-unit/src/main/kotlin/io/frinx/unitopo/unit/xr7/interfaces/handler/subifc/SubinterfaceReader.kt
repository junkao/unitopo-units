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

package io.frinx.unitopo.unit.xr7.interfaces.handler.subifc

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.Util
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations._interface.configuration.mtus.MtuKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev180111.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class SubinterfaceReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Subinterface, SubinterfaceKey, SubinterfaceBuilder> {

    private val ifcReader = InterfaceReader(underlayAccess)

    override fun getAllIds(id: InstanceIdentifier<Subinterface>, context: ReadContext): MutableList<SubinterfaceKey> {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val configurations = underlayAccess.read(InterfaceReader.IFC_CFGS, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
        val subIfcKeys = ifcReader.getInterfaceIds()
                .filter { Util.isSubinterface(it.name) }
                .filter { it.name.startsWith(ifcName) }
                .map { Util.getSubinterfaceKey(it.name) }

        val ipv4Keys = mutableListOf<AddressKey>()
        InterfaceReader.readInterfaceCfg(underlayAccess, ifcName,
                { Ipv4AddressReader.extractAddresses(it, ipv4Keys) })

        val mtuKeys = mutableListOf<MtuKey>()
        InterfaceReader.readInterfaceCfg(underlayAccess, ifcName,
            { extractMtus(it, mtuKeys) })

        return if (!ipv4Keys.isEmpty() || !mtuKeys.isEmpty())
            subIfcKeys.plus(SubinterfaceKey(ZERO_SUBINTERFACE_ID)).toMutableList() else
            subIfcKeys.toMutableList()
    }

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Subinterface>,
        builder: SubinterfaceBuilder,
        ctx: ReadContext
    ) {
        builder.index = id.firstKeyOf(Subinterface::class.java).index
    }

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<Subinterface>) {
        (builder as SubinterfacesBuilder).subinterface = readData
    }

    override fun getBuilder(p0: InstanceIdentifier<Subinterface>): SubinterfaceBuilder = SubinterfaceBuilder()

    companion object {
        const val ZERO_SUBINTERFACE_ID = 0L
        fun getSubIfcName(ifcName: String, subifcIdx: Long) = ifcName + "." + subifcIdx

        fun extractMtus(ifcCfg: InterfaceConfiguration, keys: MutableList<MtuKey>) {
            ifcCfg.getAugmentation(InterfaceConfiguration1::class.java)?.let {
                it.ipv4Network?.let {
                    it.mtu?.let {
                        keys.add(MtuKey(CiscoIosXrString(it.toString())))
                    }
                }
            }
        }
    }
}