package com.github.darmsteter.builddaemonsintellijmanagerplugin

import com.intellij.openapi.actionSystem.AnAction
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class DaemonTable(
    private val daemonsManagerAction: DaemonsManagerAction
) : JTable() {
    init {
        val model = DefaultTableModel(arrayOf("Daemon name", "RAM", "CPU"), 0)
        this.model = model

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1) {
                    val selectedRow = rowAtPoint(e.point)
                    if (selectedRow >= 0) {
                        val daemonName = model.getValueAt(selectedRow, 0) as String
                        val location = e.point
                        daemonsManagerAction.displayDaemonActions(daemonName, location)
                    }
                }
            }
        })
    }


    fun updateData(daemonActions: Map<String, AnAction>) {
        val model = this.model as DefaultTableModel
        model.rowCount = 0

        for (daemon in daemonActions) {
            val daemonName = daemon.key
            model.addRow(arrayOf(daemonName, "2GB", "10"))
        }
    }
}