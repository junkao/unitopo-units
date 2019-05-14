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

package io.frinx.unitopo.unit.xr623.isis.handler.interfaces

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr623.isis.handler.IsisProtocolConfigWriter
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.IsisConfigurableLevels
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.Interfaces
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface.InterfaceAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface.InterfaceAfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface._interface.afs.InterfaceAfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces._interface._interface.afs._interface.af.InterfaceAfDataBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.metric.table.MetricsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.metric.table.metrics.MetricBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisInternalLevel
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisSubAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisIfAfConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisIfConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.AFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.CircuitType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.IPV6
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.LevelType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.MULTICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.SAFITYPE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.UNICAST
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._interface.group.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces.Interface as XrInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._if.global.afi.safi.list.af.Config as AfConfig
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class IsisInterfaceWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Interface> {
    override fun writeCurrentAttributes(id: IID<Interface>, dataAfter: Interface, wtx: WriteContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name

        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "IS-IS configuration should be set in default network: $vrfName"
        }

        if (dataAfter.config == null) {
            return
        }
        val (underlayId, builder) = getData(id, dataAfter)
        underlayAccess.safePut(underlayId, builder)
    }

    override fun updateCurrentAttributes(
        id: IID<Interface>,
        dataBefore: Interface,
        dataAfter: Interface,
        writeContext: WriteContext
    ) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name

        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "IS-IS configuration should be set in default network: $vrfName"
        }

        if (dataAfter.config == null && dataBefore.config == null) {
            return
        }

        if (dataAfter.config == null) {
            deleteCurrentAttributes(id, dataBefore, writeContext)
            return
        }

        if (dataBefore.config == null) {
            writeCurrentAttributes(id, dataAfter, writeContext)
            return
        }

        val (underlayId, underlayBefore) = getData(id, dataBefore)
        val (_, underlayAfter) = getData(id, dataAfter)
        underlayAccess.safeMerge(underlayId, underlayBefore, underlayId, underlayAfter)
    }

    override fun deleteCurrentAttributes(id: IID<Interface>, dataBefore: Interface, wtx: WriteContext) {
        val instanceName = id.firstKeyOf(Protocol::class.java).name!!
        val interfaceId = id.firstKeyOf(Interface::class.java).interfaceId.value
        val underlayId = getUnderlayId(instanceName, interfaceId)

        if (dataBefore.config == null) {
            return
        }
        underlayAccess.delete(underlayId)
    }

    private fun getData(id: IID<Interface>, data: Interface): Pair<IID<XrInterface>, XrInterface> {
        val instanceName = id.firstKeyOf(Protocol::class.java)
        val interfaceId = id.firstKeyOf(Interface::class.java).interfaceId.value

        val underlayId = getUnderlayId(instanceName.name, interfaceId)

        val builder = InterfaceBuilder()

        builder
            .setInterfaceName(InterfaceName(interfaceId))
            .setCircuitType(getUnderlayLevel(data.config?.getAugmentation(IsisIfConfAug::class.java)?.levelCapability))
            .setPointToPoint(
                    when (data.config?.circuitType) {
                        CircuitType.POINTTOPOINT -> true
                        else -> null
                    })
            .setRunning(true)
            .setInterfaceAfs(getInterfaceAfsData(data.afiSafi))

        return Pair(underlayId, builder.build())
    }

    private fun getInterfaceAfsData(afiSafi: AfiSafi?): InterfaceAfs {
        val builder = InterfaceAfsBuilder()

        afiSafi?.af.orEmpty()
            .filter { it.config?.getAugmentation(IsisIfAfConfAug::class.java)?.metric != null }
            .map {
                InterfaceAfBuilder()
                    .fromOpenConfig(it.config)
                    .build()
            }
            .let { builder.setInterfaceAf(it) }

        return builder.build()
    }

    private fun InterfaceAfBuilder.fromOpenConfig(data: AfConfig): InterfaceAfBuilder {
        afName = getUnderlayAfiType(data.afiName)
        safName = getUnderlaySafiType(data.safiName)

        val metric = data.getAugmentation(IsisIfAfConfAug::class.java).metric

        val metricBuilder = MetricBuilder()
            .setLevel(IsisInternalLevel.NotSet)
            .setMetric(metric)

        val metricsBuilder = MetricsBuilder()
            .setMetric(listOf(metricBuilder.build()))

        val interfaceAfDataBuilder = InterfaceAfDataBuilder()
            .setRunning(true)
            .setMetrics(metricsBuilder.build())

        interfaceAfData = interfaceAfDataBuilder.build()
        return this
    }

    companion object {
        fun getUnderlayId(instanceName: String, interfaceId: String): IID<XrInterface> {
            return IsisProtocolConfigWriter.getUnderlayId(instanceName)
                .child(Interfaces::class.java)
                .child(XrInterface::class.java, InterfaceKey(InterfaceName(interfaceId)))
        }

        fun getUnderlayLevel(leveltype: LevelType?): IsisConfigurableLevels? {
            return when (leveltype) {
                LevelType.LEVEL1 -> IsisConfigurableLevels.Level1
                LevelType.LEVEL12 -> IsisConfigurableLevels.Level1And2
                LevelType.LEVEL2 -> IsisConfigurableLevels.Level2
                else -> null
            }
        }

        fun getUnderlayAfiType(afName: Class<out AFITYPE>?): IsisAddressFamily? {
            return when (afName) {
                IPV4::class.java -> IsisAddressFamily.Ipv4
                IPV6::class.java -> IsisAddressFamily.Ipv6
                else -> null
            }
        }

        fun getUnderlaySafiType(safName: Class<out SAFITYPE>?): IsisSubAddressFamily? {
            return when (safName) {
                UNICAST::class.java -> IsisSubAddressFamily.Unicast
                MULTICAST::class.java -> IsisSubAddressFamily.Multicast
                else -> null
            }
        }
    }
}