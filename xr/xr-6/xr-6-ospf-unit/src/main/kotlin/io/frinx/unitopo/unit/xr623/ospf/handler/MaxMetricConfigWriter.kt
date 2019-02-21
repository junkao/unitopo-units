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

package io.frinx.unitopo.unit.xr623.ospf.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.handlers.ospf.OspfWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.max.metric.MaxMetric
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.max.metric.max.metric.MaxMetricOnStartup
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.max.metric.max.metric.MaxMetricOnStartupBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.process.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.process.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.process.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev170102.ospf.processes.process.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.cisco.rev171124.MAXMETRICSUMMARYLSA
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.MAXMETRICINCLUDESTUB
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.MAXMETRICINCLUDETYPE2EXTERNAL
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.max.metric.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class MaxMetricConfigWriter(private val underlayAccess: UnderlayAccess) : OspfWriter<Config> {

    override fun updateCurrentAttributesForType(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (underlayId, underlayData) = getData(id, dataAfter)
        underlayAccess.put(underlayId, underlayData)
    }

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        val (underlayId, underlayData) = getData(id, dataAfter)
        underlayAccess.merge(underlayId, underlayData)
    }

    override fun deleteCurrentAttributesForType(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val (processIid, vrfName) = AreaConfigWriter.getIdentifiers(id)
        val metricIid = getInterfaceIdentifier(processIid, vrfName)

        underlayAccess.delete(metricIid)
    }

    companion object {
        fun getInterfaceIdentifier(processIid: IID<Process>, vrfName: String): IID<MaxMetricOnStartup> {
            return processIid.let {
                if (NetworInstance.DEFAULT_NETWORK_NAME == vrfName) {
                    it.child(DefaultVrf::class.java)
                            .child(MaxMetric::class.java)
                } else {
                    it.child(Vrfs::class.java).child(Vrf::class.java,
                            VrfKey(CiscoIosXrString(vrfName)))
                            .child(MaxMetric::class.java)
                }
            }.child(MaxMetricOnStartup::class.java)
        }

        fun getData(id: IID<Config>, data: Config): Pair<IID<MaxMetricOnStartup>, MaxMetricOnStartup> {
            val (processIid, vrfName) = AreaConfigWriter.getIdentifiers(id)
            val metricIid = getInterfaceIdentifier(processIid, vrfName)
            val includeStub = data.include?.any { it == MAXMETRICINCLUDESTUB::class.java }
            val includeExternal = data.include?.any { it == MAXMETRICINCLUDETYPE2EXTERNAL::class.java }
            val includeSumLsa = data.include?.any { it == MAXMETRICSUMMARYLSA::class.java }

            val metric = MaxMetricOnStartupBuilder()
                    .setIncludeStub(includeStub)
                    .setExternalLsa(includeExternal)
                    .setSummaryLsa(includeSumLsa)

            return Pair(metricIid, metric.build())
        }
    }
}