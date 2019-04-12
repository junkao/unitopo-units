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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.IsisConfigurableLevels
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisIfConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisIfConfAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.CircuitType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.LevelType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._interface.group.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._interface.group.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.Interface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class IsisInterfaceConfigReader(private val access: UnderlayAccess) :
    ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun readCurrentAttributes(id: IID<Config>, config: ConfigBuilder, readContext: ReadContext) {
        val protKey = id.firstKeyOf(Protocol::class.java)
        val ifaceId = id.firstKeyOf(Interface::class.java).interfaceId

        IsisInterfaceReader.getInterfaces(access, protKey)
                ?.`interface`.orEmpty()
                .find { it.interfaceName.value == ifaceId.value }
                ?.let {
                    config.interfaceId = ifaceId
                    it.isPointToPoint?.let {
                        if (it) {
                            config.circuitType = CircuitType.POINTTOPOINT
                        }
                    }
                    it.circuitType?.let {
                        val isisIfConfAugBuilder = IsisIfConfAugBuilder()
                        isisIfConfAugBuilder.fromUnderlay(it)
                        if (!isisIfConfAugBuilder.isEmpty()) {
                            config.addAugmentation(IsisIfConfAug::class.java, isisIfConfAugBuilder.build())
                        }
                    }
                }
    }

    companion object {
        private fun IsisIfConfAugBuilder.fromUnderlay(level: IsisConfigurableLevels) {
            when (level) {
                IsisConfigurableLevels.Level1 -> {
                    levelCapability = LevelType.LEVEL1
                }
                IsisConfigurableLevels.Level1And2 -> {
                    levelCapability = LevelType.LEVEL12
                }
                IsisConfigurableLevels.Level2 -> {
                    levelCapability = LevelType.LEVEL2
                }
                else -> {
                    levelCapability = null
                }
            }
        }

        private fun IsisIfConfAugBuilder.isEmpty() = levelCapability == null
    }
}