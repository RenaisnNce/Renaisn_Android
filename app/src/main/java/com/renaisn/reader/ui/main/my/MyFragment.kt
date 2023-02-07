package com.renaisn.reader.ui.main.my

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import com.renaisn.reader.R
import com.renaisn.reader.base.BaseFragment
import com.renaisn.reader.constant.EventBus
import com.renaisn.reader.constant.PreferKey
import com.renaisn.reader.databinding.FragmentMyConfigBinding
import com.renaisn.reader.help.config.ThemeConfig
import com.renaisn.reader.lib.dialogs.selector
import com.renaisn.reader.lib.prefs.NameListPreference
import com.renaisn.reader.lib.prefs.PreferenceCategory
import com.renaisn.reader.lib.prefs.SwitchPreference
import com.renaisn.reader.lib.prefs.fragment.PreferenceFragment
import com.renaisn.reader.lib.theme.primaryColor
import com.renaisn.reader.service.WebService
import com.renaisn.reader.ui.about.AboutActivity
import com.renaisn.reader.ui.about.DonateActivity
import com.renaisn.reader.ui.about.ReadRecordActivity
import com.renaisn.reader.ui.book.bookmark.AllBookmarkActivity
import com.renaisn.reader.ui.book.source.manage.BookSourceActivity
import com.renaisn.reader.ui.config.ConfigActivity
import com.renaisn.reader.ui.config.ConfigTag
import com.renaisn.reader.ui.replace.ReplaceRuleActivity
import com.renaisn.reader.ui.widget.dialog.TextDialog
import com.renaisn.reader.utils.*
import com.renaisn.reader.utils.viewbindingdelegate.viewBinding

class MyFragment : BaseFragment(R.layout.fragment_my_config) {

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        val fragmentTag = "prefFragment"
        var preferenceFragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (preferenceFragment == null) preferenceFragment = MyPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.pre_fragment, preferenceFragment, fragmentTag).commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
//        menuInflater.inflate(R.menu.main_my, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_help -> {
                val text = String(requireContext().assets.open("help/appHelp.md").readBytes())
                showDialogFragment(TextDialog(text, TextDialog.Mode.MD))
            }
        }
    }

    /**
     * 配置
     */
    class MyPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_main)

            findPreference<NameListPreference>(PreferKey.themeMode)?.let {
                it.setOnPreferenceChangeListener { _, _ ->
                    view?.post { ThemeConfig.applyDayNight(requireContext()) }
                    true
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?, key: String?
        ) {
            when (key) {
                "recordLog" -> LogUtils.upLevel()
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "bookSourceManage" -> startActivity<BookSourceActivity>()
                "bookmark" -> startActivity<AllBookmarkActivity>()
                "setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.OTHER_CONFIG)
                }
                "web_dav_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.BACKUP_CONFIG)
                }
                "readRecord" -> startActivity<ReadRecordActivity>()
            }
            return super.onPreferenceTreeClick(preference)
        }


    }
}