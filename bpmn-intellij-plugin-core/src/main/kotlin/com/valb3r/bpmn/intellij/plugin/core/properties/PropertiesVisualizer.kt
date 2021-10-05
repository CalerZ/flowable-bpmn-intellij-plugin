package com.valb3r.bpmn.intellij.plugin.core.properties

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.PropertyUpdateWithId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.ValueInArray
import com.valb3r.bpmn.intellij.plugin.core.events.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.IndexUiOnlyValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.ui.components.FirstColumnReadOnlyModel
import java.util.*
import javax.swing.JComponent
import javax.swing.JTable

private val visualizer = Collections.synchronizedMap(WeakHashMap<Project,  PropertiesVisualizer>())

interface TextValueAccessor {
    val text: String
    val component: JComponent
}

interface SelectedValueAccessor {
    val isSelected: Boolean
    val component: JComponent
}

fun newPropertiesVisualizer(
                            project: Project,
                            table: JTable,
                            dropDownFactory: (id: BpmnElementId, type: PropertyType, value: String, availableValues: Set<String>) -> TextValueAccessor,
                            classEditorFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
                            editorFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
                            textFieldFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
                            checkboxFieldFactory: (id: BpmnElementId, type: PropertyType, value: Boolean) -> SelectedValueAccessor): PropertiesVisualizer {
    val newVisualizer = PropertiesVisualizer(project, table, dropDownFactory, classEditorFactory, editorFactory, textFieldFactory, checkboxFieldFactory)
    visualizer[project] = newVisualizer
    return newVisualizer
}

fun propertiesVisualizer(project: Project): PropertiesVisualizer {
    return visualizer[project]!!
}

