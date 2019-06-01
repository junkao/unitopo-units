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

package io.frinx.unitopo.unit.xr6.network.instance.handler.l2vsi

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.builder.BasicCheck
import io.fd.honeycomb.translate.spi.builder.Check
import io.frinx.translate.unit.commons.handler.spi.CompositeListReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.BridgeDomainGroups
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.BridgeDomainGroup
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.BridgeDomainGroupKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.BridgeDomains
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.BridgeDomain
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.vfis.Vfi
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class L2VSIReader(private val underlayAccess: UnderlayAccess) :
    CompositeListReader.Child<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder> {

    override fun getCheck(): Check {
        return BasicCheck.emptyCheck()
    }

    override fun getBuilder(p0: InstanceIdentifier<NetworkInstance>): NetworkInstanceBuilder {
        // NOOP
        throw UnsupportedOperationException("Should not be invoked")
    }

    @Throws(ReadFailedException::class)
    override fun getAllIds(
        instanceIdentifier: InstanceIdentifier<NetworkInstance>,
        readContext: ReadContext
    ): List<NetworkInstanceKey> {
        return getAllIds(underlayAccess)
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<NetworkInstance>,
        networkInstanceBuilder: NetworkInstanceBuilder,
        readContext: ReadContext
    ) {
        val name = instanceIdentifier.firstKeyOf(NetworkInstance::class.java).name
        networkInstanceBuilder.name = name
    }

    companion object {

        public val BDG_KEY = "frinx"

        public val UNDERLAY_BD_ID = InstanceIdentifier.create(L2vpn::class.java)
                .child(Database::class.java)
                .child(BridgeDomainGroups::class.java)
                .child(BridgeDomainGroup::class.java, BridgeDomainGroupKey(CiscoIosXrString(BDG_KEY)))
                .child(BridgeDomains::class.java)!!

        fun getAllIds(underlayAccess: UnderlayAccess): List<NetworkInstanceKey> {
            return getAllVfis(underlayAccess)
                    .map { vfi -> vfi.name.value }
                    .map { vfiName -> NetworkInstanceKey(vfiName) }
        }

        public fun getAllVfis(underlayAccess: UnderlayAccess): List<Vfi> {
            return getAllBridgeDomains(underlayAccess)
                    // Only accept vfis with name == bd.name
                    .map { bd -> bd.getVfiWithSameName() }
                    .filter { vfi -> vfi != null }
                    .map { vfi -> vfi!! }
                    .toList()
        }

        public fun getAllBridgeDomains(underlayAccess: UnderlayAccess): List<BridgeDomain> {
            return underlayAccess.read(UNDERLAY_BD_ID)
                    .checkedGet()
                    .orNull()
                    ?.bridgeDomain.orEmpty()
        }

        private fun BridgeDomain.getVfiWithSameName(): Vfi? = this.vfis?.vfi.orEmpty()
                .firstOrNull { vfi -> vfi.name.value == name.value }
    }
}