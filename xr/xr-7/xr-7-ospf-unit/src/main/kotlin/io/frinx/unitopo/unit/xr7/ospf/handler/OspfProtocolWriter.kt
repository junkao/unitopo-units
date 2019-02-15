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
package io.frinx.unitopo.unit.xr7.ospf.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.handlers.ospf.OspfWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import java.util.Collections
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.ProcessBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.ProcessKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.process.VrfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.process.vrfs.VrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.process.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.Config
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class OspfProtocolWriter(private val underlayAccess: UnderlayAccess) : OspfWriter<Config> {

    override fun updateCurrentAttributesForType(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (identifier, _) = getIdentifiers(iid)

        val processBuilder = underlayAccess.read(identifier)
            .checkedGet()
            .or({ XR_EMPTY_OSPF })
            .let { ProcessBuilder(it) }

        val (underlayId, underlayCfg) = getData(iid, processBuilder)
        underlayAccess.put(underlayId, underlayCfg)
    }

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        val (underlayId, underlayCfg) = getData(id, ProcessBuilder())

        underlayAccess.merge(underlayId, underlayCfg)
    }

    override fun deleteCurrentAttributesForType(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val processName = id.firstKeyOf(Protocol::class.java).name
        val processId = IID.create(Ospf::class.java).child(Processes::class.java).child(Process::class.java,
            ProcessKey(CiscoIosXrString(processName)))

        underlayAccess.delete(processId)
    }

    private fun getData(
        id: IID<Config>,
        processBuilder: ProcessBuilder
    ):
        Pair<IID<Process>, Process> {
        val processName = id.firstKeyOf(Protocol::class.java).name
        val (processIid, vrfName) = getIdentifiers(id)

        val builder = processBuilder
                .setKey(ProcessKey(CiscoIosXrString(processName)))
                .apply {
                    if (NetworInstance.DEFAULT_NETWORK_NAME != vrfName) {
                    // reuse existing configuration if present
                    val vrfBuilder = processBuilder.vrfs?.vrf.orEmpty()
                        .find { it.vrfName?.value == vrfName }
                        ?.let { VrfBuilder(it) }
                        ?: VrfBuilder()

                    vrfs = VrfsBuilder()
                        .setVrf(Collections.singletonList(
                            vrfBuilder
                                .setKey(VrfKey(CiscoIosXrString(vrfName)))
                                .build())
                        )
                        .build()
                    }
                }
        return Pair(processIid, builder.build())
    }

    companion object {
        val XR_EMPTY_OSPF = ProcessBuilder().build()!!

        fun getIdentifiers(id: IID<out DataObject>): Pair<IID<Process>, String> {
            val processName = id.firstKeyOf(Protocol::class.java).name
            val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
            val processIid = IID.create(Ospf::class.java).child(Processes::class.java).child(Process::class.java,
                ProcessKey(CiscoIosXrString(processName)))
            return Pair(processIid, vrfName)
        }
    }
}