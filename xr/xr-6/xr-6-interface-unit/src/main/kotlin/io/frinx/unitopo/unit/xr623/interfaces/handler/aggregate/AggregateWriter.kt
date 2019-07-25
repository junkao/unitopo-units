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

package io.frinx.unitopo.unit.xr623.interfaces.handler.aggregate

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.BfdMode
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.Bfd
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.BfdBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.bfd.AddressFamilyBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.bfd.address.family.Ipv4
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.bfd.address.family.Ipv4Builder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.bfd.address.family.Ipv6
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.bundlemgr.cfg.rev161216._interface.configurations._interface.configuration.bfd.address.family.Ipv6Builder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.Aggregation
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.IfLagBfdAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.bfd.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.bfd.rev171024.bfd.top.bfd.ipv6.Config as Ipv6Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class AggregateWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Aggregation> {

    override fun writeCurrentAttributes(id: IID<Aggregation>, dataAfter: Aggregation, writeContext: WriteContext) {
        val (underlayId, data) = getUnderlayBfdData(id, dataAfter)
        underlayAccess.put(underlayId, data)
    }

    override fun deleteCurrentAttributes(id: IID<Aggregation>, dataBefore: Aggregation, context: WriteContext) {
        val underlayId = getInterfaceId(id)
                .augmentation(InterfaceConfiguration1::class.java)
                .child(Bfd::class.java)
        underlayAccess.delete(underlayId)
    }

    override fun updateCurrentAttributes(
        id: IID<Aggregation>,
        dataBefore: Aggregation,
        dataAfter: Aggregation,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    private fun getInterfaceId(id: IID<Aggregation>): IID<InterfaceConfiguration> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)

        return InterfaceReader.IFC_CFGS.child(
                InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
    }

    private fun getUnderlayBfdData(
        id: IID<Aggregation>,
        data: Aggregation
    ): Pair<IID<Bfd>, Bfd> {
        val underlayId = getInterfaceId(id)
                .augmentation(InterfaceConfiguration1::class.java)
                .child(Bfd::class.java)

        val existingData = underlayAccess.read(underlayId).checkedGet().orNull()

        var afiBuilder = when (existingData?.addressFamily) {
            null -> AddressFamilyBuilder()
            else -> AddressFamilyBuilder(existingData.addressFamily)
        }

        var ipv4Builder = when (existingData?.addressFamily?.ipv4) {
            null -> Ipv4Builder()
            else -> Ipv4Builder(existingData.addressFamily.ipv4)
        }

        var ipv6Builder = when (existingData?.addressFamily?.ipv6) {
            null -> Ipv6Builder()
            else -> Ipv6Builder(existingData.addressFamily.ipv6)
        }

        val aggregation1 = data.getAugmentation(IfLagBfdAug::class.java)
        val ipv4Config = aggregation1?.bfd?.config
        val ipv6Config = aggregation1?.bfdIpv6?.config
        if (aggregation1 == null || ipv4Config == null && ipv6Config == null) {
            return Pair(underlayId, BfdBuilder().build())
        }
        val bfd = when (existingData) {
            null -> BfdBuilder()
            else -> BfdBuilder(existingData)
        }.apply {
            mode = BfdMode.Ietf
            addressFamily = afiBuilder.apply {
                ipv4 = when (ipv4Config) {
                    null -> null
                    else -> ipv4Builder.fromOpenConfig(ipv4Config)
                }
                ipv6 = when (ipv6Config) {
                    null -> null
                    else -> ipv6Builder.fromOpenConfig(ipv6Config)
                }
            }.build()
        }.build()
        return Pair(underlayId, bfd)
    }
}

fun Ipv4Builder.fromOpenConfig(data: Config): Ipv4 {
    data.destinationAddress?.let {
        destinationAddress = Ipv4AddressNoZone(data.destinationAddress.value)
    }
    isFastDetect = true
    interval = data.minInterval
    detectionMultiplier = data.multiplier
    return build()
}

fun Ipv6Builder.fromOpenConfig(data: Ipv6Config): Ipv6 {
    ipv6DestinationAddress = data.destinationAddress?.value
    isIpv6FastDetect = true
    ipv6Interval = data.minInterval
    return build()
}