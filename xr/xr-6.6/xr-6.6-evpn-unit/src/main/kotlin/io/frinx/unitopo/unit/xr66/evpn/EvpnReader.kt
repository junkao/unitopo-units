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

package io.frinx.unitopo.unit.xr7.evpn

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.Configuration1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.top.Evpn
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.top.EvpnBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.top.evpn.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.Evpn as NativeEvpn
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Evpn, EvpnBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Evpn>) = EvpnBuilder()

    override fun readCurrentAttributes(
        id: IID<Evpn>,
        builder: EvpnBuilder,
        readContext: ReadContext
    ) {
        val NATIVE_EVPN = IID.create(NativeEvpn::class.java)
        underlayAccess.read(NATIVE_EVPN, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()
            ?.let {
                builder.setConfig(ConfigBuilder().apply {
                    this.isEnabled = true
                }.build())
            }
    }

    override fun merge(builder: Builder<out DataObject>, config: Evpn) {
        (builder as Configuration1Builder).evpn = config
    }
}