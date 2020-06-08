package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import java.awt.geom.Point2D
import javax.swing.Icon

abstract class IconAnchorElement(
        override val elementId: DiagramElementId,
        currentLocation: Point2D.Float,
        state: RenderState
): AnchorElement(elementId, currentLocation, state) {

    override fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val icon = icon()
        val active = isActive()
        val rect = currentRect(ctx.canvas.camera)
        val area = ctx.canvas.drawIcon(Point2D.Float(rect.x, rect.y), icon, if (active) Colors.SELECTED_COLOR.color else null)

        return mutableMapOf(elementId to AreaWithZindex(area, AreaType.POINT))
    }

    protected abstract fun icon(): Icon
}