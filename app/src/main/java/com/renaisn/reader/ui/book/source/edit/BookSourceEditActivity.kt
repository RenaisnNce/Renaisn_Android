package com.renaisn.reader.ui.book.source.edit

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.renaisn.reader.BuildConfig
import com.renaisn.reader.R
import com.renaisn.reader.base.VMBaseActivity
import com.renaisn.reader.constant.BookSourceType
import com.renaisn.reader.data.appDb
import com.renaisn.reader.data.entities.BookSource
import com.renaisn.reader.data.entities.rule.*
import com.renaisn.reader.databinding.ActivityBookSourceEditBinding
import com.renaisn.reader.databinding.DialogEditTextBinding
import com.renaisn.reader.help.config.LocalConfig
import com.renaisn.reader.lib.dialogs.SelectItem
import com.renaisn.reader.lib.dialogs.alert
import com.renaisn.reader.lib.dialogs.selector
import com.renaisn.reader.lib.theme.accentColor
import com.renaisn.reader.lib.theme.backgroundColor
import com.renaisn.reader.lib.theme.primaryColor
import com.renaisn.reader.ui.book.source.debug.BookSourceDebugActivity
import com.renaisn.reader.ui.document.HandleFileContract
import com.renaisn.reader.ui.login.SourceLoginActivity
import com.renaisn.reader.ui.qrcode.QrCodeResult
import com.renaisn.reader.ui.widget.dialog.TextDialog
import com.renaisn.reader.ui.widget.dialog.UrlOptionDialog
import com.renaisn.reader.ui.widget.keyboard.KeyboardToolPop
import com.renaisn.reader.ui.widget.text.EditEntity
import com.renaisn.reader.utils.*
import com.renaisn.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookSourceEditActivity :
    VMBaseActivity<ActivityBookSourceEditBinding, BookSourceEditViewModel>(false),
    KeyboardToolPop.CallBack {

    override val binding by viewBinding(ActivityBookSourceEditBinding::inflate)
    override val viewModel by viewModels<BookSourceEditViewModel>()

    private val adapter by lazy { BookSourceEditAdapter() }
    private val sourceEntities: ArrayList<EditEntity> = ArrayList()
    private val searchEntities: ArrayList<EditEntity> = ArrayList()
    private val exploreEntities: ArrayList<EditEntity> = ArrayList()
    private val infoEntities: ArrayList<EditEntity> = ArrayList()
    private val tocEntities: ArrayList<EditEntity> = ArrayList()
    private val contentEntities: ArrayList<EditEntity> = ArrayList()
    private val reviewEntities: ArrayList<EditEntity> = ArrayList()
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        viewModel.importSource(it) { source ->
            upSourceView(source)
        }
    }
    private val selectDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            if (uri.isContentScheme()) {
                sendText(uri.toString())
            } else {
                sendText(uri.path.toString())
            }
        }
    }

    private val softKeyboardTool by lazy {
        KeyboardToolPop(this, this, binding.root, this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        softKeyboardTool.attachToWindow(window)
        initView()
        viewModel.initData(intent) {
            upSourceView()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (!LocalConfig.ruleHelpVersionIsLast) {
            showHelp("ruleHelp")
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible = !getSource().loginUrl.isNullOrBlank()
        menu.findItem(R.id.menu_auto_complete)?.isChecked = viewModel.autoComplete
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> getSource().let { source ->
                if (!source.equal(viewModel.bookSource ?: BookSource())) {
                    source.lastUpdateTime = System.currentTimeMillis()
                }
                if (checkSource(source)) {
                    viewModel.save(source) { setResult(Activity.RESULT_OK); finish() }
                }
            }
            R.id.menu_debug_source -> getSource().let { source ->
                if (checkSource(source)) {
                    viewModel.save(source) {
                        startActivity<BookSourceDebugActivity> {
                            putExtra("key", source.bookSourceUrl)
                        }
                    }
                }
            }
            R.id.menu_clear_cookie -> viewModel.clearCookie(getSource().bookSourceUrl)
            R.id.menu_auto_complete -> viewModel.autoComplete = !viewModel.autoComplete
            R.id.menu_copy_source -> sendToClip(GSON.toJson(getSource()))
            R.id.menu_paste_source -> viewModel.pasteSource { upSourceView(it) }
            R.id.menu_qr_code_camera -> qrCodeResult.launch()
            R.id.menu_share_str -> share(GSON.toJson(getSource()))
            R.id.menu_share_qr -> shareWithQr(
                GSON.toJson(getSource()),
                getString(R.string.share_book_source),
                ErrorCorrectionLevel.L
            )
            R.id.menu_help -> showHelp("ruleHelp")
            R.id.menu_login -> getSource().let { source ->
                if (checkSource(source)) {
                    viewModel.save(source) {
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "bookSource")
                            putExtra("key", source.bookSourceUrl)
                        }
                    }
                }
            }
            R.id.menu_set_source_variable -> setSourceVariable()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        if (!com.renaisn.reader.BuildConfig.DEBUG) {
            binding.cbIsEnableReview.gone()
            binding.tabLayout.removeTabAt(6)
        }
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.tabLayout.setBackgroundColor(backgroundColor)
        binding.tabLayout.setSelectedTabIndicatorColor(accentColor)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                setEditEntities(tab?.position)
            }
        })
    }

    override fun finish() {
        val source = getSource()
        val source2 = viewModel.bookSource ?: BookSource().apply {
            enabledExplore = true
            enabledCookieJar = true
            enabledReview = true
        }
        if (!source.equal(source2)) {
            alert(R.string.exit) {
                setMessage(R.string.exit_no_save)
                positiveButton(R.string.yes)
                negativeButton(R.string.no) {
                    super.finish()
                }
            }
        } else {
            super.finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        softKeyboardTool.dismiss()
    }

    private fun setEditEntities(tabPosition: Int?) {
        when (tabPosition) {
            1 -> adapter.editEntities = searchEntities
            2 -> adapter.editEntities = exploreEntities
            3 -> adapter.editEntities = infoEntities
            4 -> adapter.editEntities = tocEntities
            5 -> adapter.editEntities = contentEntities
            6 -> adapter.editEntities = reviewEntities
            else -> adapter.editEntities = sourceEntities
        }
        binding.recyclerView.scrollToPosition(0)
    }

    private fun upSourceView(source: BookSource? = viewModel.bookSource) {
        source?.let {
            binding.cbIsEnable.isChecked = it.enabled
            binding.cbIsEnableExplore.isChecked = it.enabledExplore
            binding.cbIsEnableCookie.isChecked = it.enabledCookieJar ?: false
            binding.cbIsEnableReview.isChecked = it.enabledReview ?: false
            binding.spType.setSelection(
                when (it.bookSourceType) {
                    BookSourceType.file -> 3
                    BookSourceType.image -> 2
                    BookSourceType.audio -> 1
                    else -> 0
                }
            )
        }
        // 基本信息
        sourceEntities.clear()
        sourceEntities.apply {
            add(EditEntity("bookSourceUrl", source?.bookSourceUrl, R.string.source_url))
            add(EditEntity("bookSourceName", source?.bookSourceName, R.string.source_name))
            add(EditEntity("bookSourceGroup", source?.bookSourceGroup, R.string.source_group))
            add(EditEntity("bookSourceComment", source?.bookSourceComment, R.string.comment))
            add(EditEntity("loginUrl", source?.loginUrl, R.string.login_url))
            add(EditEntity("loginUi", source?.loginUi, R.string.login_ui))
            add(EditEntity("loginCheckJs", source?.loginCheckJs, R.string.login_check_js))
            add(EditEntity("coverDecodeJs", source?.coverDecodeJs, R.string.cover_decode_js))
            add(EditEntity("bookUrlPattern", source?.bookUrlPattern, R.string.book_url_pattern))
            add(EditEntity("header", source?.header, R.string.source_http_header))
            add(EditEntity("variableComment", source?.variableComment, R.string.variable_comment))
            add(EditEntity("concurrentRate", source?.concurrentRate, R.string.concurrent_rate))
        }
        // 搜索
        val sr = source?.getSearchRule()
        searchEntities.clear()
        searchEntities.apply {
            add(EditEntity("searchUrl", source?.searchUrl, R.string.r_search_url))
            add(EditEntity("checkKeyWord", sr?.checkKeyWord, R.string.check_key_word))
            add(EditEntity("bookList", sr?.bookList, R.string.r_book_list))
            add(EditEntity("name", sr?.name, R.string.r_book_name))
            add(EditEntity("author", sr?.author, R.string.r_author))
            add(EditEntity("kind", sr?.kind, R.string.rule_book_kind))
            add(EditEntity("wordCount", sr?.wordCount, R.string.rule_word_count))
            add(EditEntity("lastChapter", sr?.lastChapter, R.string.rule_last_chapter))
            add(EditEntity("intro", sr?.intro, R.string.rule_book_intro))
            add(EditEntity("coverUrl", sr?.coverUrl, R.string.rule_cover_url))
            add(EditEntity("bookUrl", sr?.bookUrl, R.string.r_book_url))
        }
        // 发现
        val er = source?.getExploreRule()
        exploreEntities.clear()
        exploreEntities.apply {
            add(EditEntity("exploreUrl", source?.exploreUrl, R.string.r_find_url))
            add(EditEntity("bookList", er?.bookList, R.string.r_book_list))
            add(EditEntity("name", er?.name, R.string.r_book_name))
            add(EditEntity("author", er?.author, R.string.r_author))
            add(EditEntity("kind", er?.kind, R.string.rule_book_kind))
            add(EditEntity("wordCount", er?.wordCount, R.string.rule_word_count))
            add(EditEntity("lastChapter", er?.lastChapter, R.string.rule_last_chapter))
            add(EditEntity("intro", er?.intro, R.string.rule_book_intro))
            add(EditEntity("coverUrl", er?.coverUrl, R.string.rule_cover_url))
            add(EditEntity("bookUrl", er?.bookUrl, R.string.r_book_url))
        }
        // 详情页
        val ir = source?.getBookInfoRule()
        infoEntities.clear()
        infoEntities.apply {
            add(EditEntity("init", ir?.init, R.string.rule_book_info_init))
            add(EditEntity("name", ir?.name, R.string.r_book_name))
            add(EditEntity("author", ir?.author, R.string.r_author))
            add(EditEntity("kind", ir?.kind, R.string.rule_book_kind))
            add(EditEntity("wordCount", ir?.wordCount, R.string.rule_word_count))
            add(EditEntity("lastChapter", ir?.lastChapter, R.string.rule_last_chapter))
            add(EditEntity("intro", ir?.intro, R.string.rule_book_intro))
            add(EditEntity("coverUrl", ir?.coverUrl, R.string.rule_cover_url))
            add(EditEntity("tocUrl", ir?.tocUrl, R.string.rule_toc_url))
            add(EditEntity("canReName", ir?.canReName, R.string.rule_can_re_name))
            add(EditEntity("downloadUrls", ir?.downloadUrls, R.string.download_url_rule))
        }
        // 目录页
        val tr = source?.getTocRule()
        tocEntities.clear()
        tocEntities.apply {
            add(EditEntity("preUpdateJs", tr?.preUpdateJs, R.string.pre_update_js))
            add(EditEntity("chapterList", tr?.chapterList, R.string.rule_chapter_list))
            add(EditEntity("chapterName", tr?.chapterName, R.string.rule_chapter_name))
            add(EditEntity("chapterUrl", tr?.chapterUrl, R.string.rule_chapter_url))
            add(EditEntity("isVolume", tr?.isVolume, R.string.rule_is_volume))
            add(EditEntity("updateTime", tr?.updateTime, R.string.rule_update_time))
            add(EditEntity("isVip", tr?.isVip, R.string.rule_is_vip))
            add(EditEntity("isPay", tr?.isPay, R.string.rule_is_pay))
            add(EditEntity("nextTocUrl", tr?.nextTocUrl, R.string.rule_next_toc_url))
        }
        // 正文页
        val cr = source?.getContentRule()
        contentEntities.clear()
        contentEntities.apply {
            add(EditEntity("content", cr?.content, R.string.rule_book_content))
            add(EditEntity("nextContentUrl", cr?.nextContentUrl, R.string.rule_next_content))
            add(EditEntity("webJs", cr?.webJs, R.string.rule_web_js))
            add(EditEntity("sourceRegex", cr?.sourceRegex, R.string.rule_source_regex))
            add(EditEntity("replaceRegex", cr?.replaceRegex, R.string.rule_replace_regex))
            add(EditEntity("imageStyle", cr?.imageStyle, R.string.rule_image_style))
            add(EditEntity("imageDecode", cr?.imageDecode, R.string.rule_image_decode))
            add(EditEntity("payAction", cr?.payAction, R.string.rule_pay_action))
        }
        // 段评
        val rr = source?.getReviewRule()
        reviewEntities.clear()
        reviewEntities.apply {
            add(EditEntity("reviewUrl", rr?.reviewUrl, R.string.rule_review_url))
            add(EditEntity("avatarRule", rr?.avatarRule, R.string.rule_avatar))
            add(EditEntity("contentRule", rr?.contentRule, R.string.rule_review_content))
            add(EditEntity("postTimeRule", rr?.postTimeRule, R.string.rule_post_time))
            add(EditEntity("reviewQuoteUrl", rr?.reviewQuoteUrl, R.string.rule_review_quote))
            add(EditEntity("voteUpUrl", rr?.voteUpUrl, R.string.review_vote_up))
            add(EditEntity("voteDownUrl", rr?.voteDownUrl, R.string.review_vote_down))
            add(EditEntity("postReviewUrl", rr?.postReviewUrl, R.string.post_review_url))
            add(EditEntity("postQuoteUrl", rr?.postQuoteUrl, R.string.post_quote_url))
            add(EditEntity("deleteUrl", rr?.deleteUrl, R.string.delete_review_url))
        }
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
        setEditEntities(0)
    }

    private fun getSource(): BookSource {
        val source = viewModel.bookSource?.copy() ?: BookSource()
        source.enabled = binding.cbIsEnable.isChecked
        source.enabledExplore = binding.cbIsEnableExplore.isChecked
        source.enabledCookieJar = binding.cbIsEnableCookie.isChecked
        source.enabledReview = binding.cbIsEnableReview.isChecked
        source.bookSourceType = when (binding.spType.selectedItemPosition) {
            3 -> BookSourceType.file
            2 -> BookSourceType.image
            1 -> BookSourceType.audio
            else -> BookSourceType.default
        }
        val searchRule = SearchRule()
        val exploreRule = ExploreRule()
        val bookInfoRule = BookInfoRule()
        val tocRule = TocRule()
        val contentRule = ContentRule()
        val reviewRule = ReviewRule()
        sourceEntities.forEach {
            when (it.key) {
                "bookSourceUrl" -> source.bookSourceUrl = it.value ?: ""
                "bookSourceName" -> source.bookSourceName = it.value ?: ""
                "bookSourceGroup" -> source.bookSourceGroup = it.value
                "loginUrl" -> source.loginUrl = it.value
                "loginUi" -> source.loginUi = it.value
                "loginCheckJs" -> source.loginCheckJs = it.value
                "coverDecodeJs" -> source.coverDecodeJs = it.value
                "bookUrlPattern" -> source.bookUrlPattern = it.value
                "header" -> source.header = it.value
                "bookSourceComment" -> source.bookSourceComment = it.value
                "concurrentRate" -> source.concurrentRate = it.value
                "variableComment" -> source.variableComment = it.value
            }
        }
        searchEntities.forEach {
            when (it.key) {
                "searchUrl" -> source.searchUrl = it.value
                "checkKeyWord" -> searchRule.checkKeyWord = it.value
                "bookList" -> searchRule.bookList = it.value
                "name" -> searchRule.name =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "author" -> searchRule.author =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "kind" -> searchRule.kind =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "intro" -> searchRule.intro =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "updateTime" -> searchRule.updateTime =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "wordCount" -> searchRule.wordCount =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "lastChapter" -> searchRule.lastChapter =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "coverUrl" -> searchRule.coverUrl =
                    viewModel.ruleComplete(it.value, searchRule.bookList, 3)
                "bookUrl" -> searchRule.bookUrl =
                    viewModel.ruleComplete(it.value, searchRule.bookList, 2)
            }
        }
        exploreEntities.forEach {
            when (it.key) {
                "exploreUrl" -> source.exploreUrl = it.value
                "bookList" -> exploreRule.bookList = it.value
                "name" -> exploreRule.name =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "author" -> exploreRule.author =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "kind" -> exploreRule.kind =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "intro" -> exploreRule.intro =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "updateTime" -> exploreRule.updateTime =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "wordCount" -> exploreRule.wordCount =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "lastChapter" -> exploreRule.lastChapter =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "coverUrl" -> exploreRule.coverUrl =
                    viewModel.ruleComplete(it.value, exploreRule.bookList, 3)
                "bookUrl" -> exploreRule.bookUrl =
                    viewModel.ruleComplete(it.value, exploreRule.bookList, 2)
            }
        }
        infoEntities.forEach {
            when (it.key) {
                "init" -> bookInfoRule.init = it.value
                "name" -> bookInfoRule.name = viewModel.ruleComplete(it.value, bookInfoRule.init)
                "author" -> bookInfoRule.author =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "kind" -> bookInfoRule.kind =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "intro" -> bookInfoRule.intro =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "updateTime" -> bookInfoRule.updateTime =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "wordCount" -> bookInfoRule.wordCount =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "lastChapter" -> bookInfoRule.lastChapter =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "coverUrl" -> bookInfoRule.coverUrl =
                    viewModel.ruleComplete(it.value, bookInfoRule.init, 3)
                "tocUrl" -> bookInfoRule.tocUrl =
                    viewModel.ruleComplete(it.value, bookInfoRule.init, 2)
                "canReName" -> bookInfoRule.canReName = it.value
                "downloadUrls" -> bookInfoRule.downloadUrls =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
            }
        }
        tocEntities.forEach {
            when (it.key) {
                "preUpdateJs" -> tocRule.preUpdateJs = it.value
                "chapterList" -> tocRule.chapterList = it.value
                "chapterName" -> tocRule.chapterName =
                    viewModel.ruleComplete(it.value, tocRule.chapterList)
                "chapterUrl" -> tocRule.chapterUrl =
                    viewModel.ruleComplete(it.value, tocRule.chapterList, 2)
                "isVolume" -> tocRule.isVolume = it.value
                "updateTime" -> tocRule.updateTime = it.value
                "isVip" -> tocRule.isVip = it.value
                "isPay" -> tocRule.isPay = it.value
                "nextTocUrl" -> tocRule.nextTocUrl =
                    viewModel.ruleComplete(it.value, tocRule.chapterList, 2)
            }
        }
        contentEntities.forEach {
            when (it.key) {
                "content" -> contentRule.content =
                    viewModel.ruleComplete(it.value)
                "nextContentUrl" -> contentRule.nextContentUrl =
                    viewModel.ruleComplete(it.value, type = 2)
                "webJs" -> contentRule.webJs = it.value
                "sourceRegex" -> contentRule.sourceRegex = it.value
                "replaceRegex" -> contentRule.replaceRegex = it.value
                "imageStyle" -> contentRule.imageStyle = it.value
                "imageDecode" -> contentRule.imageDecode = it.value
                "payAction" -> contentRule.payAction = it.value
            }
        }
        reviewEntities.forEach {
            when (it.key) {
                "reviewUrl" -> reviewRule.reviewUrl = it.value
                "avatarRule" -> reviewRule.avatarRule =
                    viewModel.ruleComplete(it.value, reviewRule.reviewUrl, 3)
                "contentRule" -> reviewRule.contentRule =
                    viewModel.ruleComplete(it.value, reviewRule.reviewUrl)
                "postTimeRule" -> reviewRule.postTimeRule =
                    viewModel.ruleComplete(it.value, reviewRule.reviewUrl)
                "reviewQuoteUrl" -> reviewRule.reviewQuoteUrl =
                    viewModel.ruleComplete(it.value, reviewRule.reviewUrl, 2)
                "voteUpUrl" -> reviewRule.voteUpUrl = it.value
                "voteDownUrl" -> reviewRule.voteDownUrl = it.value
                "postReviewUrl" -> reviewRule.postReviewUrl = it.value
                "postQuoteUrl" -> reviewRule.postQuoteUrl = it.value
                "deleteUrl" -> reviewRule.deleteUrl = it.value
            }
        }
        source.ruleSearch = searchRule
        source.ruleExplore = exploreRule
        source.ruleBookInfo = bookInfoRule
        source.ruleToc = tocRule
        source.ruleContent = contentRule
        source.ruleReview = reviewRule
        return source
    }

    private fun checkSource(source: BookSource): Boolean {
        if (source.bookSourceUrl.isBlank() || source.bookSourceName.isBlank()) {
            toastOnUi(R.string.non_null_name_url)
            return false
        }
        return true
    }

    private fun alertGroups() {
        launch {
            val groups = withContext(IO) {
                appDb.bookSourceDao.allGroups
            }
            selector(groups) { _, s, _ ->
                sendText(s)
            }
        }
    }

    override fun helpActions(): List<SelectItem<String>> {
        val helpActions = arrayListOf(
            SelectItem("插入URL参数", "urlOption"),
            SelectItem("书源教程", "ruleHelp"),
            SelectItem("js教程", "jsHelp"),
            SelectItem("正则教程", "regexHelp"),
        )
        val view = window.decorView.findFocus()
        if (view is EditText) {
            when (view.getTag(R.id.tag)) {
                "bookSourceGroup" -> {
                    helpActions.add(
                        SelectItem("插入分组", "addGroup")
                    )
                }
                else -> {
                    helpActions.add(
                        SelectItem("选择文件", "selectFile")
                    )
                }
            }
        }
        return helpActions
    }

    override fun onHelpActionSelect(action: String) {
        when (action) {
            "addGroup" -> alertGroups()
            "urlOption" -> UrlOptionDialog(this) { sendText(it) }.show()
            "ruleHelp" -> showHelp("ruleHelp")
            "jsHelp" -> showHelp("jsHelp")
            "regexHelp" -> showHelp("regexHelp")
            "selectFile" -> selectDoc.launch {
                mode = HandleFileContract.FILE
            }
        }
    }

    override fun sendText(text: String) {
        if (text.isBlank()) return
        val view = window.decorView.findFocus()
        if (view is EditText) {
            val start = view.selectionStart
            val end = view.selectionEnd
            val edit = view.editableText//获取EditText的文字
            if (start < 0 || start >= edit.length) {
                edit.append(text)
            } else {
                edit.replace(start, end, text)//光标所在位置插入文字
            }
        }
    }

    private fun showHelp(fileName: String) {
        //显示目录help下的帮助文档
        val mdText = String(assets.open("help/${fileName}.md").readBytes())
        showDialogFragment(TextDialog(mdText, TextDialog.Mode.MD))
    }

    private fun setSourceVariable() {
        launch {
            val source = viewModel.bookSource
            if (source == null) {
                toastOnUi("书源不存在")
                return@launch
            }
            val variable = withContext(IO) { source.getVariable() }
            alert(R.string.set_source_variable) {
                setMessage(source.getDisplayVariableComment("源变量可在js中通过source.getVariable()获取"))
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = "source variable"
                    editView.setText(variable)
                }
                customView { alertBinding.root }
                okButton {
                    viewModel.bookSource?.setVariable(alertBinding.editView.text?.toString())
                }
                cancelButton()
                neutralButton(R.string.delete) {
                    viewModel.bookSource?.setVariable(null)
                }
            }
        }
    }

}
