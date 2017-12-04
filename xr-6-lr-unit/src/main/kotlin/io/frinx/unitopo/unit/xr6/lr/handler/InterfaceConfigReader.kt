/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lr.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.lr.common.LrReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.route.vrf.route.VrfNextHopTable
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.ref.InterfaceRefBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.ref._interface.ref.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.ref._interface.ref.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.Static
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHop
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHopKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceConfigReader(private val access: UnderlayAccess) : LrReader.LrOperReader<Config, ConfigBuilder> {

    override fun readCurrentAttributesForType(id: InstanceIdentifier<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        val key = id.firstKeyOf(NextHop::class.java)
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        val routeKey = id.firstKeyOf(Static::class.java)
        StaticRouteReader.getAddressFamily(access, vrfName)?.let {
            NextHopReader.parseNextHopTable(it, routeKey)?.let {
                parseInterface(it, builder, key)
            }
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as InterfaceRefBuilder).config = readValue
    }

    override fun getBuilder(p0: InstanceIdentifier<Config>) = ConfigBuilder()

    companion object {

        @VisibleForTesting
        fun parseInterface(table: VrfNextHopTable, builder: ConfigBuilder, key: NextHopKey) {
            table.vrfNextHopInterfaceName.orEmpty().firstOrNull { it.interfaceName.value == key.toString() }?.let {
                builder.`interface` = it.interfaceName.value
            }
            table.vrfNextHopInterfaceNameNextHopAddress.orEmpty().firstOrNull { it.nextHopAddress.createComplexKey(it.interfaceName.value) == key }?.let {
                builder.`interface` = it.interfaceName.value
            }
        }
    }
}