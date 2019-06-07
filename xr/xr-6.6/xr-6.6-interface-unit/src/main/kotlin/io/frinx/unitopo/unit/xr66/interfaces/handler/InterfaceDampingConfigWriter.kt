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

package io.frinx.unitopo.unit.xr66.interfaces.handler

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations._interface.configuration.Dampening
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations._interface.configuration.DampeningBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.damping.rev171024.damping.top.damping.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceDampingConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        data: Config,
        writeContext: WriteContext
    ) {

        val ifcName = id.firstKeyOf(Interface::class.java).name
        require(InterfaceDampingConfigReader.isSupportedInterface(ifcName)) {
            "Unsupported interface: $ifcName"
        }

        if (!data.isEnabled) {
            return
        }
        underlayAccess.put(
            getUnderlayId(id),
            DampeningBuilder().formOpenConfig(data).build()
        )
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {

        if (!dataAfter.isEnabled) {
            deleteCurrentAttributes(id, dataBefore, writeContext)
            return
        }

        underlayAccess.merge(
            getUnderlayId(id),
            DampeningBuilder().formOpenConfig(dataAfter).build()
        )
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        underlayAccess.delete(getUnderlayId(id))
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config>):
        InstanceIdentifier<Dampening> {
        val interfaceActive = InterfaceActive("act")
        val ifcName = InterfaceName(id.firstKeyOf(Interface::class.java).name)
        return InterfaceReader.IFC_CFGS
            .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(interfaceActive, ifcName))
            .child(Dampening::class.java)
    }
}

private fun DampeningBuilder.formOpenConfig(data: Config): DampeningBuilder {
    halfLife = data.halfLife
    suppressTime = data.maxSuppress
    suppressThreshold = data.suppress
    reuseThreshold = data.reuse
    args = when {
        halfLife == null -> Dampening.Args.DefaultValues
        reuseThreshold == null -> Dampening.Args.SpecifyHalfLife
        restartPenalty == null -> Dampening.Args.SpecifyAll
        else -> Dampening.Args.SpecifyRp
    }
    return this
}