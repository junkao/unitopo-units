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

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.handlers.l3vrf.L3VrfReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.network.instance.vrf.VrfReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.Instance
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.instance.Interface as JunosInstInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.juniper.routing.instance.InterfaceKey as JunosInstInterfaceKey

class InterfaceConfigReader(private val underlayAccess: UnderlayAccess) :
        L3VrfReader.L3VrfConfigReader<Config, ConfigBuilder> {

    override fun readCurrentAttributesForType(
        id: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        ctx: ReadContext
    ) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name!!
        val ifcName = id.firstKeyOf(Interface::class.java).id!!

        underlayAccess.read(
            VrfReader.JUNOS_VRFS_ID
                .child(Instance::class.java, InstanceKey(vrfName))
                .child(JunosInstInterface::class.java, JunosInstInterfaceKey(ifcName))
        )
            .checkedGet()
            .orNull()
            ?.let {
                    builder.fromUnderlay(it)
                }
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as InterfaceBuilder).config = config
    }

    override fun getBuilder(id: InstanceIdentifier<Config>): ConfigBuilder = ConfigBuilder()
}

private fun ConfigBuilder.fromUnderlay(underlay: JunosInstInterface) {
    this.id = underlay.name
}