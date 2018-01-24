/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos17.mpls.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.mpls.common.MplsReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.Tunnel
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.TunnelBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.TunnelKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.tunnel.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.tunnel.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.types.rev170824.LSPMETRICABSOLUTE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.types.rev170824.P2P
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.mpls.LabelSwitchedPath
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException

class TunnelConfigReader(private val access: UnderlayAccess) : MplsReader.MplsConfigReader<Config, ConfigBuilder> {

    override fun getBuilder(p0: InstanceIdentifier<Config>): ConfigBuilder = ConfigBuilder()

    override fun readCurrentAttributesForType(instanceIdentifier: InstanceIdentifier<Config>, configBuilder: ConfigBuilder, readContext: ReadContext) {
        val name = instanceIdentifier.firstKeyOf<Tunnel, TunnelKey>(Tunnel::class.java).name
        configBuilder.name = name
        try {
            readTunnel(access, name)?.let { configBuilder.parseConfig(it) }
        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as TunnelBuilder).config = readValue
    }

    @VisibleForTesting
    fun ConfigBuilder.parseConfig(path : LabelSwitchedPath) {
        isShortcutEligible = true
        type = P2P::class.java
        metric = path.metric?.uint32?.toInt()
        if (metric != null) {
            metricType = LSPMETRICABSOLUTE::class.java
        }
    }

    companion object {
        fun readTunnel(underlayAccess: UnderlayAccess, name: String) : LabelSwitchedPath? {
            return underlayAccess.read(TunnelReader.MPLS).checkedGet().orNull()?.let {
                it.labelSwitchedPath.orEmpty()
                        .find { it.name == name }
            }
        }
    }
}