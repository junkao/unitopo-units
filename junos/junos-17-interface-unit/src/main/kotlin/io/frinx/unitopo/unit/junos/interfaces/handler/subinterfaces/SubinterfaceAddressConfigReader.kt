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

package io.frinx.unitopo.unit.junos.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MDSalReadFailed
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.Address as JunosInterfaceUnitAddress
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class SubinterfaceAddressConfigReader(private val underlayAccess: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    @Throws(ReadFailedException::class)
    override fun readCurrentAttributes(
        instanceIdentifier: IID<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        try {
            val name = instanceIdentifier.firstKeyOf(Interface::class.java).name
            val unitId = instanceIdentifier.firstKeyOf(Subinterface::class.java).index
            val addressKey = AddressKey(instanceIdentifier.firstKeyOf(Address::class.java).ip)

            InterfaceReader.readUnitAddress(underlayAccess, name, unitId, addressKey,
                { configBuilder.fromUnderlay(it) })
        } catch (e: MDSalReadFailed) {
            throw ReadFailedException(instanceIdentifier, e)
        }
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as AddressBuilder).config = config
    }
}

internal fun ConfigBuilder.fromUnderlay(junosUnitAddress: JunosInterfaceUnitAddress) {
    ip = resolveIpv4Address(junosUnitAddress.name)
    prefixLength = resolveIpv4Prefix(junosUnitAddress.name)
}