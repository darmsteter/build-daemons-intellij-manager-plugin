package com.github.darmsteter.builddaemonsintellijmanagerplugin

import com.intellij.openapi.actionSystem.AnAction
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class DaemonActionsTable : JTable() {
    init {
        val model = DefaultTableModel(arrayOf("Daemon name", "RAM", "CPU"), 0)
        this.model = model
    }

    fun updateData(daemonActions: Map<String, AnAction>) {
        val model = this.model as DefaultTableModel
        model.setRowCount(0)

        for (daemon in daemonActions) {
            val daemonName = daemon.key
            model.addRow(arrayOf(daemonName, "2GB", "10"))
        }
    }
}