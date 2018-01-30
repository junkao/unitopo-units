/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.interfaces.handler.lag.aggregate

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader.Companion.LAG_PREFIX
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171024.bfd.top.BfdBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171024.bfd.top.bfd.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171024.bfd.top.bfd.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MDSalReadFailed
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetection as JunosBfdLivenessDetection
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetectionBuilder as JunosBfdLivenessDetectionBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceAggregationBfdConfigReader(private val underlayAccess: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {
    override fun getBuilder(iid: IID<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(iid: IID<Config>, builder: ConfigBuilder, context: ReadContext) {
        try {
            val name = iid.firstKeyOf(Interface::class.java).name.removePrefix(LAG_PREFIX)
            InterfaceReader.readAggregationBfdCfg(underlayAccess, name, { builder.fromUnderlay(it) })
        } catch (e: MDSalReadFailed) {
            throw ReadFailedException(iid, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, data: Config) {
        (builder as BfdBuilder).config = data
    }
}

private fun ConfigBuilder.fromUnderlay(underlay: JunosBfdLivenessDetection?) {
    localAddress = IpAddress(Ipv4Address(underlay?.localAddress?.value))
    destinationAddress = IpAddress(Ipv4Address(underlay?.neighbor?.value))
    minInterval = underlay?.minimumInterval?.uint32
    multiplier = underlay?.multiplier?.uint32
}
