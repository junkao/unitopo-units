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
package io.frinx.unitopo.unit.xr623.isis.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.translate.unit.commons.handler.spi.CompositeWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.InstanceBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev151109.isis.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev151109.IsisInstanceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.Config
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class IsisProtocolConfigWriter(private val underlayAccess: UnderlayAccess) : CompositeWriter.Child<Config> {
    override fun writeCurrentAttributesWResult(id: IID<Config>, dataAfter: Config, wtx: WriteContext): Boolean {
        if (!ChecksMap.PathCheck.Protocol.ISIS.canProcess(id, wtx, false)) {
            return false
        }

        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        val instanceName = id.firstKeyOf(Protocol::class.java).name

        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "IS-IS configuration should be set in default network: $vrfName"
        }

        val underlayId = getUnderlayId(instanceName)
        val builder = InstanceBuilder()
            .setInstanceName(IsisInstanceName(instanceName))
            .setRunning(true)

        underlayAccess.merge(underlayId, builder.build())
        return true
    }

    override fun updateCurrentAttributesWResult(
        iid: IID<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ): Boolean {
        if (!ChecksMap.PathCheck.Protocol.ISIS.canProcess(iid, writeContext, false)) {
            return false
        }
        // there is no modifiable attributes in this container.
        // identifier and name are key attribute in parent container and other attributes are not used by this handler.
        return true
    }

    override fun deleteCurrentAttributesWResult(id: IID<Config>, dataBefore: Config, wtx: WriteContext): Boolean {
        if (!ChecksMap.PathCheck.Protocol.ISIS.canProcess(id, wtx, true)) {
            return false
        }

        val instanceName = id.firstKeyOf(Protocol::class.java).name!!
        val id = getUnderlayId(instanceName)

        underlayAccess.delete(id)
        return true
    }

    companion object {
        fun getUnderlayId(instanceName: String): IID<Instance> {
            return IsisProtocolReader.UNDERLAY_ISIS
                .child(Instance::class.java, InstanceKey(IsisInstanceName(instanceName)))
        }
    }
}