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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.VlanLogicalConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.VlanBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.vlan.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.vlan.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.types.rev170714.QinqId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.types.rev170714.VlanId
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.slf4j.LoggerFactory
import java.util.regex.Pattern
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.vlan_choice.Case1 as VlanChoiceCase1
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.vlan_choice.Case6 as VlanChoiceCase6
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.interfaces_type.unit.vlan_choice.case_6.vlan.tags.inner_choice.Case1 as InnterChoiceCase1
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class SubinterfaceVlanConfigReader(private val underlayAccess: UnderlayAccess)
    : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: IID<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val name = instanceIdentifier.firstKeyOf(Interface::class.java).name
        val unitId = instanceIdentifier.firstKeyOf(Subinterface::class.java).index
        InterfaceReader.readUnitCfg(underlayAccess, name, unitId) { configBuilder.fromUnderlay(it) }
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as VlanBuilder).config = config
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SubinterfaceVlanConfigReader::class.java)

        const val VLAN_PROTOCOL_ID = "0x8100"
        val QINQID_PATTERN = Pattern.compile("(?<outer>\\d+)\\.(?<inner>\\d+)")
        private val TAGGED_VLAN_PATTERN = Pattern.compile("$VLAN_PROTOCOL_ID:(?<vlanid>\\d+)")

        private fun ConfigBuilder.fromUnderlay(junosUnit: JunosInterfaceUnit) {
            val vlanChoice = junosUnit.vlanChoice

            when (vlanChoice) {
                null -> {} // NOP
                is VlanChoiceCase1 -> setVlanIdFromVlanChoice(vlanChoice)
                is VlanChoiceCase6 -> setVlanIdFromVlanChoice(vlanChoice)
                else -> LOG.info("Unsupported vlan type. class=${vlanChoice.javaClass}, value=$vlanChoice")
            }
        }

        private fun ConfigBuilder.setVlanIdFromVlanChoice(vlanChoice: VlanChoiceCase1) {
            vlanId = VlanLogicalConfig.VlanId(VlanId(vlanChoice.vlanId.toInt()))
        }

        private fun ConfigBuilder.setVlanIdFromVlanChoice(vlanChoice: VlanChoiceCase6) {
            var outer = ""
            var inner = ""
            val outerMatcher = TAGGED_VLAN_PATTERN.matcher(vlanChoice.vlanTags.outer)

            if (outerMatcher.matches()) {
                outer = outerMatcher.group("vlanid")
            } else {
                LOG.info("Outer tag does not match '$VLAN_PROTOCOL_ID:NNNN'. tag=${vlanChoice.vlanTags.outer}.")
            }

            val innerChoice = vlanChoice.vlanTags.innerChoice
            when (innerChoice) {
                is InnterChoiceCase1 -> {
                    val innerMatcher = TAGGED_VLAN_PATTERN.matcher(innerChoice.inner)
                    if (innerMatcher.matches()) {
                        inner = innerMatcher.group("vlanid")
                    } else {
                        LOG.info("Inner tag does not match '$VLAN_PROTOCOL_ID:NNNN'. tag=${innerChoice.inner}.")
                    }
                }
                else -> LOG.info("Unsupported inner vlan type. class=${innerChoice?.javaClass}, value=$innerChoice")
            }
            val qinqTag = "$outer.$inner"
            if (QINQID_PATTERN.matcher(qinqTag).matches()) {
                vlanId = VlanLogicalConfig.VlanId(QinqId(qinqTag))
            }
        }
    }
}