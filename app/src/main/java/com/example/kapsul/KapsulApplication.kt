package com.example.kapsul

import android.app.Application
import com.example.kapsul.data.AttachmentStore
import com.example.kapsul.data.KapsulDatabase
import com.example.kapsul.data.NoteRepository
import com.example.kapsul.data.SettingsRepository
import com.example.kapsul.intelligence.LocalIntelligence
import com.example.kapsul.intelligence.NoteIntelligence
import com.example.kapsul.reminders.ReminderNotifications
import com.example.kapsul.reminders.ReminderScheduler

class KapsulApplication : Application() {
    val database by lazy { KapsulDatabase.create(this) }
    val attachmentStore by lazy { AttachmentStore(this) }
    val repository by lazy { NoteRepository(database.noteDao(), attachmentStore) }
    val settings by lazy { SettingsRepository(this) }
    val intelligence: NoteIntelligence by lazy { LocalIntelligence() }
    val reminders by lazy { ReminderScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.createChannels(this)
    }
}
