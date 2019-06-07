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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.ProcessBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.ProcessKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.process.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.process.DefaultVrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.process.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.process.VrfsBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.process.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.process.vrfs.VrfBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev180514.ospf.processes.process.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.Config
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.yang.binding.DataObject
import java.util.Collections
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class GlobalConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun updateCurrentAttributes(
        id: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (identifier, _) = getIdentifiers(id)

        val processBuilder = underlayAccess.read(identifier)
                .checkedGet()
                .or({ XR_EMPTY_OSPF })
                .let { ProcessBuilder(it) }

        val (underlayId, underlayCfg) = getData(id, dataAfter, processBuilder)
        underlayAccess.put(underlayId, underlayCfg)
    }

    override fun writeCurrentAttributes(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        val (underlayId, underlayCfg) = getData(id, dataAfter, ProcessBuilder())

        underlayAccess.merge(underlayId, underlayCfg)
    }

    override fun deleteCurrentAttributes(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val processName = id.firstKeyOf(Protocol::class.java).name
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        val processId = IID.create(Ospf::class.java).child(Processes::class.java).child(Process::class.java,
                ProcessKey(CiscoIosXrString(processName)))

        val vrfId = processId.let {
            if (NetworInstance.DEFAULT_NETWORK_NAME == vrfName) {
                processId.child(DefaultVrf::class.java)
            } else {
                processId.child(Vrfs::class.java).child(Vrf::class.java,
                        VrfKey(CiscoIosXrString(vrfName)))
            }
        }

        underlayAccess.delete(vrfId)
    }

    companion object {

        val XR_EMPTY_OSPF = ProcessBuilder().build()!!

        private fun getData(
            id: IID<Config>,
            data: Config,
            processBuilder: ProcessBuilder
        ):
                Pair<IID<Process>, Process> {
            val processName = id.firstKeyOf(Protocol::class.java).name
            val routerId = data.routerId.value
            val (processIid, vrfName) = getIdentifiers(id)

            val builder = processBuilder
                    .setKey(ProcessKey(CiscoIosXrString(processName)))
                    .apply {
                        if (NetworInstance.DEFAULT_NETWORK_NAME == vrfName) {
                            // reuse existing configuration if present
                            val builder = if (processBuilder.defaultVrf != null)
                                DefaultVrfBuilder(processBuilder.defaultVrf) else DefaultVrfBuilder()

                            defaultVrf = builder.setRouterId(Ipv4AddressNoZone(routerId)).build()
                        } else {

                            // reuse existing configuration if present
                            val vrfBuilder = processBuilder.vrfs?.vrf.orEmpty()
                                    .find { it.vrfName?.value == vrfName }
                                    ?.let { VrfBuilder(it) }
                                    ?: VrfBuilder()

                            vrfs = VrfsBuilder()
                                    .setVrf(Collections.singletonList(
                                            vrfBuilder
                                                    .setKey(VrfKey(CiscoIosXrString(vrfName)))
                                                    .setRouterId(Ipv4AddressNoZone(routerId))
                                                    .build())
                                    )
                                    .build()
                        }
                    }

            return Pair(processIid, builder.build())
        }

        fun getIdentifiers(id: IID<out DataObject>): Pair<IID<Process>, String> {
            val processName = id.firstKeyOf(Protocol::class.java).name
            val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
            val processIid = IID.create(Ospf::class.java).child(Processes::class.java).child(Process::class.java,
                    ProcessKey(CiscoIosXrString(processName)))
            return Pair(processIid, vrfName)
        }
    }
}