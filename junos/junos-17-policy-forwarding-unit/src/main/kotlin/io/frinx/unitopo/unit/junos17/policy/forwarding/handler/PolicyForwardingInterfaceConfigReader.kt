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

package io.frinx.unitopo.unit.junos17.policy.forwarding.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.juniper.rev171109.NiPfIfJuniperAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.juniper.rev171109.NiPfIfJuniperAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.juniper.rev171109.juniper.pf._interface.extension.config.ClassifiersBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.juniper.rev171109.juniper.pf._interface.extension.config.classifiers.ExpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.juniper.rev171109.juniper.pf._interface.extension.config.classifiers.InetPrecedenceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.Interface as OcInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.cos_interfaces_type.unit.Classifiers
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper._class.of.service.options.interfaces.Interface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class PolicyForwardingInterfaceConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>, builder: ConfigBuilder, p2: ReadContext) {
        val ifcName = id.firstKeyOf(OcInterface::class.java).interfaceId.value
        val iface = PolicyForwardingInterfaceReader.readSpecificInterface(underlayAccess, ifcName)
        iface?.let {
            val niPfIfJuniperAugBuilder = NiPfIfJuniperAugBuilder()
            it.schedulerMap?.let {
                niPfIfJuniperAugBuilder.schedulerMap = it
            }
            val cBuilder = ClassifiersBuilder()
            filterUnit(it).forEach {
                it.inetPrecedence?.let {
                    cBuilder.inetPrecedence = InetPrecedenceBuilder().setName(it.classifierName?.string).build()
                }
                it.exp?.let {
                    cBuilder.exp = ExpBuilder().setName(it.classifierName?.string).build()
                }
            }
            niPfIfJuniperAugBuilder.classifiers = cBuilder.build()
            builder.addAugmentation(NiPfIfJuniperAug::class.java, niPfIfJuniperAugBuilder.build())
            builder.setInterfaceId(InterfaceId(it.name))
        }
    }

    override fun merge(p0: Builder<out DataObject>, p1: Config) {
        (p0 as InterfaceBuilder).config = p1
    }

    override fun getBuilder(p0: InstanceIdentifier<Config>): ConfigBuilder = ConfigBuilder()

    companion object {

        fun filterUnit(iface: Interface): List<Classifiers> =
            iface.unit.orEmpty().filter { it.classifiers != null }.map { it.classifiers }.toList()
    }
}