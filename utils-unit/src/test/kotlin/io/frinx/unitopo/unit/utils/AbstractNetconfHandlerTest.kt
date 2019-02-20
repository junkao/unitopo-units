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

package io.frinx.unitopo.unit.utils

import io.frinx.translate.unit.commons.handler.spi.GenericTranslateContext
import org.opendaylight.controller.config.util.xml.XmlUtil
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext
import org.opendaylight.netconf.api.NetconfMessage
import org.opendaylight.netconf.sal.connect.netconf.schema.mapping.NetconfMessageTransformer
import org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yangtools.yang.binding.util.BindingReflections
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.yangtools.yang.model.api.SchemaPath

abstract class AbstractNetconfHandlerTest {

    private val mib = createMib()
    val schemaContext = createSchemaContext(mib)
    val transformer = NetconfMessageTransformer(schemaContext, true)
    val codec = GenericTranslateContext.getCodec(mib, schemaContext)

    private fun createSchemaContext(mib: ModuleInfoBackedContext): SchemaContext {
        return mib.schemaContext
    }

    private fun createMib(): ModuleInfoBackedContext {
        val moduleInfoBackedContext = ModuleInfoBackedContext.create()
        moduleInfoBackedContext.addModuleInfos(getModels())
        return moduleInfoBackedContext
    }

    open fun getModels(): Collection<YangModuleInfo> {
        return BindingReflections.loadModuleInfos()
    }

    fun <T : DataObject> parseGetCfgResponse(xml: String, type: InstanceIdentifier<T>): T {
        val msg = NetconfMessage(XmlUtil.readXmlToDocument(xml))

        val normalizedRpcResult = transformer.toRpcResult(msg, SchemaPath.create(true,
                NetconfMessageTransformUtil.NETCONF_GET_CONFIG_QNAME)).result
        val normalizedData = (normalizedRpcResult as ContainerNode).getChild(
                NetconfMessageTransformUtil.toId(NetconfMessageTransformUtil.NETCONF_DATA_QNAME)).get()

        val yangIID = codec.toYangInstanceIdentifier(type)
        val findNode = NormalizedNodes.findNode(normalizedData, yangIID)

        val value = codec.fromNormalizedNode(yangIID, findNode.get())!!.value
        return type.targetType.cast(value)
    }

    protected fun getResourceAsString(name: String) = AbstractNetconfHandlerTest.getResourceAsString(javaClass, name)

    companion object {
        fun getResourceAsString(clazz: Class<*>, name: String) = clazz.getResource(name).readText(Charsets.UTF_8)
    }
}