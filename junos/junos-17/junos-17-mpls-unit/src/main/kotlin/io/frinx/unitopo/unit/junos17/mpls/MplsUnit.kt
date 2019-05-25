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
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
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
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.addNoop(IIDs.NE_NE_MPLS)

        // RSVP
        wRegistry.addNoop(IIDs.NE_NE_MP_SI_RSVPTE)
        wRegistry.addNoop(IIDs.NE_NE_MP_SI_RS_IN_INTERFACE)
        wRegistry.add(IIDs.NE_NE_MP_SI_RS_IN_IN_CONFIG, RsvpInterfaceConfigWriter(underlayAccess))
        wRegistry.addNoop(IIDs.NE_NE_MP_SI_RS_IN_IN_SU_CONFIG)
        wRegistry.addAfter(IIDs.NE_NE_MP_SI_RS_IN_IN_SU_CO_AUG_NIMPLSRSVPIFSUBSCRIPAUG,
            NiMplsRsvpIfSubscripAugWriter(underlayAccess), IIDs.NE_NE_MP_SI_RS_IN_IN_CONFIG)

        // TE
        wRegistry.addNoop(IIDs.NE_NE_MP_TE_INTERFACE)
        wRegistry.add(IIDs.NE_NE_MP_TE_IN_CONFIG, TeInterfaceConfigWriter(underlayAccess))

        // Tunnel
        wRegistry.addNoop(IIDs.NE_NE_MP_LSPS)
        wRegistry.addNoop(IIDs.NE_NE_MP_LS_CONSTRAINEDPATH)
        wRegistry.addNoop(IIDs.NE_NE_MP_LS_CO_TU_TUNNEL)
        wRegistry.add(IIDs.NE_NE_MP_LS_CO_TU_TU_CONFIG, TunnelConfigWriter(underlayAccess))
        wRegistry.addNoop(IIDs.NE_NE_MP_LS_CO_TU_TU_P2PTUNNELATTRIBUTES)
        wRegistry.addNoop(IIDs.NE_NE_MP_LS_CO_TU_TU_P2_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        // RSVP
        rRegistry.add(IIDs.NE_NE_MP_SI_RS_IN_INTERFACE, RsvpInterfaceReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_MP_SI_RS_IN_IN_CONFIG, RsvpInterfaceConfigReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_MP_SI_RS_IN_IN_SU_CO_AUG_NIMPLSRSVPIFSUBSCRIPAUG,
            NiMplsRsvpIfSubscripAugReader(underlayAccess))

        // TE
        rRegistry.add(IIDs.NE_NE_MP_TE_INTERFACE, TeInterfaceReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_MP_TE_IN_CONFIG, TeInterfaceConfigReader())

        // Tunnel
        rRegistry.add(IIDs.NE_NE_MP_LS_CO_TU_TUNNEL, TunnelReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_MP_LS_CO_TU_TU_CONFIG, TunnelConfigReader(underlayAccess))
        rRegistry.add(IIDs.NE_NE_MP_LS_CO_TU_TU_P2_CONFIG,
            P2pAttributesConfigReader(underlayAccess))
    }

    override fun toString(): String {
        return "Junos 17.3 MPLS translate unit"
    }
}