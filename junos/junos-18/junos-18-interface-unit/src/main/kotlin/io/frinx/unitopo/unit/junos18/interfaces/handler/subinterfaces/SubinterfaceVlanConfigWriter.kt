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

package io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.vlan.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.types.rev170714.QinqId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.types.rev170714.VlanId
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.vlan_choice.case_6.VlanTagsBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.slf4j.LoggerFactory
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces.group.interfaces.InterfaceKey as JunosInterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitBuilder as JunosInterfaceUnitBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.vlan_choice.Case1Builder as JunosVlanChoiceCase1Builder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.vlan_choice.Case6Builder as JunosVlanChoiceCase6Builder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.vlan_choice.case_6.vlan.tags.inner_choice.Case1Builder as InnterChoiceCase1Builder

class SubinterfaceVlanConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {
    override fun writeCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        val (_, unitName, underlayIfcUnitId) = getUnderlayId(id)
        val ifcUnitBuilder = JunosInterfaceUnitBuilder()
        ifcUnitBuilder.name = unitName
        ifcUnitBuilder.key = JunosInterfaceUnitKey(unitName)
        ifcUnitBuilder.fromOpenConfig(dataAfter)

        underlayAccess.merge(underlayIfcUnitId, ifcUnitBuilder.build())
    }

    override fun deleteCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        writeContext: WriteContext
    ) {
        val (_, _, underlayIfcUnitId) = getUnderlayId(id)
        // safe delete
        val ifcUnitBuilder = JunosInterfaceUnitBuilder()
            .setKey(JunosInterfaceUnitKey(id.firstKeyOf(Subinterface::class.java).index.toString()))
        when {
            dataBefore.vlanId.vlanId != null -> ifcUnitBuilder.setVlanChoiceCase1(null)
            dataBefore.vlanId.qinqId != null -> ifcUnitBuilder.setVlanChoiceCase6(null)
        }

        underlayAccess.put(underlayIfcUnitId, ifcUnitBuilder.build())
    }

    override fun updateCurrentAttributes(
        id: InstanceIdentifier<Config>,
        dataBefore: Config,
        dataAfter: Config,
        writeContext: WriteContext
    ) {
        writeCurrentAttributes(id, dataAfter, writeContext)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SubinterfaceVlanConfigWriter::class.java)

        private fun getUnderlayId(id: InstanceIdentifier<Config>):
            Triple<String, String, InstanceIdentifier<JunosInterfaceUnit>> {

            val ifcName = id.firstKeyOf(Interface::class.java).name
            val unitName = id.firstKeyOf(Subinterface::class.java).index.toString()
            val underlayIfcUnitId = InterfaceReader.JUNOS_IFCS
                .child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosInterfaceUnit::class.java, JunosInterfaceUnitKey(unitName))

            return Triple(ifcName, unitName, underlayIfcUnitId)
        }

        private fun JunosInterfaceUnitBuilder.fromOpenConfig(dataAfter: Config) {
            val vlanId = dataAfter.vlanId
            when {
                vlanId.vlanId != null -> setVlanChoiceCase1(vlanId.vlanId)
                vlanId.qinqId != null -> setVlanChoiceCase6(vlanId.qinqId)
            }
        }

        private fun JunosInterfaceUnitBuilder.setVlanChoiceCase1(vlanId: VlanId?) {
            vlanChoice = when (vlanId) {
                null -> JunosVlanChoiceCase1Builder().setVlanId(null).build()
                else -> JunosVlanChoiceCase1Builder().setVlanId(vlanId.value.toString()).build()
            }
        }

        private fun JunosInterfaceUnitBuilder.setVlanChoiceCase6(qinqId: QinqId?) {
            val vlanTags = when (qinqId) {
                null -> null
                else -> {
                    val value = qinqId.value
                    LOG.debug("JunosInterfaceUnitBuilder.fromOpenConfig: qinqId=$value")

                    val matcher = SubinterfaceVlanConfigReader.QINQID_PATTERN.matcher(value)
                    require(matcher.matches()) { "Qinq id does not match 'NNNN.NNNN'. qinqId=$value" }
                    VlanTagsBuilder()
                        .setOuter("${SubinterfaceVlanConfigReader.VLAN_PROTOCOL_ID}:${matcher.group("outer")}")
                        .setInnerChoice(
                            InnterChoiceCase1Builder()
                                .setInner("${SubinterfaceVlanConfigReader.VLAN_PROTOCOL_ID}:${matcher.group("inner")}")
                                .build())
                        .build()
                }
            }
            vlanChoice = JunosVlanChoiceCase6Builder().setVlanTags(vlanTags).build()
        }
    }
}