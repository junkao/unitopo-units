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

package io.frinx.unitopo.unit.xr6.network.instance.vrf

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.topology.impl.data.UnderlayTxManager
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.VrfAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.VrfSubAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.af.table.AfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.af.table.afs.AfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.VrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L3VRF
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.ADDRESSFAMILY
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.IPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.types.rev170113.IPV6
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class VrfConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun deleteCurrentAttributes(iid: IID<Config>, dataBefore: Config, wtc: WriteContext) {

        if (dataBefore.type != L3VRF::class.java) {
            return
        }

        if (dataBefore.name == NetworInstance.DEFAULT_NETWORK_NAME)
            return

        val vrfIid = getVrfIdentifier(dataBefore.name)

        underlayAccess.delete(vrfIid)
    }

    private fun commitUnderlay() {
        val underlayTxManager = underlayAccess as UnderlayTxManager
        underlayTxManager.commitTransaction().get()
        underlayTxManager.refreshTransaction()
    }

    override fun writeCurrentAttributes(iid: IID<Config>, dataAfter: Config, wtc: WriteContext) {
        if (dataAfter.type != L3VRF::class.java) {
            return
        }

        if (dataAfter.name == NetworInstance.DEFAULT_NETWORK_NAME)
            return

        val (vrfIid, vrf) = getVrfData(dataAfter)
        underlayAccess.merge(vrfIid, vrf)
        // Need to commit, because creating and touching VRF in a single TX is not possible, VRF has to exist before
        // e.g. using it in BGP
        commitUnderlay()
    }

    private fun getVrfData(data: Config): Pair<IID<Vrf>, Vrf> {
        val vrfIid = getVrfIdentifier(data.name)

        val vrf = VrfBuilder()
                .setKey(VrfKey(CiscoIosXrString(data.name)))
                .setVrfName(CiscoIosXrString(data.name))
                .setCreate(true)

        vrf.afs =
                AfsBuilder()
                        .setAf(data.enabledAddressFamilies.orEmpty()
                                .mapNotNull { it.toUnderlay() }
                                .map { AfBuilder()
                                        .setCreate(true)
                                        .setAfName(it)
                                        .setSafName(VrfSubAddressFamily.Unicast)
                                        .setTopologyName(CiscoIosXrString("default"))
                                        .build() })
                        .build()

        return Pair(vrfIid, vrf.build())
    }

    companion object {
        public fun getVrfIdentifier(vrfName: String): IID<Vrf> {
            return IID.create(Vrfs::class.java)
                    .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName)))
        }
    }
}

private fun Class<out ADDRESSFAMILY>.toUnderlay(): VrfAddressFamily? {
    return when (this) {
        IPV4::class.java -> VrfAddressFamily.Ipv4
        IPV6::class.java -> VrfAddressFamily.Ipv6
        else -> null
    }
}
