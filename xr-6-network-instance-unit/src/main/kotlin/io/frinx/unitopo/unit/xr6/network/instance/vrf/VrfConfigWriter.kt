/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.network.instance.vrf

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.Vrfs;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.VrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L3VRF
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class VrfConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun deleteCurrentAttributes(iid: IID<Config>, dataBefore: Config, wtc: WriteContext) {

        if (dataBefore.type != L3VRF::class.java) {
            return
        }

        if (dataBefore.name == NetworInstance.DEFAULT_NETWORK_NAME)
            return

        val vrfIid = getVrfIdentifier(dataBefore.name)

        try {
            underlayAccess.delete(vrfIid)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(vrfIid, e)
        }

    }

    override fun writeCurrentAttributes(iid: IID<Config>, dataAfter: Config, wtc: WriteContext) {
        if (dataAfter.type != L3VRF::class.java) {
            return
        }

        if (dataAfter.name == NetworInstance.DEFAULT_NETWORK_NAME)
            return

        val (vrfIid, vrf) = getVrfData(dataAfter)

        try {
            underlayAccess.merge(vrfIid, vrf)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(vrfIid, e)
        }
    }

    private fun getVrfData(data: Config): Pair<IID<Vrf>, Vrf> {
        val vrfIid = getVrfIdentifier(data.name)

        val vrf = VrfBuilder()
                .setKey(VrfKey(CiscoIosXrString(data.name)))
                .setCreate(data.isEnabled)

        return Pair(vrfIid, vrf.build())
    }

    companion object {
        public fun getVrfIdentifier(vrfName: String): IID<Vrf> {
            return IID.create(Vrfs::class.java)
                    .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName)))
        }
    }
}
