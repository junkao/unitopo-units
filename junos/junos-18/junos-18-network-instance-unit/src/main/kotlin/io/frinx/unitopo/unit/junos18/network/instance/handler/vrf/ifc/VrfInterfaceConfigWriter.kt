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

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.ni.base.handler.vrf.ifc.AbstractVrfInterfaceConfigWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.network.instance.handler.vrf.L3VrfReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.instance.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.instance.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.instance.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.Instance
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class VrfInterfaceConfigWriter(underlayAccess: UnderlayAccess) :
    AbstractVrfInterfaceConfigWriter<Interface>(underlayAccess) {

    override fun deleteCurrentAttributes(iid: InstanceIdentifier<Config>, dataBefore: Config, wtc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        underlayAccess.delete(getUnderlayIid(vrfName, dataBefore.id))
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        // There are no modifiable attributes.
    }

    override fun writeCurrentAttributes(iid: InstanceIdentifier<Config>, dataAfter: Config, wtc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        underlayAccess.put(getUnderlayIid(vrfName, dataAfter.id), getData(vrfName, dataAfter))
    }

    override fun getUnderlayIid(vrfName: String, ifcName: String): InstanceIdentifier<Interface> {
        return L3VrfReader.JUNOS_VRFS_ID.child(Instance::class.java, InstanceKey(vrfName))
            .child(Interface::class.java, InterfaceKey(ifcName))
    }

    override fun getData(vrfName: String, config: Config): Interface {
        return InterfaceBuilder().setName(config.id).build()
    }
}