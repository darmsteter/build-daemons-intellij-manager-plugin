package com.github.darmsteter.builddaemonsintellijmanagerplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.sun.management.OperatingSystemMXBean
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.concurrent.fixedRateTimer

class MyToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow()
        val content = ContentFactory.getInstance()
            .createContent(myToolWindow.getContent(), "Build Daemons Intellij Manager", false)
        toolWindow.contentManager.addContent(content)
    }

    class MyToolWindow {
        private val panel = JPanel()
        private val layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        private val daemonsLabel = JLabel("Daemons:")
        private var daemonsInfo = StringBuilder()

        init {
            panel.layout = layout

            val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
            val decimalFormat = DecimalFormat("#.##")

            val totalRAMLabel = JLabel("Total RAM: ${getTotalRAM()} MB")
            val freeRAMLabel = JLabel()

            panel.add(totalRAMLabel)
            panel.add(freeRAMLabel)
            panel.add(daemonsLabel)

            fixedRateTimer(period = 5000) {
                val freePhysicalMemorySize = osBean.freePhysicalMemorySize
                val freeMemoryInMB = freePhysicalMemorySize.toDouble() / (1024 * 1024)
                val freeRAMText = "Free RAM: ${decimalFormat.format(freeMemoryInMB)} MB"
                SwingUtilities.invokeLater {
                    freeRAMLabel.text = freeRAMText
                    updateDaemonsInfo()
                }
            }
        }

        private fun updateDaemonsInfo() {
            val process = Runtime.getRuntime().exec("jps")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val daemons = reader.readText().lines().filter { it.contains("Daemon") }

            daemonsInfo.clear()
            for (daemon in daemons) {
                daemonsInfo.append(daemon).append("\n")
            }

            SwingUtilities.invokeLater {
                daemonsLabel.text = "Daemons:\n$daemonsInfo"
                panel.revalidate()
                panel.repaint()
            }
        }

        fun getContent() = panel

        private fun getTotalRAM(): Long {
            val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
            return osBean.totalPhysicalMemorySize / (1024 * 1024)
        }
    }
}