package com.renaisn.reader.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.renaisn.reader.R
import com.renaisn.reader.constant.AppConst.appInfo
import com.renaisn.reader.help.AppUpdate
import com.renaisn.reader.lib.dialogs.alert
import com.renaisn.reader.lib.dialogs.selector
import com.renaisn.reader.ui.widget.dialog.TextDialog
import com.renaisn.reader.ui.widget.dialog.WaitDialog
import com.renaisn.reader.utils.*
import splitties.init.appCtx

class AboutFragment : PreferenceFragmentCompat() {

    private val qqGroups = linkedMapOf(
        Pair("(QQ群1)805192012", "6GlFKjLeIk5RhQnR3PNVDaKB6j10royo"),
        Pair("(QQ群2)773736122", "5Bm5w6OgLupXnICbYvbgzpPUgf0UlsJF"),
        Pair("(QQ群3)981838750", "g_Sgmp2nQPKqcZQ5qPcKLHziwX_mpps9"),
        Pair("(QQ群4)256929088", "czEJPLDnT4Pd9SKQ6RoRVzKhDxLchZrO"),
        Pair("(QQ群5)811843556", "zKZ2UYGZ7o5CzcA6ylxzlqi21si_iqaX"),
        Pair("(QQ群6)686910436", "reOUwIDDJXoTZQxXTr8VOEUu5IQLeME2"),
        Pair("(QQ群7)15987187", "S2g2TMD0LGd3sefUADd1AbyPEW2o2XfC"),
        Pair("(QQ群8)1079926194", "gg2qFH8q9IPFaCHV3H7CqCN-YljvazE1"),
        Pair("(QQ群9)892108780", "Ci_O3aysKjEBfplOWeCud-rxl71TjU2Q"),
        Pair("(QQ群10)812720266", "oW9ksY0sAWUEq0hfM5irN5aOdvKVgMEE")
    )

    private val qqChannel =
        "https://qun.qq.com/qqweb/qunpro/share?_wv=3&_wwv=128&inviteCode=25d870&from=246610&biz=ka"

    private val waitDialog by lazy {
        WaitDialog(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
        findPreference<Preference>("update_log")?.summary =
            "${getString(R.string.version)} ${appInfo.versionName}"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "contributors" -> openUrl(R.string.contributors_url)
            "update_log" -> showMdFile("updateLog.md")
            //"check_update" -> checkUpdate()
            "mail" -> requireContext().sendMail(getString(R.string.email))
            "sourceRuleSummary" -> openUrl(R.string.source_rule_url)
            "git" -> openUrl(R.string.this_github_url)
            "home_page" -> openUrl(R.string.home_page_url)
            "license" -> openUrl(R.string.license_url)
            "disclaimer" -> showMdFile("disclaimer.md")
            "qq" -> showQqGroups()
            "gzGzh" -> requireContext().sendToClip(getString(R.string.renaisn_gzh))
            "crashLog" -> showCrashLogs()
            "qqChannel" -> context?.openUrl(qqChannel)
            "tg" -> openUrl(R.string.tg_url)
            "discord" -> openUrl(R.string.discord_url)
        }
        return super.onPreferenceTreeClick(preference)
    }

    @Suppress("SameParameterValue")
    private fun openUrl(@StringRes addressID: Int) {
        requireContext().openUrl(getString(addressID))
    }

    /**
     * 显示md文件
     */
    private fun showMdFile(FileName: String) {
        val mdText = String(requireContext().assets.open(FileName).readBytes())
        showDialogFragment(TextDialog(mdText, TextDialog.Mode.MD))
    }

    /**
     * 检测更新
     */
    private fun checkUpdate() {
        waitDialog.show()
        AppUpdate.gitHubUpdate?.run {
            check(lifecycleScope)
                .onSuccess {
                    showDialogFragment(
                        UpdateDialog(it)
                    )
                }.onError {
                    appCtx.toastOnUi("${getString(R.string.check_update)}\n${it.localizedMessage}")
                }.onFinally {
                    waitDialog.hide()
                }
        }
    }

    /**
     * 显示qq群
     */
    private fun showQqGroups() {
        alert(titleResource = R.string.join_qq_group) {
            val names = arrayListOf<String>()
            qqGroups.forEach {
                names.add(it.key)
            }
            items(names) { _, index ->
                qqGroups[names[index]]?.let {
                    if (!joinQQGroup(it)) {
                        requireContext().sendToClip(it)
                    }
                }
            }
        }
    }

    /**
     * 加入qq群
     */
    private fun joinQQGroup(key: String): Boolean {
        val intent = Intent()
        intent.data =
            Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D$key")
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        kotlin.runCatching {
            startActivity(intent)
            return true
        }.onFailure {
            toastOnUi("添加失败,请手动添加")
        }
        return false
    }

    private fun showCrashLogs() {
        context?.externalCacheDir?.let { exCacheDir ->
            val crashDir = exCacheDir.getFile("crash")
            val crashLogs = crashDir.listFiles()
            val crashLogNames = arrayListOf<String>()
            crashLogs?.forEach {
                crashLogNames.add(it.name)
            }
            context?.selector(R.string.crash_log, crashLogNames) { _, select ->
                crashLogs?.getOrNull(select)?.let { logFile ->
                    showDialogFragment(TextDialog(logFile.readText()))
                }
            }
        }
    }

}