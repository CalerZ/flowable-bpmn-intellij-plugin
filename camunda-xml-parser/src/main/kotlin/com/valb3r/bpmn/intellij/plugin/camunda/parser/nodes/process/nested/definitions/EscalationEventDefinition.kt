package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.definitions

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class EscalationEventDefinition(
    @JacksonXmlProperty(isAttribute = true) val escalationRef: String? = null
)