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
package io.frinx.unitopo.unit.junos.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.handlers.ospf.OspfListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.AreasBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.OspfAreaIdentifier
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class OspfAreaReader(private val underlayAccess: UnderlayAccess) :
        OspfListReader.OspfConfigListReader<Area, AreaKey, AreaBuilder> {

    override fun getBuilder(id: IID<Area>): AreaBuilder = AreaBuilder()

    override fun merge(builder: Builder<out DataObject>, areas: List<Area>) {
        (builder as AreasBuilder).`area` = areas
    }

    override fun readCurrentAttributesForType(id: IID<Area>, area: AreaBuilder, readContext: ReadContext) {
        area.key = AreaKey(id.firstKeyOf(Area::class.java))
    }

    override fun getAllIdsForType(id: IID<Area>, readContext: ReadContext): List<AreaKey> {
        val optOspf = underlayAccess.read(OspfProtocolReader.getOspfId()).checkedGet()
        if (!optOspf.isPresent) {
            return emptyList()
        }
        return optOspf.get()?.`area`.orEmpty().map {
                AreaKey(OspfAreaIdentifier(DottedQuad(it.name.value)))
            }
    }
}