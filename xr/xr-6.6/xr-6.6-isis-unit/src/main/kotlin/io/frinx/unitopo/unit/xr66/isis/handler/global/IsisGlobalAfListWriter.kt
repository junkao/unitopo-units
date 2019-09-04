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

package io.frinx.unitopo.unit.xr66.isis.handler.global

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.translate.unit.commons.handler.spi.ChecksMap
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr66.isis.handler.IsisProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev170501.IsisInstanceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.Af
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.afi.safi.list.AfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.Afs as UlAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.afs.Af as UlAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev181123.isis.instances.instance.afs.AfBuilder as UlAfBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

open class IsisGlobalAfListWriter(private val access: UnderlayAccess) : ListWriterCustomizer<Af, AfKey> {

    override fun writeCurrentAttributes(id: IID<Af>, data: Af, writeContext: WriteContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        require(ChecksMap.PathCheck.Protocol.ISIS.canProcess(id, writeContext, false))
        require(vrfName == NetworInstance.DEFAULT_NETWORK_NAME) {
            "IS-IS configuration should be set in default network: $vrfName"
        }
        val (underlayId, underlayData) = getData(id)
        access.safePut(underlayId, underlayData)
    }

    override fun deleteCurrentAttributes(
        id: IID<Af>,
        dataBefore: Af,
        writeContext: WriteContext
    ) {
        val (underlayId, _) = getData(id)
        val underlayBefore = access.read(underlayId).checkedGet().orNull()!!
        access.safeDelete(underlayId, underlayBefore)
    }

    override fun updateCurrentAttributes(
        id: IID<Af>,
        dataBefore: Af,
        dataAfter: Af,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    private fun getData(id: IID<Af>): Pair<IID<UlAf>, UlAf>
    {
        val protKey = id.firstKeyOf(Protocol::class.java)
        val afKey = id.firstKeyOf(Af::class.java)
        val ulAfKey = afKey.toUnderlay()
        val underlayId = IsisProtocolReader.UNDERLAY_ISIS
            .child(Instance::class.java, InstanceKey(IsisInstanceName(protKey.name)))
            .child(UlAfs::class.java)
            .child(UlAf::class.java, ulAfKey)
        val underlayData = UlAfBuilder()
            .setAfName(ulAfKey.afName)
            .setSafName(ulAfKey.safName)
            .build()
        return Pair(underlayId, underlayData)
    }
}