/*
 * Copyright © 2019 Frinx and others.
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

package io.frinx.unitopo.unit.xr66.bgp.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.As
import io.frinx.unitopo.unit.xr66.bgp.IID
import io.frinx.unitopo.unit.xr66.bgp.UnderlayBgp
import io.frinx.unitopo.unit.xr66.bgp.UnderlayBgpBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.InstanceBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.InstanceAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.FourByteAsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.Global
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev180615.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev170626.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.Bgp

open class GlobalConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
            val bgpBuilder = underlayAccess.read(XR_BGP_ID)
                    .checkedGet()
                    .or({ XR_EMPTY_BGP })
                    .let { UnderlayBgpBuilder(it) }

            renderGlobalData(bgpBuilder, dataAfter)
            underlayAccess.put(XR_BGP_ID, bgpBuilder.build())
    }

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, wc: WriteContext) {
            val bgpBuilder = UnderlayBgpBuilder()
            renderGlobalData(bgpBuilder, dataAfter)
            underlayAccess.put(XR_BGP_ID, bgpBuilder.build())
    }

    override fun deleteCurrentAttributes(iid: IID<Config>, dataBefore: Config, wc: WriteContext) {
            underlayAccess.delete(XR_BGP_ID)
    }

    companion object {
        val NAME = "default"
        val XR_BGP_INSTANCE_NAME = CiscoIosXrString(NAME)
        val XR_BGP_ID = IID.create(UnderlayBgp::class.java)
        private val XR_EMPTY_BGP = UnderlayBgpBuilder().build()

        private fun renderGlobalData(bgpBuilder: UnderlayBgpBuilder, dataAfter: Config) {
            val (as1, as2) = As.asToDotNotation(dataAfter.`as`)

            // Reuse existing fields for four byte as container
            val fourByteAsBuilder = bgpBuilder.instance.orEmpty().firstOrNull()
                    ?.instanceAs.orEmpty().firstOrNull()
                    ?.fourByteAs.orEmpty().firstOrNull()
                    ?.let {
                        FourByteAsBuilder(it)
                    } ?: FourByteAsBuilder()

            // Reuse existing fields for global container
            val globalBuilder = bgpBuilder.instance.orEmpty().firstOrNull()
                    ?.instanceAs.orEmpty().firstOrNull()
                    ?.fourByteAs.orEmpty().firstOrNull()
                    ?.defaultVrf
                    ?.global
                    ?.let {
                        GlobalBuilder(it)
                    } ?: GlobalBuilder()

            bgpBuilder
                    .setInstance(listOf(InstanceBuilder()
                            .setInstanceName(XR_BGP_INSTANCE_NAME)
                            .setInstanceAs(listOf(InstanceAsBuilder()
                                    .setAs(BgpAsRange(as1))
                                    .setFourByteAs(listOf(fourByteAsBuilder
                                            .setBgpRunning(true)
                                            .setAs(BgpAsRange(as2))
                                            .setDefaultVrf(DefaultVrfBuilder()
                                                    .setGlobal(dataAfter.getGlobal(globalBuilder))
                                                    .build())
                                            .build()))
                                    .build()))
                            .build()))
                    .build()
        }
    }
}

private fun Config.getGlobal(globalBuilder: GlobalBuilder): Global {
    return globalBuilder
            // optional
            .build()
}

/**
 * Collect all afi safi referenced in this instance
 */
fun Bgp.getAfiSafis(): Set<AfiSafi> {
    val global = this
            .global
            ?.afiSafis
            ?.afiSafi.orEmpty()
            .map { AfiSafiBuilder().setAfiSafiName(it.afiSafiName).build() }
            .toSet()
            .toMutableSet()

    return global
}