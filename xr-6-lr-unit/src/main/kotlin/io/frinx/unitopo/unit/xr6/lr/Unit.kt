/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lr

import com.google.common.collect.Sets
import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl .read.GenericListReader
import io.fd.honeycomb.translate.impl.read.GenericReader
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.lr.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.xr6.lr.handler.StaticRouteReader
import io.frinx.unitopo.unit.xr6.lr.handler.NextHopReader
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev161222._interface.ref.InterfaceRefBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top.StaticRoutesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.Static
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.NextHopsBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHop
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHopBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHopKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.Config
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.State
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.`$YangModuleInfoImpl` as OpenconfigLocalRoutingYangModule
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.`$YangModuleInfoImpl` as UnderlayLocalRoutingYangModule


class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        if (reg != null) {
            reg!!.close()
        }
    }

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(OpenconfigLocalRoutingYangModule.getInstance())

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(UnderlayLocalRoutingYangModule.getInstance())

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<out DataObject, out DataObject>>()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder, wRegistry: ModifiableWriterRegistryBuilder,
                                 access: UnderlayAccess) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, access: UnderlayAccess) {
        // no-op
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, access: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_STATICROUTES, StaticRoutesBuilder::class.java)
        rRegistry.subtreeAdd(Sets.newHashSet(
                InstanceIdentifier.create(Static::class.java).child(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.Config::class.java)
                , InstanceIdentifier.create(Static::class.java).child(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.State::class.java)),
                GenericListReader<Static, StaticKey, StaticBuilder>(IIDs.NE_NE_PR_PR_ST_STATIC, StaticRouteReader(access)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_ST_ST_NEXTHOPS, NextHopsBuilder::class.java)
        rRegistry.subtreeAdd(Sets.newHashSet(
                InstanceIdentifier.create(NextHop::class.java).child(Config::class.java),
                InstanceIdentifier.create(NextHop::class.java).child(State::class.java)),
                GenericListReader<NextHop, NextHopKey, NextHopBuilder>(IIDs.NE_NE_PR_PR_ST_ST_NE_NEXTHOP, NextHopReader(access)))
        rRegistry.addStructuralReader(IIDs.NE_NE_PR_PR_ST_ST_NE_NE_INTERFACEREF, InterfaceRefBuilder::class.java)
        rRegistry.add(GenericReader(IIDs.NE_NE_PR_PR_ST_ST_NE_NE_IN_CONFIG, InterfaceConfigReader(access)))
    }

    override fun toString() = "XR 6 (2015-07-30) Local Routes translate unit"
}
