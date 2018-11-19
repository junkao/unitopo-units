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

package io.frinx.unitopo.unit.junos18.network.instance.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.translate.unit.commons.handler.spi.CompositeReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L3VRF
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.Instance
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.routing.instances.InstanceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class VrfConfigReader(private val underlayAccess: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder>,
        CompositeReader.Child<Config, ConfigBuilder> {

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        val name = id.firstKeyOf(NetworkInstance::class.java).name!!

        if (!isVrf(name)) {
            return
        }

        underlayAccess.read(VrfReader.JUNOS_VRFS_ID.child(Instance::class.java, InstanceKey(name)))
                .checkedGet()
                .orNull()
                ?.let { builder.fromUnderlay(it, name) }
    }

    private fun isVrf(name: String): Boolean {
        return VrfReader.getAllIds(underlayAccess)
            .map { it.name }
            .toList()
            .contains(name)
    }

    override fun getBuilder(p0: InstanceIdentifier<Config>): ConfigBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }
}

private fun ConfigBuilder.fromUnderlay(underlay: Instance, vrfName: String) {
    name = vrfName
    type = L3VRF::class.java
}