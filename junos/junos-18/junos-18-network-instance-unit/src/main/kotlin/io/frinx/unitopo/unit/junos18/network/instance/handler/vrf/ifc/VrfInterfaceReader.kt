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

package io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.ifc

import io.frinx.unitopo.ni.base.handler.vrf.ifc.AbstractVrfInterfaceReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.L3VrfReader
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.Instance
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceKey

class VrfInterfaceReader(underlayAccess: UnderlayAccess) : AbstractVrfInterfaceReader(underlayAccess) {

    override fun getAllInterfaces(vrfName: String): List<String> =
        underlayAccess.read(L3VrfReader.JUNOS_VRFS_ID.child(Instance::class.java, InstanceKey(vrfName)))
            .checkedGet()
            .orNull()?.`interface`.orEmpty()
            .map { it.name }
}