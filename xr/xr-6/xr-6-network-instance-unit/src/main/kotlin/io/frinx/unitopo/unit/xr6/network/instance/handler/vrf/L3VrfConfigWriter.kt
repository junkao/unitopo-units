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

package io.frinx.unitopo.unit.xr6.network.instance.handler.vrf

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.VrfAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.VrfSubAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.af.table.AfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.af.table.afs.AfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.af.table.afs.AfKey
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

class L3VrfConfigWriter(private val underlayAccess: UnderlayAccess) : CompositeWriter.Child<Config> {

    override fun deleteCurrentAttributesWResult(iid: IID<Config>, dataBefore: Config, wtx: WriteContext): Boolean {
        if (dataBefore.type != L3VRF::class.java) {
            return false
        }

        val vrfIid = getVrfIdentifier(dataBefore.name)
        underlayAccess.delete(vrfIid)
        return true
    }

    override fun updateCurrentAttributesWResult(
        id: org.opendaylight.yangtools.yang.binding.InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ): Boolean {
        if (dataBefore.type != L3VRF::class.java) {
            return false
        }

        val vrfIid = getVrfIdentifier(dataAfter.name)
        val vrfBuilder = underlayAccess.read(vrfIid).checkedGet()
                .or(EMPTY_VRF)
                .let { VrfBuilder(it) }

        val (_, vrf) = getVrfData(dataAfter, vrfBuilder)
        underlayAccess.put(vrfIid, vrf)
        return true
    }

    override fun writeCurrentAttributesWResult(iid: IID<Config>, dataAfter: Config, wtx: WriteContext): Boolean {
        if (dataAfter.type != L3VRF::class.java) {
            return false
        }

        val (vrfIid, vrf) = getVrfData(dataAfter, VrfBuilder())
        underlayAccess.merge(vrfIid, vrf)
        return true
    }

    private fun getVrfData(data: Config, vrfBuilder: VrfBuilder): Pair<IID<Vrf>, Vrf> {
        val vrfIid = getVrfIdentifier(data.name)

        val vrf = vrfBuilder
                .setKey(VrfKey(CiscoIosXrString(data.name)))
                .setVrfName(CiscoIosXrString(data.name))
                .setCreate(true)

        vrf.afs =
                AfsBuilder()
                        .setAf(data.enabledAddressFamilies.orEmpty()
                                .mapNotNull { it.toUnderlay() }
                                // Reuse existing AF configuration
                                .map { vrfBuilder
                                            .afs?.af.orEmpty()
                                            .find { existIt -> existIt.afName == it &&
                                                existIt.safName == VrfSubAddressFamily.Unicast &&
                                                existIt.topologyName == TOPO_NAME
                                            }
                                            ?: AfBuilder().setAfName(it).build() }
                                .map { AfBuilder(it) }
                                .map { it.setCreate(true)
                                            .setSafName(VrfSubAddressFamily.Unicast)
                                            .setTopologyName(TOPO_NAME)
                                            .setKey(AfKey(it.afName, it.safName, it.topologyName))
                                            .build() })
                        .build()

        return Pair(vrfIid, vrf.build())
    }

    companion object {
        private val EMPTY_VRF = VrfBuilder().build()
        private val TOPO_NAME = CiscoIosXrString("default")

        fun getVrfIdentifier(vrfName: String): IID<Vrf> {
            return IID.create(Vrfs::class.java)
                    .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName)))
        }

        fun Class<out ADDRESSFAMILY>.toUnderlay(): VrfAddressFamily? {
            return when (this) {
                IPV4::class.java -> VrfAddressFamily.Ipv4
                IPV6::class.java -> VrfAddressFamily.Ipv6
                else -> null
            }
        }
    }
}