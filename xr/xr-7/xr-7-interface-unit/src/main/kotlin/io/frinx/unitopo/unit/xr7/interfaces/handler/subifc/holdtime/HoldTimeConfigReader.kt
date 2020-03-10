/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.interfaces.handler.subifc.holdtime

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.xr7.interfaces.handler.Util
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev190405._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev190405.InterfaceConfiguration7
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.HoldTimeBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class HoldTimeConfigReader(private val underlayAccess: UnderlayAccess)
    : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as HoldTimeBuilder).config = readValue
    }

    override fun getBuilder(p0: IID<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Config>,
        builder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val ifcName = instanceIdentifier.firstKeyOf(Interface::class.java).name
        val subifcIndex = instanceIdentifier.firstKeyOf(Subinterface::class.java).index
        val subifcName = Util.getSubIfcName(ifcName, subifcIndex)

        if (isSupportedInterface(ifcName)) {
            InterfaceReader.readInterfaceCfg(underlayAccess, subifcName) { extractHoldTime(it, builder) }
        }
    }

    private fun extractHoldTime(ifcCfg: InterfaceConfiguration, builder: ConfigBuilder) {
        ifcCfg.getAugmentation(InterfaceConfiguration7::class.java)?.let {
            builder.fromUnderlay(it)
        }
    }

    companion object {
        fun isSupportedInterface(name: String): Boolean {
            return Util.parseIfcType(name) == Ieee8023adLag::class.java
        }

        private fun ConfigBuilder.fromUnderlay(underlay: InterfaceConfiguration7) {
            up = underlay.carrierDelay.carrierDelayUp
        }
    }
}