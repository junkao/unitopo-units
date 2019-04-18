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

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.ni.base.handler.vrf.AbstractL3VrfReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.routing.instances.group.RoutingInstances
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.routing.instances.rev180101.Configuration1 as RoutingInstanceConfigurationAug

class L3VrfReader(underlayAccess: UnderlayAccess) : AbstractL3VrfReader<RoutingInstances>(underlayAccess) {

    private val readIId = InstanceIdentifier.create(Configuration::class.java)
        .augmentation(RoutingInstanceConfigurationAug::class.java).child(RoutingInstances::class.java)!!

    override fun getAllIds(id: InstanceIdentifier<NetworkInstance>, context: ReadContext): List<NetworkInstanceKey> =
        parseIds().toList()

    override fun parseIds(): MutableList<NetworkInstanceKey> =
        underlayAccess.read(readIId).checkedGet().orNull()
            ?.let {
                instances -> instances.instance.orEmpty().map { NetworkInstanceKey(it.name) }
                .toMutableList()
            }!!

    fun vrfExists(name: String): Boolean = parseIds().contains(NetworkInstanceKey(name))

    companion object {
        private val JUNOS_CFG = InstanceIdentifier.create(Configuration::class.java)!!
        private val JUNOS_RI_AUG = JUNOS_CFG.augmentation(RoutingInstanceConfigurationAug::class.java)!!
        val JUNOS_VRFS_ID = JUNOS_RI_AUG.child(RoutingInstances::class.java)!!
    }
}