class PropertiesVisualizer(
        private val project: Project,
        val table: JTable,
        val dropDownFactory: (id: BpmnElementId, type: PropertyType, value: String, availableValues: Set<String>) -> TextValueAccessor,
        val classEditorFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
        val editorFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
        private val textFieldFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
        private val checkboxFieldFactory: (id: BpmnElementId, type: PropertyType, value: Boolean) -> SelectedValueAccessor) {

    // Using order as ID property change should fire last for this view, otherwise other property change values
    // will use wrong ID as an anchor
    // Listeners with their order
    private var listenersForCurrentView: MutableMap<Int, MutableList<() -> Unit>> = mutableMapOf()

    @Synchronized
    fun clear() {
        notifyDeFocusElement()

        // drop and re-create table model
        val model = FirstColumnReadOnlyModel()
        model.addColumn("")
        model.addColumn("")
        table.model = model
        table.columnModel.getColumn(1).preferredWidth = 500
        model.fireTableDataChanged()
    }

    @Synchronized
    fun visualize(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId) {
        notifyDeFocusElement()

        // drop and re-create table model
        val model = FirstColumnReadOnlyModel()
        model.addColumn("")
        model.addColumn("")
        table.model = model
        table.columnModel.getColumn(1).preferredWidth = 500

        val groupedEntries = state[bpmnElementId]?.view()?.entries
            ?.groupBy { it.key.controlInGroupCaption }
            ?.toSortedMap(Comparator.comparingInt { it?.length ?: 0 }) ?: emptyMap()
        
        for ((groupId, entries) in groupedEntries) {
            if (null != groupId) {
                // todo add group caption
            }

            entries
                .flatMap { it.value.map { v -> Pair(it.key, v) } }
                .sortedBy { extractIndex(it.second) }
                .forEach {
                    when(it.first.valueType) {
                        STRING -> model.addRow(arrayOf(it.first.caption, buildTextField(state, bpmnElementId, it.first, it.second)))
                        BOOLEAN -> model.addRow(arrayOf(it.first.caption, buildCheckboxField(bpmnElementId, it.first, it.second)))
                        CLASS -> model.addRow(arrayOf(it.first.caption, buildClassField(state, bpmnElementId, it.first, it.second)))
                        EXPRESSION -> model.addRow(arrayOf(it.first.caption, buildExpressionField(state, bpmnElementId, it.first, it.second)))
                        ATTACHED_SEQUENCE_SELECT -> model.addRow(arrayOf(it.first.caption, buildDropDownSelectFieldForTargettedIds(state, bpmnElementId, it.first, it.second)))
                    }
                }
        }
        
        model.fireTableDataChanged()
    }

    private fun notifyDeFocusElement() {
        // Fire de-focus to move changes to memory (Using order as ID property), component listeners doesn't seem to work with EditorTextField
        listenersForCurrentView.toSortedMap().flatMap { it.value }.forEach { it() }
        listenersForCurrentView.clear()
    }

    private fun buildTextField(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue = lastStringValueFromRegistry(bpmnElementId, type) ?: extractString(value)
        val field = textFieldFactory.invoke(bpmnElementId, type, fieldValue)
        val initialValue = field.text

        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.text) {
                emitStringUpdateWithCascadeIfNeeded(
                        state,
                        StringValueUpdatedEvent(
                                bpmnElementId,
                                type,
                                field.text,
                                if (type.cascades) initialValue else null,
                                if (type == PropertyType.ID) BpmnElementId(field.text) else null,
                                extractIndex(value)
                        )
                )
            }
        }
        return field.component
    }

    private fun buildCheckboxField(bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue =  lastBooleanValueFromRegistry(bpmnElementId, type) ?: extractBoolean(value)
        val field = checkboxFieldFactory.invoke(bpmnElementId, type, fieldValue)
        val initialValue = field.isSelected

        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.isSelected) {
                updateEventsRegistry(project).addPropertyUpdateEvent(BooleanValueUpdatedEvent(bpmnElementId, type, field.isSelected))
            }
        }
        return field.component
    }

    private fun buildClassField(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue = lastStringValueFromRegistry(bpmnElementId, type) ?: extractString(value)
        val field = classEditorFactory(bpmnElementId, type, fieldValue)
        addEditorTextListener(state, field, bpmnElementId, type)
        return field.component
    }

    private fun buildExpressionField(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue =  lastStringValueFromRegistry(bpmnElementId, type) ?: extractString(value)
        val field = editorFactory(bpmnElementId, type, "\"${fieldValue}\"")
        addEditorTextListener(state, field, bpmnElementId, type)
        return field.component
    }

    private fun buildDropDownSelectFieldForTargettedIds(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue =  lastStringValueFromRegistry(bpmnElementId, type) ?: extractString(value)
        val field = dropDownFactory(bpmnElementId, type, fieldValue, findCascadeTargetIds(bpmnElementId, type, state))
        addEditorTextListener(state, field, bpmnElementId, type)
        return field.component
    }

    private fun findCascadeTargetIds(forId: BpmnElementId, type: PropertyType, state: Map<BpmnElementId, PropertyTable>): Set<String> {
        if (null == type.updatedBy) {
            throw IllegalArgumentException("Type $type should be cascadable")
        }

        val result = mutableSetOf("")

        state.forEach { (_, props) ->
            props.forEach { k, v ->
                if (k == type.updatedBy && props[PropertyType.SOURCE_REF]?.value == forId.id) {
                    props[PropertyType.ID]?.value?.let { result += it as String }
                }
            }
        }

        return result
    }

    private fun addEditorTextListener(state: Map<BpmnElementId, PropertyTable>, field: TextValueAccessor, bpmnElementId: BpmnElementId, type: PropertyType) {
        val initialValue = field.text
        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.text) {
                emitStringUpdateWithCascadeIfNeeded(state, StringValueUpdatedEvent(bpmnElementId, type, removeQuotes(field.text)))
            }
        }
    }

    private fun emitStringUpdateWithCascadeIfNeeded(state: Map<BpmnElementId, PropertyTable>, event: StringValueUpdatedEvent) {
        val cascades = mutableListOf<Event>()
        if (null != event.referencedValue) {
            state.forEach { (id, props) ->
                props.filter { k, _ -> k.updatedBy == event.property }.filter { it.second.value == event.referencedValue }.forEach { prop ->
                    cascades += StringValueUpdatedEvent(id, prop.first, event.newValue, event.referencedValue, null)
                }
            }
        }
        if (event.property.indexCascades) {
            state[event.bpmnElementId]?.view()?.filter { it.key.indexInGroupArrayName == event.property.indexInGroupArrayName && it.key != event.property }?.forEach { (k, _) ->
                cascades += IndexUiOnlyValueUpdatedEvent(event.bpmnElementId, k, event.newValue)
            }
        }

        updateEventsRegistry(project).addEvents(listOf(event) + cascades)
    }

    private fun removeQuotes(value: String): String {
        return value.replace("^\"".toRegex(), "").replace("\"$".toRegex(), "")
    }

    private fun lastStringValueFromRegistry(bpmnElementId: BpmnElementId, type: PropertyType): String? {
        return (updateEventsRegistry(project).currentPropertyUpdateEventList(bpmnElementId)
                .map { it.event }
                .filter { bpmnElementId == it.bpmnElementId && it.property.id == type.id }
                .lastOrNull { it is StringValueUpdatedEvent } as StringValueUpdatedEvent?)
                ?.newValue
    }

    private fun lastBooleanValueFromRegistry(bpmnElementId: BpmnElementId, type: PropertyType): Boolean? {
        // It is not possible to handle boolean cascades due to ambiguities
        return (updateEventsRegistry(project).currentPropertyUpdateEventList(bpmnElementId)
                .map { it.event }
                .filter { it.property.id == type.id }
                .lastOrNull { it is BooleanValueUpdatedEvent } as BooleanValueUpdatedEvent?)
                ?.newValue
    }

    private fun extractIndex(prop: Property?): String? {
        return if (prop?.value is ValueInArray) (prop.value as ValueInArray).index else null
    }

    private fun extractString(prop: Property?): String {
        return extractValue(prop, "")
    }

    private fun extractBoolean(prop: Property?): Boolean {
        return extractValue(prop, false)
    }

    private fun <T> extractValue(prop: Property?, defaultValue: T): T {
        if (prop == null) {
            return defaultValue
        }

        return if (prop.value is ValueInArray) {
            (prop.value as ValueInArray).value as T? ?: defaultValue
        } else {
            prop.value as T? ?: defaultValue
        }
    }
}