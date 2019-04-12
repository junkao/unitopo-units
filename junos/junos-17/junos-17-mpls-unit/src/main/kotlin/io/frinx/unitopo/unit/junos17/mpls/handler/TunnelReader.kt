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
package io.frinx.unitopo.unit.junos17.mpls.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.TunnelsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.Tunnel
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.TunnelBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.TunnelKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.Protocols
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.Mpls
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class TunnelReader(private val underlayAccess: UnderlayAccess)
    : ConfigListReaderCustomizer<Tunnel, TunnelKey, TunnelBuilder> {

    override fun getBuilder(p0: InstanceIdentifier<Tunnel>): TunnelBuilder = TunnelBuilder()

    override fun getAllIds(instanceIdentifier: InstanceIdentifier<Tunnel>, readContext: ReadContext):
        List<TunnelKey> {
        try {
            return getTunnelKeys(underlayAccess)
        } catch (e: org.opendaylight.controller.md.sal.common.api.data.ReadFailedException) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<Tunnel>) {
        (builder as TunnelsBuilder).tunnel = readData
    }

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Tunnel>,
        tunnelBuilder: TunnelBuilder,
        readContext: ReadContext
    ) {
        val key = instanceIdentifier.firstKeyOf(Tunnel::class.java)
        tunnelBuilder.name = key.name
    }

    companion object {

        val MPLS = InstanceIdentifier.create(Configuration::class.java)
                .child(Protocols::class.java)
                .child(Mpls::class.java)!!

        @VisibleForTesting
        fun getTunnelKeys(underlayAccess: UnderlayAccess): List<TunnelKey> {
            return underlayAccess.read(MPLS, LogicalDatastoreType.OPERATIONAL)
                    .checkedGet()
                    .orNull()
                    ?.let { parsePaths(it) }.orEmpty()
        }

        @VisibleForTesting
        fun parsePaths(it: Mpls): List<TunnelKey> {
            return it.labelSwitchedPath.orEmpty()
                .map { TunnelKey(it.name) }
                .toList()
        }
    }
}