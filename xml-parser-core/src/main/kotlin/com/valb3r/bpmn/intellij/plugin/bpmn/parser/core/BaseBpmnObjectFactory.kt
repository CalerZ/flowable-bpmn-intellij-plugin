package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnObjectFactory
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ConditionExpression
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateConditionalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateMessageCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateSignalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateTimerCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateEscalationThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnEventGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnInclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnParallelGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.*
import java.util.*
import kotlin.reflect.KClass

abstract class BaseBpmnObjectFactory : BpmnObjectFactory {
    private val mapper = ObjectMapper()

    override fun <T : WithBpmnId> newBpmnObject(clazz: KClass<T>): T {
        val result: WithBpmnId = when(clazz) {
            BpmnStartEvent::class -> BpmnStartEvent(generateBpmnId(), null, null, null, null, null, null, null, null)
            BpmnStartConditionalEvent::class -> BpmnStartConditionalEvent(generateBpmnId(), null, null)
            BpmnStartEscalationEvent::class -> BpmnStartEscalationEvent(generateBpmnId(), null, null)
            BpmnStartErrorEvent::class -> BpmnStartErrorEvent(generateBpmnId(), null, null)
            BpmnStartMessageEvent::class -> BpmnStartMessageEvent(generateBpmnId(), null, null)
            BpmnStartSignalEvent::class -> BpmnStartSignalEvent(generateBpmnId(), null, null)
            BpmnStartTimerEvent::class -> BpmnStartTimerEvent(generateBpmnId(), null, null)
            BpmnBoundaryCancelEvent::class -> BpmnBoundaryCancelEvent(generateBpmnId(), null, null, null)
            BpmnBoundaryCompensationEvent::class -> BpmnBoundaryCompensationEvent(generateBpmnId(), null, null, null)
            BpmnBoundaryConditionalEvent::class -> BpmnBoundaryConditionalEvent(generateBpmnId(), null, null, null)
            BpmnBoundaryErrorEvent::class -> BpmnBoundaryErrorEvent(generateBpmnId(), null, null, null)
            BpmnBoundaryEscalationEvent::class -> BpmnBoundaryEscalationEvent(generateBpmnId(), null, null, null)
            BpmnBoundaryMessageEvent::class -> BpmnBoundaryMessageEvent(generateBpmnId(), null, null, null)
            BpmnBoundarySignalEvent::class -> BpmnBoundarySignalEvent(generateBpmnId(), null, null, null)
            BpmnBoundaryTimerEvent::class -> BpmnBoundaryTimerEvent(generateBpmnId(), null, null, null)
            BpmnUserTask::class -> BpmnUserTask(generateBpmnId(), null, null, null, null, null, null, null, null, null, null, null)
            BpmnScriptTask::class -> BpmnScriptTask(generateBpmnId(), null, null, null, null, null, null, null)
            BpmnServiceTask::class -> BpmnServiceTask(generateBpmnId(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
            BpmnBusinessRuleTask::class -> BpmnBusinessRuleTask(generateBpmnId(), null, null, null, null, null, null, null, null)
            BpmnReceiveTask::class -> BpmnReceiveTask(generateBpmnId(), null, null, null, null)
            BpmnManualTask::class -> BpmnManualTask(generateBpmnId(), null, null, null, null, null)
            BpmnCamelTask::class -> BpmnCamelTask(generateBpmnId(), null, null, null, null, null, null)
            BpmnHttpTask::class -> BpmnHttpTask(generateBpmnId(), null, null, null, null, null)
            BpmnMailTask::class -> BpmnMailTask(generateBpmnId(), null, null, null, null)
            BpmnMuleTask::class -> BpmnMuleTask(generateBpmnId(), null, null, null, null, null)
            BpmnDecisionTask::class -> BpmnDecisionTask(generateBpmnId(), null, null, null, null, null)
            BpmnShellTask::class -> BpmnShellTask(generateBpmnId(), null, null, null, null, null)
            BpmnSubProcess::class -> BpmnSubProcess(generateBpmnId(), null, null, null, null, false, false)
            BpmnEventSubprocess::class -> BpmnEventSubprocess(generateBpmnId(), null, null, null, null, true)
            BpmnTransactionalSubProcess::class -> BpmnTransactionalSubProcess(generateBpmnId(), null, null, null, null, true)
            BpmnCallActivity::class -> BpmnCallActivity(generateBpmnId(), null, null, null, "", null, null, null, null)
            BpmnAdHocSubProcess::class -> BpmnAdHocSubProcess(generateBpmnId(), null, null, CompletionCondition(null))
            BpmnExclusiveGateway::class -> BpmnExclusiveGateway(generateBpmnId(), null, null, null)
            BpmnParallelGateway::class -> BpmnParallelGateway(generateBpmnId(), null, null, null)
            BpmnInclusiveGateway::class -> BpmnInclusiveGateway(generateBpmnId(), null, null, null)
            BpmnEventGateway::class -> BpmnEventGateway(generateBpmnId(), null, null, null)
            BpmnEndEvent::class -> BpmnEndEvent(generateBpmnId(), null, null, null, null, null, null)
            BpmnEndCancelEvent::class -> BpmnEndCancelEvent(generateBpmnId(), null, null)
            BpmnEndErrorEvent::class -> BpmnEndErrorEvent(generateBpmnId(), null, null)
            BpmnEndEscalationEvent::class -> BpmnEndEscalationEvent(generateBpmnId(), null, null)
            BpmnEndTerminateEvent::class -> BpmnEndTerminateEvent(generateBpmnId(), null, null)
            BpmnIntermediateTimerCatchingEvent::class -> BpmnIntermediateTimerCatchingEvent(generateBpmnId(), null, null)
            BpmnIntermediateMessageCatchingEvent::class -> BpmnIntermediateMessageCatchingEvent(generateBpmnId(), null, null)
            BpmnIntermediateSignalCatchingEvent::class -> BpmnIntermediateSignalCatchingEvent(generateBpmnId(), null, null)
            BpmnIntermediateConditionalCatchingEvent::class -> BpmnIntermediateConditionalCatchingEvent(generateBpmnId(), null, null)
            BpmnIntermediateNoneThrowingEvent::class -> BpmnIntermediateNoneThrowingEvent(generateBpmnId(), null, null)
            BpmnIntermediateSignalThrowingEvent::class -> BpmnIntermediateSignalThrowingEvent(generateBpmnId(), null, null)
            BpmnIntermediateEscalationThrowingEvent::class -> BpmnIntermediateEscalationThrowingEvent(generateBpmnId(), null, null)
            else -> throw IllegalArgumentException("Can't create class: " + clazz.qualifiedName)
        }

        return result as T
    }

    override fun <T : WithDiagramId> newDiagramObject(clazz: KClass<T>, forBpmnObject: WithBpmnId): T {
        val result: WithDiagramId = when (clazz) {
            EdgeElement::class -> EdgeElement(
                DiagramElementId("edge-" + UUID.randomUUID().toString()),
                forBpmnObject.id,
                null
            )
            ShapeElement::class -> ShapeElement(
                DiagramElementId("shape-" + UUID.randomUUID().toString()),
                forBpmnObject.id,
                bounds(forBpmnObject)
            )
            else -> throw IllegalArgumentException("Can't create class: " + clazz.qualifiedName)
        }

        return result as T
    }

    override fun <T : WithBpmnId> newOutgoingSequence(obj: T): BpmnSequenceFlow {
        return when (obj) {
            is BpmnExclusiveGateway, is BpmnParallelGateway, is BpmnInclusiveGateway -> BpmnSequenceFlow(
                generateBpmnId(),
                null,
                null,
                obj.id.id,
                "",
                ConditionExpression("tFormalExpression", "")
            )
            else -> BpmnSequenceFlow(generateBpmnId(), null, null, obj.id.id, "", null)
        }
    }

    override fun <T : WithBpmnId> propertiesOf(obj: T): PropertyTable {
        val table = when (obj) {
            is BpmnStartEvent, is BpmnStartTimerEvent, is BpmnStartSignalEvent, is BpmnStartMessageEvent,
            is BpmnStartErrorEvent, is BpmnStartEscalationEvent, is BpmnStartConditionalEvent, is BpmnEndEvent,
            is BpmnEndErrorEvent, is BpmnEndCancelEvent, is BpmnEndEscalationEvent,
            is BpmnEndTerminateEvent, is BpmnBoundaryCancelEvent, is BpmnBoundaryCompensationEvent,
            is BpmnBoundaryConditionalEvent, is BpmnBoundaryEscalationEvent, is BpmnBoundaryMessageEvent, is BpmnBoundaryErrorEvent,
            is BpmnBoundarySignalEvent, is BpmnBoundaryTimerEvent, is BpmnUserTask, is BpmnScriptTask, is BpmnServiceTask, is BpmnBusinessRuleTask,
            is BpmnReceiveTask, is BpmnCamelTask, is BpmnHttpTask, is BpmnMuleTask, is BpmnDecisionTask, is BpmnShellTask, is BpmnMailTask,
            is BpmnManualTask,
            is BpmnSubProcess, is BpmnEventSubprocess, is BpmnTransactionalSubProcess, is BpmnAdHocSubProcess, is BpmnCollapsedSubprocess, is BpmnTransactionCollapsedSubprocess,
            is BpmnExclusiveGateway, is BpmnParallelGateway, is BpmnInclusiveGateway, is BpmnEventGateway, is BpmnIntermediateTimerCatchingEvent,
            is BpmnIntermediateMessageCatchingEvent, is BpmnIntermediateSignalCatchingEvent, is BpmnIntermediateConditionalCatchingEvent,
            is BpmnIntermediateNoneThrowingEvent, is BpmnIntermediateSignalThrowingEvent, is BpmnIntermediateEscalationThrowingEvent, is BpmnProcess
            -> processDtoToPropertyMap(obj)

            is BpmnCallActivity -> fillForCallActivity(obj)
            is BpmnSequenceFlow -> fillForSequenceFlow(obj)
            else -> throw IllegalArgumentException("Can't parse properties of: ${obj.javaClass}")
        }
        
        return PropertyTable(table)
    }

    protected abstract fun propertyTypes(): List<PropertyType>

    protected open fun processDtoToPropertyMap(dto: Any): MutableMap<PropertyType, MutableList<Property>> {
        val result: MutableMap<PropertyType, MutableList<Property>> = mutableMapOf()
        val propertyTree = mapper.valueToTree<JsonNode>(dto)

        for (type in propertyTypes()) {
            parseValue(type.path, type, propertyTree, result)
        }

        return result
    }

    protected open fun fillForSequenceFlow(activity: BpmnSequenceFlow): MutableMap<PropertyType, MutableList<Property>> {
        verifyConditionalExpressionInSequenceFlow(activity)
        return processDtoToPropertyMap(activity)
    }

    protected open fun verifyConditionalExpressionInSequenceFlow(activity: BpmnSequenceFlow) {
        if (null != activity.conditionExpression && activity.conditionExpression!!.type != "tFormalExpression") {
            throw IllegalArgumentException("Unknown type: ${activity.conditionExpression!!.type}")
        }
    }

    private fun fillForCallActivity(activity: BpmnCallActivity): MutableMap<PropertyType, MutableList<Property>> {
        val properties = processDtoToPropertyMap(activity)
        // TODO: handle extension elements
        return properties
    }

    private fun parseValue(
        path: String,
        type: PropertyType,
        propertyTree: JsonNode,
        result: MutableMap<PropertyType, MutableList<Property>>,
        indexInArray: String? = null
    ) {
        val split = path.split(".", limit = 2)
        val targetId = if (null != indexInArray) split[0].substring(1) else split[0]

        if (true == propertyTree[targetId]?.isArray) {
            propertyTree[targetId].forEachIndexed { index, it -> parseValue(split[1], type, it, result, it[type.indexInGroupArrayName!!].asText()) }
            return
        }

        propertyTree[targetId]?.apply {
            if (split.size < 2) {
                doParse(this, result, type, indexInArray)
                return
            }

            if (split[1].contains(".")) {
                parseValue(split[1], type, this, result, indexInArray)
                return
            }

            val value = this[split[1]]
            doParse(value, result, type, indexInArray)
        }
    }

    private fun doParse(
        node: JsonNode?,
        result: MutableMap<PropertyType, MutableList<Property>>,
        type: PropertyType,
        indexInArray: String? = null
    ) {
        val makeProperty = {it: Any? ->
            if (null != indexInArray) { Property(ValueInArray(indexInArray, it))} else Property(it)
        }

        if (null == node || node.isNull) {
            result.computeIfAbsent(type) { mutableListOf() }.add(makeProperty(type.defaultValueIfNull))
            return
        }

        val propVal = when (type.valueType) {
            PropertyValueType.STRING, PropertyValueType.CLASS, PropertyValueType.EXPRESSION, PropertyValueType.ATTACHED_SEQUENCE_SELECT
            -> if (node.isNull) makeProperty(null) else makeProperty(node.asText())
            PropertyValueType.BOOLEAN -> makeProperty(node.asBoolean())
        }
        result.computeIfAbsent(type) { mutableListOf() }.add(propVal)
    }

    private fun bounds(forBpmnObject: WithBpmnId): BoundsElement {
        return when (forBpmnObject) {
            is BpmnStartEvent, is BpmnStartEscalationEvent, is BpmnStartConditionalEvent, is BpmnStartErrorEvent,
            is BpmnStartMessageEvent, is BpmnStartSignalEvent, is BpmnStartTimerEvent, is BpmnEndEvent,
            is BpmnEndTerminateEvent, is BpmnEndEscalationEvent, is BpmnBoundaryCancelEvent, is BpmnBoundaryCompensationEvent,
            is BpmnBoundaryConditionalEvent, is BpmnBoundaryEscalationEvent, is BpmnBoundaryMessageEvent, is BpmnBoundaryErrorEvent,
            is BpmnBoundarySignalEvent, is BpmnBoundaryTimerEvent, is BpmnEndErrorEvent,
            is BpmnEndCancelEvent, is BpmnIntermediateTimerCatchingEvent, is BpmnIntermediateMessageCatchingEvent,
            is BpmnIntermediateSignalCatchingEvent, is BpmnIntermediateConditionalCatchingEvent, is BpmnIntermediateNoneThrowingEvent,
            is BpmnIntermediateSignalThrowingEvent, is BpmnIntermediateEscalationThrowingEvent -> BoundsElement(
                0.0f,
                0.0f,
                30.0f,
                30.0f
            )
            is BpmnExclusiveGateway, is BpmnParallelGateway, is BpmnInclusiveGateway, is BpmnEventGateway -> BoundsElement(
                0.0f,
                0.0f,
                40.0f,
                40.0f
            )
            else -> BoundsElement(0.0f, 0.0f, 100.0f, 80.0f)
        }
    }

    private fun generateBpmnId(): BpmnElementId {
        return BpmnElementId("sid-" + UUID.randomUUID().toString())
    }
}
