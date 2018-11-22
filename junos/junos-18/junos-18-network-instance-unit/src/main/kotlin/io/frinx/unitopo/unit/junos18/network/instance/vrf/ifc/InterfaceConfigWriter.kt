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

package io.frinx.unitopo.unit.junos18.network.instance.vrf.ifc

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.network.instance.vrf.VrfReader
import io.frinx.unitopo.unit.network.instance.common.L3VrfWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.instance.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.instance.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.instance.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.Instance
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceKey
import io.frinx.openconfig.network.instance.NetworInstance as DefaultNetworkInstance
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : L3VrfWriter<Config> {

    override fun deleteCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, wtc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name!!
        val ifcId = getInterfaceIdentifier(vrfName, dataBefore.id)

        underlayAccess.delete(ifcId)
    }

    override fun updateCurrentAttributesForType(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        // There are no modifiable attributes.
    }

    override fun writeCurrentAttributesForType(iid: IID<Config>, dataAfter: Config, wtc: WriteContext) {
        val vrfKey = iid.firstKeyOf(NetworkInstance::class.java)
        require(vrfKey != DefaultNetworkInstance.DEFAULT_NETWORK) {
            "Cannot configure interface in default network instance. Vrf: ${vrfKey.name}, Interface: ${dataAfter.id}"
        }

        val (iid, underlayData) = getInterfaceData(vrfKey.name, dataAfter, InterfaceBuilder())
        underlayAccess.put(iid, underlayData)
    }

    companion object {
        private fun getInterfaceIdentifier(vrfName: String, ifcName: String): IID<Interface> {
            return VrfReader.JUNOS_VRFS_ID.child(Instance::class.java, InstanceKey(vrfName))
                .child(Interface::class.java, InterfaceKey(ifcName))
        }

        private fun getInterfaceData(vrfName: String, config: Config, builder: InterfaceBuilder):
                Pair<IID<Interface>, Interface> {

            val id = getInterfaceIdentifier(vrfName, config.id)

            builder.fromOpenConfig(config)
            return Pair(id, builder.build())
        }

        fun InterfaceBuilder.fromOpenConfig(config: Config): InterfaceBuilder {
            this.name = config.id
            return this
        }
    }
}