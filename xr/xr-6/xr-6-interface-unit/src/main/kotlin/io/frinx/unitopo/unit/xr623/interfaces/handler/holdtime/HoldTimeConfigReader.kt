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

package io.frinx.unitopo.unit.xr623.interfaces.handler.holdtime

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.drivers.media.eth.cfg.rev150730.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222._interface.phys.holdtime.top.hold.time.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class HoldTimeConfigReader(private val underlayAccess: UnderlayAccess)
    : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Config>,
        builder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val ifcName = instanceIdentifier.firstKeyOf(Interface::class.java).name

        if (isSupportedInterface(ifcName)) {
            InterfaceReader.readInterfaceCfg(underlayAccess, ifcName) { extractHoldTime(it, builder) }
        }
    }

    private fun extractHoldTime(ifcCfg: InterfaceConfiguration, builder: ConfigBuilder) {
        ifcCfg.getAugmentation(InterfaceConfiguration1::class.java)?.let {
            builder.fromUnderlay(it)
        }
    }

    companion object {

        fun isSupportedInterface(name: String): Boolean {
            return Util.parseIfcType(name) == EthernetCsmacd::class.java
        }

        private fun ConfigBuilder.fromUnderlay(underlay: InterfaceConfiguration1) {
            up = underlay.ethernet.carrierDelay.carrierDelayUp
            down = underlay.ethernet.carrierDelay.carrierDelayDown
        }
    }
}