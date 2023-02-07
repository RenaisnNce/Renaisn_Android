package com.renaisn.reader.ui.book.changesource

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.view.KeyEvent.ACTION_UP
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.renaisn.reader.R
import com.renaisn.reader.base.BaseDialogFragment
import com.renaisn.reader.constant.EventBus
import com.renaisn.reader.data.appDb
import com.renaisn.reader.data.entities.Book
import com.renaisn.reader.data.entities.BookChapter
import com.renaisn.reader.data.entities.BookSource
import com.renaisn.reader.data.entities.SearchBook
import com.renaisn.reader.databinding.DialogChapterChangeSourceBinding
import com.renaisn.reader.help.book.BookHelp
import com.renaisn.reader.help.config.AppConfig
import com.renaisn.reader.lib.dialogs.alert
import com.renaisn.reader.lib.theme.elevation
import com.renaisn.reader.lib.theme.primaryColor
import com.renaisn.reader.ui.book.source.edit.BookSourceEditActivity
import com.renaisn.reader.ui.book.source.manage.BookSourceActivity
import com.renaisn.reader.ui.widget.recycler.VerticalDivider
import com.renaisn.reader.utils.*
import com.renaisn.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch


class ChangeChapterSourceDialog() : BaseDialogFragment(R.layout.dialog_chapter_change_source),
    Toolbar.OnMenuItemClickListener,
    DialogInterface.OnKeyListener,
    ChangeChapterSourceAdapter.CallBack,
    ChangeChapterTocAdapter.Callback {

    constructor(name: String, author: String, chapterIndex: Int, chapterTitle: String) : this() {
        arguments = Bundle().apply {
            putString("name", name)
            putString("author", author)
            putInt("chapterIndex", chapterIndex)
            putString("chapterTitle", chapterTitle)
        }
    }

    private val binding by viewBinding(DialogChapterChangeSourceBinding::bind)
    private val groups = linkedSetOf<String>()
    private val callBack: CallBack? get() = activity as? CallBack
    private val viewModel: ChangeChapterSourceViewModel by viewModels()
    private val editSourceResult =
        registerForActivityResult(StartActivityContract(BookSourceEditActivity::class.java)) {
            viewModel.startSearch()
        }
    private val searchBookAdapter by lazy {
        ChangeChapterSourceAdapter(requireContext(), viewModel, this)
    }
    private val tocAdapter by lazy {
        ChangeChapterTocAdapter(requireContext(), this)
    }
    private val contentSuccess: (content: String) -> Unit = {
        binding.loadingToc.hide()
        callBack?.replaceContent(it)
        dismissAllowingStateLoss()
    }
    private var searchBook: SearchBook? = null
    private val searchFinishCallback: (isEmpty: Boolean) -> Unit = {
        if (it) {
            val searchGroup = AppConfig.searchGroup
            if (searchGroup.isNotEmpty()) {
                launch {
                    alert("搜索结果为空") {
                        setMessage("${searchGroup}分组搜索结果为空,是否切换到全部分组")
                        noButton()
                        yesButton {
                            AppConfig.searchGroup = ""
                            viewModel.startSearch()
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.setOnKeyListener(this)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        viewModel.initData(arguments)
        showTitle()
        initMenu()
        initView()
        initRecyclerView()
        initSearchView()
        initBottomBar()
        initLiveData()
        viewModel.searchFinishCallback = searchFinishCallback
    }

    private fun showTitle() {
        binding.toolBar.title = viewModel.chapterTitle
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.change_source)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.menu.findItem(R.id.menu_check_author)
            ?.isChecked = AppConfig.changeSourceCheckAuthor
        binding.toolBar.menu.findItem(R.id.menu_load_info)
            ?.isChecked = AppConfig.changeSourceLoadInfo
        binding.toolBar.menu.findItem(R.id.menu_load_toc)
            ?.isChecked = AppConfig.changeSourceLoadToc
    }

    private fun initView() {
        binding.ivHideToc.setOnClickListener {
            binding.clToc.gone()
        }
        binding.flHideToc.elevation = requireContext().elevation
    }

    private fun initRecyclerView() {
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = searchBookAdapter
        searchBookAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                if (toPosition == 0) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        })
        binding.recyclerViewToc.adapter = tocAdapter
    }

    private fun initSearchView() {
        val searchView = binding.toolBar.menu.findItem(R.id.menu_screen).actionView as SearchView
        searchView.setOnCloseListener {
            showTitle()
            false
        }
        searchView.setOnSearchClickListener {
            binding.toolBar.title = ""
            binding.toolBar.subtitle = ""
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.screen(newText)
                return false
            }

        })
    }

    private fun initBottomBar() {
        binding.tvDur.text = callBack?.oldBook?.originName
        binding.tvDur.setOnClickListener {
            scrollToDurSource()
        }
        binding.ivTop.setOnClickListener {
            binding.recyclerView.scrollToPosition(0)
        }
        binding.ivBottom.setOnClickListener {
            binding.recyclerView.scrollToPosition(searchBookAdapter.itemCount - 1)
        }
    }

    private fun initLiveData() {
        viewModel.searchStateData.observe(viewLifecycleOwner) {
            binding.refreshProgressBar.isAutoLoading = it
            if (it) {
                startStopMenuItem?.let { item ->
                    item.setIcon(R.drawable.ic_stop_black_24dp)
                    item.setTitle(R.string.stop)
                }
            } else {
                startStopMenuItem?.let { item ->
                    item.setIcon(R.drawable.ic_refresh_black_24dp)
                    item.setTitle(R.string.refresh)
                }
            }
            binding.toolBar.menu.applyTint(requireContext())
        }
        lifecycleScope.launchWhenStarted {
            viewModel.searchDataFlow.conflate().collect {
                searchBookAdapter.setItems(it)
                delay(1000)
            }
        }
        launch {
            appDb.bookSourceDao.flowEnabledGroups().conflate().collect {
                groups.clear()
                groups.addAll(it)
                upGroupMenu()
            }
        }
    }

    private val startStopMenuItem: MenuItem?
        get() = binding.toolBar.menu.findItem(R.id.menu_start_stop)

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_check_author -> {
                AppConfig.changeSourceCheckAuthor = !item.isChecked
                item.isChecked = !item.isChecked
                viewModel.refresh()
            }
            R.id.menu_load_info -> {
                AppConfig.changeSourceLoadInfo = !item.isChecked
                item.isChecked = !item.isChecked
            }
            R.id.menu_load_toc -> {
                AppConfig.changeSourceLoadToc = !item.isChecked
                item.isChecked = !item.isChecked
            }
            R.id.menu_start_stop -> viewModel.startOrStopSearch()
            R.id.menu_source_manage -> startActivity<BookSourceActivity>()
            else -> if (item?.groupId == R.id.source_group) {
                if (!item.isChecked) {
                    item.isChecked = true
                    if (item.title.toString() == getString(R.string.all_source)) {
                        AppConfig.searchGroup = ""
                    } else {
                        AppConfig.searchGroup = item.title.toString()
                    }
                    viewModel.startOrStopSearch()
                    viewModel.refresh()
                }
            }
        }
        return false
    }

    private fun scrollToDurSource() {
        searchBookAdapter.getItems().forEachIndexed { index, searchBook ->
            if (searchBook.bookUrl == bookUrl) {
                (binding.recyclerView.layoutManager as LinearLayoutManager)
                    .scrollToPositionWithOffset(index, 60.dpToPx())
                return
            }
        }
    }

    override fun openToc(searchBook: SearchBook) {
        this.searchBook = searchBook
        tocAdapter.setItems(null)
        binding.clToc.visible()
        binding.loadingToc.show()
        val book = searchBook.toBook()
        viewModel.getToc(book, {
            binding.clToc.gone()
            toastOnUi(it)
        }) { toc: List<BookChapter>, _: BookSource ->
            tocAdapter.durChapterIndex =
                BookHelp.getDurChapter(viewModel.chapterIndex, viewModel.chapterTitle, toc)
            binding.loadingToc.hide()
            tocAdapter.setItems(toc)
            binding.recyclerViewToc.scrollToPosition(tocAdapter.durChapterIndex - 5)
        }
    }

    override val bookUrl: String?
        get() = callBack?.oldBook?.bookUrl

    override fun topSource(searchBook: SearchBook) {
        viewModel.topSource(searchBook)
    }

    override fun bottomSource(searchBook: SearchBook) {
        viewModel.bottomSource(searchBook)
    }

    override fun editSource(searchBook: SearchBook) {
        editSourceResult.launch {
            putExtra("sourceUrl", searchBook.origin)
        }
    }

    override fun disableSource(searchBook: SearchBook) {
        viewModel.disableSource(searchBook)
    }

    override fun deleteSource(searchBook: SearchBook) {
        viewModel.del(searchBook)
        if (bookUrl == searchBook.bookUrl) {
            viewModel.autoChangeSource(callBack?.oldBook?.type) { book, toc, source ->
                callBack?.changeTo(source, book, toc)
            }
        }
    }

    override fun setBookScore(searchBook: SearchBook, score: Int) {
        viewModel.setBookScore(searchBook, score)
    }

    override fun getBookScore(searchBook: SearchBook): Int {
        return viewModel.getBookScore(searchBook)
    }

    override fun clickChapter(bookChapter: BookChapter, nextChapterUrl: String?) {
        searchBook?.let {
            binding.loadingToc.show()
            viewModel.getContent(it.toBook(), bookChapter, nextChapterUrl, contentSuccess) { msg ->
                binding.loadingToc.hide()
                binding.clToc.gone()
                toastOnUi(msg)
            }
        }
    }

    /**
     * 更新分组菜单
     */
    private fun upGroupMenu() {
        val menu: Menu = binding.toolBar.menu
        val selectedGroup = AppConfig.searchGroup
        menu.removeGroup(R.id.source_group)
        val allItem = menu.add(R.id.source_group, Menu.NONE, Menu.NONE, R.string.all_source)
        var hasSelectedGroup = false
        groups.sortedWith { o1, o2 ->
            o1.cnCompare(o2)
        }.forEach { group ->
            menu.add(R.id.source_group, Menu.NONE, Menu.NONE, group)?.let {
                if (group == selectedGroup) {
                    it.isChecked = true
                    hasSelectedGroup = true
                }
            }
        }
        menu.setGroupCheckable(R.id.source_group, true, true)
        if (!hasSelectedGroup) {
            allItem.isChecked = true
        }
    }

    override fun observeLiveBus() {
        observeEvent<String>(EventBus.SOURCE_CHANGED) {
            searchBookAdapter.notifyItemRangeChanged(
                0,
                searchBookAdapter.itemCount,
                bundleOf(Pair("upCurSource", bookUrl))
            )
        }
    }

    override fun onKey(dialog: DialogInterface?, keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> event?.let {
                if (it.action == ACTION_UP && binding.clToc.isVisible) {
                    binding.clToc.gone()
                    return true
                }
            }
        }
        return false
    }

    interface CallBack {
        val oldBook: Book?
        fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>)
        fun replaceContent(content: String)
    }

}