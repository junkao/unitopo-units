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

package io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ip.vrrp.top.VrrpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ip.vrrp.top.vrrp.VrrpGroup
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ip.vrrp.top.vrrp.VrrpGroupBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ip.vrrp.top.vrrp.VrrpGroupKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.family.inet.address.VrrpGroup as JunosVrrpGroup

open class SubinterfaceVrrpGroupReader(private val underlayAccess: UnderlayAccess)
    : ConfigListReaderCustomizer<VrrpGroup, VrrpGroupKey, VrrpGroupBuilder> {

    override fun getAllIds(iid: InstanceIdentifier<VrrpGroup>, context: ReadContext): List<VrrpGroupKey> {
        val ifcName = iid.firstKeyOf(Interface::class.java).name
        val unitId = iid.firstKeyOf(Subinterface::class.java).index
        return getSubInterfaceAddressIds(underlayAccess, ifcName, unitId.toString())
    }

    private fun getSubInterfaceAddressIds(underlayAccess: UnderlayAccess, ifcName: String, unitId: String):
            List<VrrpGroupKey> {
        val instanceIdentifier = InterfaceReader.JUNOS_IFCS
                .child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(unitId))

        return underlayAccess.read(instanceIdentifier, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                ?.let {
                    parseVrrpGroupIds(it)
                }.orEmpty()
    }

    override fun readCurrentAttributes(
        iid: InstanceIdentifier<VrrpGroup>,
        builder: VrrpGroupBuilder,
        ctx: ReadContext
    ) {
        val (ifcName, subIfcId, vrrpGroupKey) = resolveKeys(iid)
        InterfaceReader.readUnitVrrpGroup(underlayAccess, ifcName, subIfcId, vrrpGroupKey,
                { builder.fromUnderlay(it) })
    }

    private fun parseVrrpGroupIds(it: JunosInterfaceUnit): List<VrrpGroupKey> {
        val rtn = mutableListOf<VrrpGroupKey>()

        it.family?.inet?.address.orEmpty().map {
            it.vrrpGroup.orEmpty()
        }.map {
            it.map {
                rtn.add(VrrpGroupKey(it.name.toShort()))
            }
        }
        return rtn
    }
    fun resolveKeys(iid: InstanceIdentifier<VrrpGroup>): Triple<String, Long, VrrpGroupKey> {
        val ifcName = iid.firstKeyOf(Interface::class.java).name
        val subIfcId = iid.firstKeyOf(Subinterface::class.java).index
        val vrrpGroupKey = VrrpGroupKey(iid.firstKeyOf(VrrpGroup::class.java).virtualRouterId)
        return Triple(ifcName, subIfcId, vrrpGroupKey)
    }

    private fun VrrpGroupBuilder.fromUnderlay(vrrpGroup: JunosVrrpGroup) {
        key = VrrpGroupKey(vrrpGroup.name.toShort())
    }
    override fun merge(builder: Builder<out DataObject>, vrrpGroup: List<VrrpGroup>) {
        (builder as VrrpBuilder).vrrpGroup = vrrpGroup
    }

    override fun getBuilder(iid: InstanceIdentifier<VrrpGroup>): VrrpGroupBuilder {
        return VrrpGroupBuilder()
    }
}