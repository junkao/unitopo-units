/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos17.mpls.handler

import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnel.p2p_top.P2pTunnelAttributes
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.Tunnel
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.TunnelKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.tunnels.tunnel.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.types.rev170824.LSPMETRICABSOLUTE
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Ipv4addr
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.mpls.LabelSwitchedPath
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.mpls.LabelSwitchedPathBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.mpls.LabelSwitchedPathKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.mpls.label.switched.path.label.switched.path.or.template.Case1Builder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class TunnelConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    @Throws(WriteFailedException::class)
    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, data: Config, writeContext: WriteContext) {
        checkTunnelConfig(data)
        val (underlayId, underlayIfcCfg) = getData(id, data, writeContext)
        try {
            underlayAccess.put(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun checkTunnelConfig(data: Config) {
        // TODO What if metric-type is set but metric is not
        data.metric?.let {
            Preconditions.checkArgument(LSPMETRICABSOLUTE::class.java == data.metricType,
                    "Only LSP_METRIC_ABSOLUTE metric type is supported")
            Preconditions.checkArgument(data.isShortcutEligible!!,
                    "Cannot configure metric on non shortcut-eligible tunnel")
        }
    }

    @Throws(WriteFailedException::class)
    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, data: Config, writeContext: WriteContext) {
        try {
            underlayAccess.delete(getId(id))
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext):
            Pair<InstanceIdentifier<LabelSwitchedPath>, LabelSwitchedPath> {
        val attrs = writeContext.readAfter(RWUtils.cutId(id, Tunnel::class.java).child(P2pTunnelAttributes::class.java))?.orNull()
        attrs ?: throw IllegalArgumentException("Destination path MUST be specified")

        val to = attrs.config?.destination?.ipv4Address?.value
        val underlayIfcCfg = LabelSwitchedPathBuilder()
                .setName(dataAfter.name)
                .setLabelSwitchedPathOrTemplate(Case1Builder().setTo(Ipv4addr(to)).build())
                .setMetric(LabelSwitchedPath.Metric(dataAfter.metric?.toLong()))
                .build()
        return Pair(getId(id), underlayIfcCfg)
    }

    private fun getId(id: InstanceIdentifier<Config>):
            InstanceIdentifier<LabelSwitchedPath> {
        val name = id.firstKeyOf<Tunnel, TunnelKey>(Tunnel::class.java).name
        return TunnelReader.MPLS.child(LabelSwitchedPath::class.java, LabelSwitchedPathKey(name))
    }
}