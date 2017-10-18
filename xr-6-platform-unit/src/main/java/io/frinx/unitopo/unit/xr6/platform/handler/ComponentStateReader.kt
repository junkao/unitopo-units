/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.platform.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.sc.invmgr.admin.oper.rev151109.Inventory
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.sc.invmgr.admin.oper.rev151109.basic.attributes.basic.attributes.BasicInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.sc.invmgr.admin.oper.rev151109.inventory.Racks
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev161222.PlatformComponentState
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev161222.platform.component.top.components.Component
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev161222.platform.component.top.components.ComponentBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev161222.platform.component.top.components.component.State
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev161222.platform.component.top.components.component.StateBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.types.rev170816.LINECARD
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ComponentStateReader(private val access: UnderlayAccess) : ReaderCustomizer<State, StateBuilder> {

    override fun getBuilder(id: InstanceIdentifier<State>) = StateBuilder()


    override fun readCurrentAttributes(id: InstanceIdentifier<State>, builder: StateBuilder,
                                       ctx: ReadContext) {

        access.read(RACKS_ID).checkedGet().orNull()
                ?.let {
                    getBasicInfoForComponent(id, it)
                }
                ?.let {
                    builder.fromUnderlay(it)
                }
    }

    private fun StateBuilder.fromUnderlay(basicInfo: BasicInfo) {
        serialNo = basicInfo.serialNumber
        description = basicInfo.description
        version = basicInfo.hardwareRevision
        id = basicInfo.name
        partNo = basicInfo.modelName
        mfgName = basicInfo.manufacturerName

        // TODO We are reading just line cards now, so it should be fine
        // to always set LINECARD type for now. But in the future we should
        // take into account also other types
        type = PlatformComponentState.Type(LINECARD::class.java)
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: State) {
        (parentBuilder as ComponentBuilder).state = readValue
    }

    companion object {

        // TODO RACKS_ID InstanceIdentifier is defined also in ComponentReader class.
        // We should move it to some more appropriate place, so it can be reused.
        private val RACKS_ID = InstanceIdentifier.create(Inventory::class.java).child(Racks::class.java)

        private fun getBasicInfoForComponent(id: InstanceIdentifier<State>, racks: Racks): BasicInfo? {
            // TODO The component's name from XR's inventory model is not the best key for
            // Component. Maybe we should try to encode the real key from the inventory model
            // so we can read directly the node we want
            val cardName = id.firstKeyOf(Component::class.java).name

            return racks.rack?.flatMap { it?.slots?.slot.orEmpty() }
                    ?.flatMap { it?.cards?.card.orEmpty() }
                    ?.map { it?.basicAttributes?.basicInfo }
                    // TODO there should be exactly one card with given name
                    // but anyway we should check that here, if that is really
                    // the case
                    ?.last { cardName == it?.name }
        }
    }
}
