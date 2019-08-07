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

package io.frinx.unitopo.unit.xr623.interfaces

import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.CommonUnit
import io.frinx.unitopo.unit.xr623.interfaces.handler.aggregate.AggregateConfigReader
import io.frinx.unitopo.unit.xr623.interfaces.handler.aggregate.AggregateConfigWriter
import io.frinx.unitopo.unit.xr623.interfaces.handler.aggregate.AggregateWriter
import io.frinx.unitopo.unit.xr623.interfaces.handler.aggregate.bfd.BfdConfigReader
import io.frinx.unitopo.unit.xr623.interfaces.handler.aggregate.bfd.BfdIpv6ConfigReader
import io.frinx.unitopo.unit.xr623.interfaces.handler.ethernet.EthernetConfigReader
import io.frinx.unitopo.unit.xr623.interfaces.handler.ethernet.EthernetConfigWriter
import io.frinx.unitopo.unit.xr623.interfaces.handler.holdtime.HoldTimeConfigReader
import io.frinx.unitopo.unit.xr623.interfaces.handler.holdtime.HoldTimeConfigWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.Aggregation
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import io.frinx.openconfig.openconfig.lacp.IIDs as LacpIIDs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.`$YangModuleInfoImpl` as UnderlayBundleMgrYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.`$YangModuleInfoImpl` as UnderlayL2EthYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.`$YangModuleInfoImpl` as BfdYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.lag.member.rev171109.`$YangModuleInfoImpl` as LacpLagMemberYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.`$YangModuleInfoImpl` as LacpYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.drivers.media.eth.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayDriversMediaYangInfo

class Xr623Unit(private val registry: TranslationUnitCollector) : CommonUnit(registry) {

    override fun provideSpecificWriters(
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        // if-ethernet
        wRegistry.addNoop(IIDs.IN_IN_AUG_INTERFACE1_ETHERNET)
        wRegistry.addNoop(IIDs.IN_IN_ET_CO_AUG_CONFIG1)
        wRegistry.subtreeAddAfter(setOf(
            RWUtils.cutIdFromStart(IIDs.INT_INT_ETH_CON_AUG_CONFIG1, IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG),
            RWUtils.cutIdFromStart(LacpIIDs.IN_IN_ET_CO_AUG_LACPETHCONFIGAUG, IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG)),
            GenericWriter(IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG, EthernetConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)

        // if-aggregation
        wRegistry.subtreeAddAfter(setOf(
            RWUtils.cutIdFromStart(IIDs.IN_IN_AG_CO_AUG_IFLAGAUG, InstanceIdentifier.create(Config::class.java))),
            GenericWriter(IIDs.IN_IN_AUG_INTERFACE1_AG_CONFIG, AggregateConfigWriter(underlayAccess)),
            IIDs.IN_IN_CONFIG)

        // if-aggregation / bfd
        wRegistry.subtreeAddAfter(IID_SUB_TREE_BFD,
            GenericWriter(IIDs.IN_IN_AUG_INTERFACE1_AGGREGATION, AggregateWriter(underlayAccess)),
            setOf(IIDs.IN_IN_CONFIG,
                IIDs.IN_IN_AUG_INTERFACE1))

        // hold-time(interface)
        wRegistry.addNoop(IIDs.IN_IN_HOLDTIME)
        wRegistry.addAfter(GenericWriter(IIDs.IN_IN_HO_CONFIG, HoldTimeConfigWriter(underlayAccess)),
                IIDs.IN_IN_CONFIG)
    }

    override fun provideSpecificReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        // if-ethernet
        rRegistry.subtreeAdd(setOf(
            RWUtils.cutIdFromStart(IIDs.INT_INT_ETH_CON_AUG_CONFIG1, IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG),
            RWUtils.cutIdFromStart(LacpIIDs.IN_IN_ET_CO_AUG_CONFIG1, IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG)),
            GenericConfigReader(IIDs.IN_IN_AUG_INTERFACE1_ET_CONFIG, EthernetConfigReader(underlayAccess)))

        // if-aggregation
        rRegistry.subtreeAdd(setOf(
            RWUtils.cutIdFromStart(IIDs.IN_IN_AG_CO_AUG_IFLAGAUG, InstanceIdentifier.create(Config::class.java))),
            GenericConfigReader(IIDs.IN_IN_AUG_INTERFACE1_AG_CONFIG, AggregateConfigReader(underlayAccess)))

        // if-aggregation / bfd
        rRegistry.add(
            GenericConfigReader(IIDs.IN_IN_AG_AUG_AGGREGATION1_BF_CONFIG, BfdConfigReader(underlayAccess)))
        rRegistry.add(
            GenericConfigReader(IIDs.INT_INT_AGG_AUG_AGGREGATION1_BFD_CONFIG, BfdIpv6ConfigReader(underlayAccess)))

        // hold-time(interface)
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_HO_CONFIG, HoldTimeConfigReader(underlayAccess)))
    }

    override fun getSpecificYangSchemas() = setOf(
        LacpYangInfo.getInstance(),
        LacpLagMemberYangInfo.getInstance(),
        BfdYangInfo.getInstance())

    override fun getSpecificUnderlayYangSchemas() = UNDERLAY_SCHEMAS

    override fun toString(): String = "XR 6.2.3 (2015-07-30) interface translate unit"

    companion object {
        private val UNDERLAY_SCHEMAS = setOf(
            UnderlayBundleMgrYangInfo.getInstance(),
            UnderlayL2EthYangInfo.getInstance(),
            UnderlayDriversMediaYangInfo.getInstance()
        )
        private val IID_FOR_CUT_AGGREGATION = InstanceIdentifier.create(Aggregation::class.java)
        private val IID_SUB_TREE_BFD = setOf(
            RWUtils.cutIdFromStart(IIDs.IN_IN_AG_AUG_IFLAGBFDAUG, IID_FOR_CUT_AGGREGATION),
            RWUtils.cutIdFromStart(IIDs.IN_IN_AG_AUG_IFLAGBFDAUG_BFD, IID_FOR_CUT_AGGREGATION),
            RWUtils.cutIdFromStart(IIDs.IN_IN_AG_AUG_IFLAGBFDAUG_BF_CONFIG, IID_FOR_CUT_AGGREGATION),
            RWUtils.cutIdFromStart(IIDs.IN_IN_AG_AUG_IFLAGBFDAUG_BFDIPV6, IID_FOR_CUT_AGGREGATION),
            RWUtils.cutIdFromStart(IIDs.INT_INT_AGG_AUG_IFLAGBFDAUG_BFD_CONFIG, IID_FOR_CUT_AGGREGATION)
        )
    }
}