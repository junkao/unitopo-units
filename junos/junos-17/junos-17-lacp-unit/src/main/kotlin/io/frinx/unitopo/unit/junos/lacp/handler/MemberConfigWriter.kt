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

package io.frinx.unitopo.unit.junos.lacp.handler

import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.aggregation.lacp.members.top.members.Member
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.aggregation.lacp.members.top.members.member.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lacp.rev170505.lacp.interfaces.top.interfaces.Interface as LacpInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.GigetherOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.gigether.options.Ieee8023ad
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.gigether.options.Ieee8023adBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class MemberConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(iid: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        isSupportedMemberInterface(iid)
        val (underlayGigEthIeee8023adId, underlayGigEthIeee8023ad) = getData(iid)
        underlayAccess.put(underlayGigEthIeee8023adId, underlayGigEthIeee8023ad)
    }

    override fun deleteCurrentAttributes(iid: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        isSupportedMemberInterface(iid)
        val underlayGigEthIeee8023adId = getUnderlayId(iid)
        underlayAccess.delete(underlayGigEthIeee8023adId)
    }

    private fun getData(iid: InstanceIdentifier<Config>):
        Pair<InstanceIdentifier<Ieee8023ad>, Ieee8023ad> {
        val underlayGigEthIeee8023adId = getUnderlayId(iid)
        val bundleName = iid.firstKeyOf(LacpInterface::class.java).name
        val gigEthIeee8023adBuilder = Ieee8023adBuilder()
            .setBundle(Ieee8023ad.Bundle(bundleName))
            .build()
        return Pair(underlayGigEthIeee8023adId, gigEthIeee8023adBuilder)
    }

    companion object {
        private fun getUnderlayId(iid: InstanceIdentifier<Config>): InstanceIdentifier<Ieee8023ad> {
            val ifcName = iid.firstKeyOf(Member::class.java).`interface`
            return InterfaceReader.IFCS
                .child(Interface::class.java, InterfaceKey(ifcName))
                .child(GigetherOptions::class.java)
                .child(Ieee8023ad::class.java)
        }

        private fun isSupportedMemberInterface(id: InstanceIdentifier<Config>) {
            val ifcName = id.firstKeyOf(Member::class.java).`interface`
            val ifcType = InterfaceConfigReader.parseIfcType(ifcName)
            Preconditions.checkArgument(ifcType === EthernetCsmacd::class.java) {
                """Ethernet interface aggregation configuration is supported only on ethernet interfaces.
                Cannot configure interface $ifcName of type $ifcType""".trimIndent()
            }
        }
    }
}