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

package io.frinx.unitopo.unit.xr6.network.instance.handler.vrf

import io.frinx.unitopo.ni.base.handler.vrf.AbstractL3VrfReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L3VrfReader(underlayAccess: UnderlayAccess) : AbstractL3VrfReader<Vrf>(underlayAccess) {

    override fun parseIds(): MutableList<NetworkInstanceKey> =
        underlayAccess.read(InstanceIdentifier.create(Vrfs::class.java)).checkedGet().orNull()
            ?.let {
                vrfs -> vrfs.vrf?.map { NetworkInstanceKey(it.vrfName.value) }?.toCollection(mutableListOf())
            }.orEmpty().toMutableList()

    fun vrfExists(name: String): Boolean {
        return parseIds().contains(NetworkInstanceKey(name))
    }

    companion object {
        val VRFS_ID = InstanceIdentifier.create(Vrfs::class.java)!!
    }
}