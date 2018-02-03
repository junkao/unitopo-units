/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.ospf.common.OspfWriter
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.max.metric.MaxMetric
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.max.metric.max.metric.MaxMetricOnStartup
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.max.metric.max.metric.MaxMetricOnStartupBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.process.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.cisco.rev171124.MAXMETRICSUMMARYLSA
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.MAXMETRICINCLUDESTUB
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.MAXMETRICINCLUDETYPE2EXTERNAL
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.timers.max.metric.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class MaxMetricConfigWriter(private val underlayAccess: UnderlayAccess) : OspfWriter<Config> {

    override fun updateCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        val (underlayId, underlayData) = getData(id, dataAfter)

        try {
            underlayAccess.merge(underlayId, underlayData)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributesForType(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val (processIid, vrfName) = GlobalConfigWriter.getIdentifiers(id)
        val metricIid = getInterfaceIdentifier(processIid, vrfName)

        try {
            underlayAccess.delete(metricIid)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(id, e)
        }
    }



    companion object {
        public fun getInterfaceIdentifier(processIid: IID<Process>, vrfName: String): IID<MaxMetricOnStartup> {
            return processIid.let {
                if (GlobalConfigWriter.DEFAULT_VRF.equals(vrfName)) {
                    it.child(DefaultVrf::class.java)
                            .child(MaxMetric::class.java)
                } else {
                    it.child(Vrfs::class.java).child(Vrf::class.java,
                            VrfKey(CiscoIosXrString(vrfName)))
                            .child(MaxMetric::class.java)
                }
            }.child(MaxMetricOnStartup::class.java)
        }

        public fun getData(id: IID<Config>, data: Config): Pair<IID<MaxMetricOnStartup>, MaxMetricOnStartup> {
            val (processIid, vrfName) = GlobalConfigWriter.getIdentifiers(id)
            val metricIid = getInterfaceIdentifier(processIid, vrfName)
            val includeStub = data.include?.any {it.equals(MAXMETRICINCLUDESTUB::class.java)}
            val includeExternal = data.include?.any {it.equals(MAXMETRICINCLUDETYPE2EXTERNAL::class.java)}
            val includeSumLsa = data.include?.any {it.equals(MAXMETRICSUMMARYLSA::class.java)}

            val metric = MaxMetricOnStartupBuilder()
                    .setIncludeStub(includeStub)
                    .setExternalLsa(includeExternal)
                    .setSummaryLsa(includeSumLsa)

            return Pair(metricIid, metric.build())
        }
    }
}