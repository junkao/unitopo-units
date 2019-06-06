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

package io.frinx.unitopo.unit.xr66.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.ConfigBuilder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class GlobalConfigReader(private val access: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(id: IID<Config>) = ConfigBuilder()

    override fun readCurrentAttributes(id: IID<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java)
        val protKey = id.firstKeyOf(Protocol::class.java)

        readProcess(access, protKey, { builder.fromUnderlay(it, vrfName.name) })
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as GlobalBuilder).config = readValue
    }

    companion object {
        private val UNDERLAY_OSPF_PROCESSES = IID.create(Ospf::class.java).child(Processes::class.java)

        fun readProcess(access: UnderlayAccess, protKey: ProtocolKey, handler: (Process) -> Unit) {
            getProcess(access, protKey)
                    ?.let(handler)
        }

        fun getProcess(access: UnderlayAccess, protKey: ProtocolKey): Process? {
            return access.read(UNDERLAY_OSPF_PROCESSES)
                    .checkedGet()
                    .orNull()
                    ?.process.orEmpty()
                    .find { it.processName.value == protKey.name }
        }

        fun getRouterId(vrfName: String, p: Process): DottedQuad? {
            // Set router ID for appropriate VRF
            var routerId: DottedQuad? = null
            if (NetworInstance.DEFAULT_NETWORK_NAME == vrfName) {
                p.defaultVrf?.routerId?.value?.let { routerId = DottedQuad(it) }
            } else {
                p.vrfs?.vrf.orEmpty()
                        .find { it.vrfName.value == vrfName }
                        ?.let { routerId = if (it.routerId != null) DottedQuad(it.routerId?.value) else null }
            }
            return routerId
        }
    }
}

private fun ConfigBuilder.fromUnderlay(p: Process, vrfName: String) {
    GlobalConfigReader.getRouterId(vrfName, p)?.let {
        routerId = it
    }
}