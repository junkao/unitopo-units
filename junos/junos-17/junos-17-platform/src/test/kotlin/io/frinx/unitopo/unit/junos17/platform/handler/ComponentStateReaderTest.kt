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

package io.frinx.unitopo.unit.junos17.platform.handler

import com.google.common.base.Optional
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode
import org.opendaylight.yangtools.yang.model.api.SchemaPath
import org.opendaylight.controller.config.util.xml.XmlUtil
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext
import org.opendaylight.netconf.api.NetconfMessage
import org.opendaylight.netconf.sal.connect.netconf.schema.mapping.NetconfMessageTransformer
import org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yangtools.yang.binding.util.BindingReflections
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.component.StateBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jrpc.show.version.junos._17._3r1._10.rev170101.GetSoftwareInformationInput
import org.opendaylight.yangtools.yang.common.QName

class ComponentStateReaderTest {

    private val mib = createMib()
    val schemaContext = createSchemaContext(mib)
    val transformer = NetconfMessageTransformer(schemaContext, true)
    private val qName = GetSoftwareInformationInput.QNAME

    private val schema = SchemaPath.create(true,
        QName.create(qName, ComponentStateReader.GET_SOFT_INF).intern())

    private fun createSchemaContext(mib: ModuleInfoBackedContext): SchemaContext {
        return mib.schemaContext
    }

    private fun createMib(): ModuleInfoBackedContext {
        val moduleInfoBackedContext = ModuleInfoBackedContext.create()
        moduleInfoBackedContext.addModuleInfos(getModels())
        return moduleInfoBackedContext
    }

    private fun getModels(): Collection<YangModuleInfo> {
        return BindingReflections.loadModuleInfos()
    }

    protected fun getResourceAsString(name: String) = javaClass.getResource(name).readText(Charsets.UTF_8)

    private val DATA_NODES = getResourceAsString("/junos-conf.xml")
    val Reader = ComponentStateReader(UnderlayAccess.NOOP)
    val msg = NetconfMessage(XmlUtil.readXmlToDocument(DATA_NODES))
    val normalizedRpcResult = transformer.toRpcResult(msg,
        schema).result

    val normalizedData =
        (normalizedRpcResult as ContainerNode).getChild(
            NetconfMessageTransformUtil.toId(QName.create(qName, ComponentStateReader.OUTPUT_C))).get()

    val optional = Optional.of(normalizedData)

    val choice = optional

    @Test
    @Throws(Exception::class)
    fun testParse() {
        val stateBuilder = StateBuilder()
        Reader.parseFields(stateBuilder, choice)
        Assert.assertEquals(StateBuilder()
                .setId("JUNOS")
                .setName("OS")
                .setSoftwareVersion("17.3R1.10")
                .build(),

                stateBuilder.build())
    }
}