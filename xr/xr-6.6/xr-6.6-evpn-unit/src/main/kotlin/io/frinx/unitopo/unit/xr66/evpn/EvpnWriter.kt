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

package io.frinx.unitopo.unit.xr66.evpn

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.evpn.rev181112.evpn.top.Evpn
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.Evpn as NativeEvpn
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.EvpnBuilder as NativeEvpnBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class EvpnWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Evpn> {
    val NATIVE_EVPN = IID.create(NativeEvpn::class.java)

    override fun writeCurrentAttributes(id: IID<Evpn>, dataAfter: Evpn, writeContext: WriteContext) {
        val builder = NativeEvpnBuilder().apply {
            this.isEnable = dataAfter.config.isEnabled
        }
        underlayAccess.put(NATIVE_EVPN, builder.build())
    }

    override fun deleteCurrentAttributes(
        id: IID<Evpn>,
        dataBefore: Evpn,
        writeContext: WriteContext
    ) {
        underlayAccess.delete(NATIVE_EVPN)
    }

    override fun updateCurrentAttributes(
        id: IID<Evpn>,
        dataBefore: Evpn,
        dataAfter: Evpn,
        writeContext: WriteContext
    ) {
        if (dataAfter.config.isEnabled) {
            underlayAccess.merge(NATIVE_EVPN, NativeEvpnBuilder().build())
        } else {
            underlayAccess.delete(NATIVE_EVPN)
        }
    }
}