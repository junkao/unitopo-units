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

package io.frinx.unitopo.unit.junos.interfaces.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Config1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Config1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ethernet.rev161222.ethernet.top.ethernet.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MDSalReadFailed
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.gigether.options.Ieee8023ad as JunosGigEthIeee8023ad
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class InterfaceIfAggregateConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config1, Config1Builder> {
    override fun getBuilder(iid: IID<Config1>): Config1Builder {
        return Config1Builder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(iid: IID<Config1>, builder: Config1Builder, context: ReadContext) {
        try {
            val name = iid.firstKeyOf(Interface::class.java).name
            InterfaceReader.readEthernetCfg(underlayAccess, name, { builder.fromUnderlay(it) })
        } catch (e: MDSalReadFailed) {
            throw ReadFailedException(iid, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, data: Config1) {
        (builder as ConfigBuilder).addAugmentation(Config1::class.java, data)
    }
}

internal fun Config1Builder.fromUnderlay(underlay: JunosGigEthIeee8023ad?) {
    aggregateId = when (underlay?.bundle?.interfaceDevice?.value) {
        null -> null
        else -> underlay.bundle?.interfaceDevice?.value!!
    }
}