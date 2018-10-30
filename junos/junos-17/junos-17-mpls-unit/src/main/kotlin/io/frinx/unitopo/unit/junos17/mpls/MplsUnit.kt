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

package io.frinx.unitopo.unit.junos17.mpls

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.mpls.handler.NiMplsRsvpIfSubscripAugReader
import io.frinx.unitopo.unit.junos17.mpls.handler.NiMplsRsvpIfSubscripAugWriter
import io.frinx.unitopo.unit.junos17.mpls.handler.P2pAttributesConfigReader
import io.frinx.unitopo.unit.junos17.mpls.handler.RsvpInterfaceConfigReader
import io.frinx.unitopo.unit.junos17.mpls.handler.RsvpInterfaceConfigWriter
import io.frinx.unitopo.unit.junos17.mpls.handler.RsvpInterfaceReader
import io.frinx.unitopo.unit.junos17.mpls.handler.TeInterfaceConfigReader
import io.frinx.unitopo.unit.junos17.mpls.handler.TeInterfaceConfigWriter
import io.frinx.unitopo.unit.junos17.mpls.handler.TeInterfaceReader
import io.frinx.unitopo.unit.junos17.mpls.handler.TunnelConfigReader
import io.frinx.unitopo.unit.junos17.mpls.handler.TunnelConfigWriter
import io.frinx.unitopo.unit.junos17.mpls.handler.TunnelReader
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.extension.rev171024.NiMplsRsvpIfSubscripAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.mpls.top.MplsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.mpls.top.mpls.LspsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.mpls.top.mpls.SignalingProtocolsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.mpls.top.mpls.TeInterfaceAttributesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.mpls.top.mpls.lsps.ConstrainedPathBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnel.p2p_top.P2pTunnelAttributesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te.tunnels_top.TunnelsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.mpls.rsvp.subscription.SubscriptionBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.mpls.rsvp.subscription.subscription.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.RsvpTeBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.rsvp.rev170824.rsvp.global.rsvp.te.InterfaceAttributesBuilder
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.extension.rev171024.`$YangModuleInfoImpl` as RsvpExtension
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.`$YangModuleInfoImpl` as MplsYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as JunosYangInfo

class MplsUnit(private val registry: TranslationUnitCollector) : TranslateUnit {

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
        RsvpExtension.getInstance(),
        MplsYangInfo.getInstance()
    )

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        if (reg != null) {
            reg!!.close()
        }
    }

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
            JunosYangInfo.getInstance()
    )

    override fun getRpcs(underlayAccess: UnderlayAccess): Set<RpcService<*, *>> = emptySet()

    override fun provideHandlers(
        rRegistry: ModifiableReaderRegistryBuilder,
        wRegistry: ModifiableWriterRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.NE_NE_MPLS, NoopWriter()))

        // RSVP
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_SI_RSVPTE, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_SI_RS_IN_INTERFACE, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_SI_RS_IN_IN_CONFIG, RsvpInterfaceConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_SI_RS_IN_IN_SU_CONFIG, NoopWriter()))
        wRegistry.addAfter(GenericWriter(IIDs.NE_NE_MP_SI_RS_IN_IN_SU_CONFIG.augmentation(
            NiMplsRsvpIfSubscripAug::class.java), NiMplsRsvpIfSubscripAugWriter(underlayAccess)),
                IIDs.NE_NE_MP_SI_RS_IN_IN_CONFIG)

        // TE
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_TE_INTERFACE, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_TE_IN_CONFIG, TeInterfaceConfigWriter(underlayAccess)))

        // Tunnel
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LSPS, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LS_CONSTRAINEDPATH, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LS_CO_TU_TUNNEL, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LS_CO_TU_TU_CONFIG, TunnelConfigWriter(underlayAccess)))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LS_CO_TU_TU_P2PTUNNELATTRIBUTES, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_MP_LS_CO_TU_TU_P2_CONFIG, NoopWriter()))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_MPLS, MplsBuilder::class.java)

        // RSVP
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_SIGNALINGPROTOCOLS, SignalingProtocolsBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_SI_RSVPTE, RsvpTeBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_SI_RS_INTERFACEATTRIBUTES, InterfaceAttributesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_MP_SI_RS_IN_INTERFACE, RsvpInterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_MP_SI_RS_IN_IN_CONFIG, RsvpInterfaceConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_SI_RS_IN_IN_SUBSCRIPTION, SubscriptionBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_SI_RS_IN_IN_SU_CONFIG, ConfigBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_MP_SI_RS_IN_IN_SU_CONFIG.augmentation(
            NiMplsRsvpIfSubscripAug::class.java), NiMplsRsvpIfSubscripAugReader(underlayAccess)))

        // TE
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_TEINTERFACEATTRIBUTES, TeInterfaceAttributesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_MP_TE_INTERFACE, TeInterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_MP_TE_IN_CONFIG, TeInterfaceConfigReader()))

        // Tunnel
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_LSPS, LspsBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_LS_CONSTRAINEDPATH, ConstrainedPathBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_LS_CO_TUNNELS, TunnelsBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_MP_LS_CO_TU_TUNNEL, TunnelReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_MP_LS_CO_TU_TU_CONFIG, TunnelConfigReader(underlayAccess)))
        rRegistry.addStructuralReader(IIDs.NE_NE_MP_LS_CO_TU_TU_P2PTUNNELATTRIBUTES,
            P2pTunnelAttributesBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_MP_LS_CO_TU_TU_P2_CONFIG,
            P2pAttributesConfigReader(underlayAccess)))
    }

    override fun toString(): String {
        return "Junos 17.3 MPLS translate unit"
    }
}