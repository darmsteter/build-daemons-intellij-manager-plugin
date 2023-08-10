package com.github.darmsteter.builddaemonsintellijmanagerplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import javax.swing.SwingUtilities
import kotlin.concurrent.fixedRateTimer

class MyToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance()
            .createContent(myToolWindow.getContent(), "Build Daemons Intellij Manager", false)
        toolWindow.contentManager.addContent(content)
    }

    class MyToolWindow(private val toolWindow: ToolWindow) {
        private val panel = JBPanel<JBPanel<*>>().apply {
            val totalRAMLabel = JBLabel("Total RAM: ${getTotalRAM()} MB")
            add(totalRAMLabel)
        }

        init {
            val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
            val decimalFormat = DecimalFormat("#.##")

            val freeRAMLabel = JBLabel()
            panel.add(freeRAMLabel)

            fixedRateTimer(period = 5000) {
                val freePhysicalMemorySize = osBean.freePhysicalMemorySize
                val freeMemoryInMB = freePhysicalMemorySize.toDouble() / (1024 * 1024)
                val freeRAMText = "Free RAM: ${decimalFormat.format(freeMemoryInMB)} MB"
                SwingUtilities.invokeLater {
                    freeRAMLabel.text = freeRAMText
                    panel.revalidate()
                    panel.repaint()
                }
            }
        }

        fun getContent() = panel

        private fun getTotalRAM(): Long {
            val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
            return osBean.totalPhysicalMemorySize / (1024 * 1024)
        }
    }
}
