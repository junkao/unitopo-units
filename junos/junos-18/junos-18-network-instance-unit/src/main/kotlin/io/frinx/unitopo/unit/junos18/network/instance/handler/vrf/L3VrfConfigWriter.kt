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

package io.frinx.unitopo.unit.junos18.network.instance.handler.vrf

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L3VRF
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.Instance
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class L3VrfConfigWriter(private val underlayAccess: UnderlayAccess) : CompositeWriter.Child<Config> {

    override fun deleteCurrentAttributesWResult(iid: IID<Config>, dataBefore: Config, wtc: WriteContext): Boolean {
        if (dataBefore.type != L3VRF::class.java) {
            return false
        }

        val vrfIid = getVrfIdentifier(dataBefore.name)
        val emptyData = InstanceBuilder().setName(dataBefore.name).build()
        underlayAccess.put(vrfIid, emptyData)
        return true
    }

    override fun updateCurrentAttributesWResult(
        id: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ): Boolean {
        if (dataBefore.type != L3VRF::class.java) {
            return false
        }
        // There are no modifiable attributes.
        return true
    }

    override fun writeCurrentAttributesWResult(iid: IID<Config>, dataAfter: Config, wtc: WriteContext): Boolean {
        if (dataAfter.type != L3VRF::class.java) {
            return false
        }

        val (vrfIid, vrf) = getVrfData(dataAfter, InstanceBuilder())
        // We should use merge method because this container may have attributes that are not covered by OpenConfig,
        // and it will be deleted.
        underlayAccess.merge(vrfIid, vrf)
        return true
    }

    private fun getVrfData(data: Config, vrfBuilder: InstanceBuilder): Pair<IID<Instance>, Instance> {
        val vrfIid = getVrfIdentifier(data.name)

        val vrf = vrfBuilder
            .setName(data.name)

        return Pair(vrfIid, vrf.build())
    }

    companion object {
        private fun getVrfIdentifier(vrfName: String): IID<Instance> {
            return L3VrfReader.JUNOS_VRFS_ID.child(Instance::class.java, InstanceKey(vrfName))
        }
    }
}