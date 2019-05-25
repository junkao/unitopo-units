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
package io.frinx.unitopo.unit.xr6.lr

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.init.Unit
import io.frinx.unitopo.unit.xr6.lr.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.xr6.lr.handler.NextHopReader
import io.frinx.unitopo.unit.xr6.lr.handler.StaticConfigReader
import io.frinx.unitopo.unit.xr6.lr.handler.StaticRouteReader
import io.frinx.unitopo.unit.xr6.lr.handler.StaticStateReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHop
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.State
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.`$YangModuleInfoImpl` as UnderlayLocalRoutingYangModule
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.`$YangModuleInfoImpl` as OpenconfigLocalRoutingYangModule

class Unit(private val registry: TranslationUnitCollector) : Unit() {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        if (reg != null) {
            reg!!.close()
        }
    }

    override fun getYangSchemas() = setOf(OpenconfigLocalRoutingYangModule.getInstance())

    override fun getUnderlayYangSchemas() = setOf(UnderlayLocalRoutingYangModule.getInstance())

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<out DataObject, out DataObject>>()

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, access: UnderlayAccess) {
        // no-op
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, access: UnderlayAccess) {
        rRegistry.add(IIDs.NE_NE_PR_PR_ST_STATIC, StaticRouteReader(access))
        rRegistry.add(IIDs.NE_NE_PR_PR_ST_ST_STATE, StaticStateReader())
        rRegistry.add(IIDs.NE_NE_PR_PR_ST_ST_CONFIG, StaticConfigReader())
        // FIXME split the next hop reader
        // this way it mixes config and oper data
        rRegistry.subtreeAdd(IIDs.NE_NE_PR_PR_ST_ST_NE_NEXTHOP, NextHopReader(access),
                setOf(InstanceIdentifier.create(NextHop::class.java).child(Config::class.java),
                InstanceIdentifier.create(NextHop::class.java).child(State::class.java)))
        // FIXME after next hop reader is split, mark this config instead of oper
        rRegistry.add(IIDs.NE_NE_PR_PR_ST_ST_NE_NE_IN_CONFIG, InterfaceConfigReader(access))
    }

    override fun toString() = "XR 6 (2015-07-30) Local Routes translate unit"
}