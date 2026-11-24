package eu.kanade.tachiyomi.ui.setting.controllers

import eu.kanade.tachiyomi.ui.setting.SettingsComposeController
import yori.presentation.settings.ComposableSettings
import yori.presentation.settings.screen.SettingsAdvancedScreen

class SettingsAdvancedController : SettingsComposeController() {
    override fun getComposableSettings(): ComposableSettings = SettingsAdvancedScreen
}
