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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.instance.interfaces.Interface as XrInterface
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisIfConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.CircuitType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.types.rev181121.LevelType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis._interface.group.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.interfaces.Interface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class IsisInterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {
    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name

        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "IS-IS configuration should be set in default network: $vrfName"
        }

        val (underlayId, builder) = getData(id, dataAfter)
        underlayAccess.put(underlayId, builder)
    }

    override fun updateCurrentAttributes(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(iid, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributes(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val instanceName = id.firstKeyOf(Protocol::class.java).name!!
        val interfaceId = id.firstKeyOf(Interface::class.java).interfaceId.value
        val underlayId = getUnderlayId(instanceName, interfaceId)

        underlayAccess.delete(underlayId)
    }

    private fun getData(id: IID<Config>, dataAfter: Config): Pair<IID<XrInterface>, XrInterface> {
        val instanceName = id.firstKeyOf(Protocol::class.java)
        val interfaceId = id.firstKeyOf(Interface::class.java).interfaceId.value

        val underlayId = getUnderlayId(instanceName.name, interfaceId)
        val ifc = IsisInterfaceReader.getInterfaces(underlayAccess, instanceName)
                    ?.`interface`.orEmpty()
                    .find { it.interfaceName.value == interfaceId }
        val builder = when (ifc) {
            null -> InterfaceBuilder()
            else -> InterfaceBuilder(ifc)
        }

        builder
            .setInterfaceName(InterfaceName(interfaceId))
            .setCircuitType(getUnderlayLevel(dataAfter.getAugmentation(IsisIfConfAug::class.java)?.levelCapability))
            .setPointToPoint(
                    when (dataAfter.circuitType) {
                        CircuitType.POINTTOPOINT -> true
                        else -> null
                    })
            .setRunning(true)

        return Pair(underlayId, builder.build())
    }

    companion object {
        fun getUnderlayId(instanceName: String, interfaceId: String): IID<XrInterface> {
            return IsisProtocolConfigWriter.getUnderlayId(instanceName)
                .child(Interfaces::class.java)
                .child(XrInterface::class.java, InterfaceKey(InterfaceName(interfaceId)))
        }

        fun getUnderlayLevel(leveltype: LevelType?): IsisConfigurableLevels? {
            return when (leveltype) {
                LevelType.LEVEL1 -> {
                    IsisConfigurableLevels.Level1
                }
                LevelType.LEVEL12 -> {
                    IsisConfigurableLevels.Level1And2
                }
                LevelType.LEVEL2 -> {
                    IsisConfigurableLevels.Level2
                }
                else -> {
                    null
                }
            }
        }
    }
}