package com.renaisn.reader.ui.main.bookshelf.style2

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.renaisn.reader.R
import com.renaisn.reader.constant.AppConst
import com.renaisn.reader.constant.AppLog
import com.renaisn.reader.constant.EventBus
import com.renaisn.reader.constant.PreferKey
import com.renaisn.reader.data.appDb
import com.renaisn.reader.data.entities.Book
import com.renaisn.reader.data.entities.BookGroup
import com.renaisn.reader.databinding.FragmentBookshelf1Binding
import com.renaisn.reader.help.book.isAudio
import com.renaisn.reader.help.config.AppConfig
import com.renaisn.reader.lib.theme.accentColor
import com.renaisn.reader.lib.theme.primaryColor
import com.renaisn.reader.ui.book.audio.AudioPlayActivity
import com.renaisn.reader.ui.book.group.GroupEditDialog
import com.renaisn.reader.ui.book.info.BookInfoActivity
import com.renaisn.reader.ui.book.read.ReadBookActivity
import com.renaisn.reader.ui.book.search.SearchActivity
import com.renaisn.reader.ui.main.bookshelf.BaseBookshelfFragment
import com.renaisn.reader.utils.*
import com.renaisn.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 书架界面
 */
class BookshelfFragment2 : BaseBookshelfFragment(R.layout.fragment_bookshelf1),
    SearchView.OnQueryTextListener,
    BaseBooksAdapter.CallBack {

    private val binding by viewBinding(FragmentBookshelf1Binding::bind)
    private val bookshelfLayout by lazy {
        getPrefInt(PreferKey.bookshelfLayout)
    }
    private val booksAdapter: BaseBooksAdapter<*> by lazy {
        if (bookshelfLayout == 0) {
            BooksAdapterList(requireContext(), this)
        } else {
            BooksAdapterGrid(requireContext(), this)
        }
    }
    private var bookGroups: List<BookGroup> = emptyList()
    private var booksFlowJob: Job? = null
    override var groupId = AppConst.rootGroupId
    override var books: List<Book> = emptyList()

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initRecyclerView()
        initBookGroupData()
        initBooksData()
    }

    private fun initRecyclerView() {
        binding.rvBookshelf.setEdgeEffectColor(primaryColor)
        binding.refreshLayout.setColorSchemeColors(accentColor)
        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.isRefreshing = false
            activityViewModel.upToc(books)
        }
        if (bookshelfLayout == 0) {
            binding.rvBookshelf.layoutManager = LinearLayoutManager(context)
        } else {
            binding.rvBookshelf.layoutManager = GridLayoutManager(context, bookshelfLayout + 2)
        }
        binding.rvBookshelf.adapter = booksAdapter
        booksAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                val layoutManager = binding.rvBookshelf.layoutManager
                if (positionStart == 0 && layoutManager is LinearLayoutManager) {
                    val scrollTo = layoutManager.findFirstVisibleItemPosition() - itemCount
                    binding.rvBookshelf.scrollToPosition(max(0, scrollTo))
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                val layoutManager = binding.rvBookshelf.layoutManager
                if (toPosition == 0 && layoutManager is LinearLayoutManager) {
                    val scrollTo = layoutManager.findFirstVisibleItemPosition() - itemCount
                    binding.rvBookshelf.scrollToPosition(max(0, scrollTo))
                }
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun upGroup(data: List<BookGroup>) {
        if (data != bookGroups) {
            bookGroups = data
            booksAdapter.notifyDataSetChanged()
            binding.tvEmptyMsg.isGone = getItemCount() > 0
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initBooksData() {
        if (groupId == -100L) {
            binding.titleBar.title = getString(R.string.bookshelf)
        } else {
            bookGroups.forEach {
                if (groupId == it.groupId) {
                    binding.titleBar.title = "${getString(R.string.bookshelf)}(${it.groupName})"
                }
            }
        }
        booksFlowJob?.cancel()
        booksFlowJob = launch {
            when (groupId) {
                AppConst.rootGroupId -> appDb.bookDao.flowRoot()
                AppConst.bookGroupAllId -> appDb.bookDao.flowAll()
                AppConst.bookGroupLocalId -> appDb.bookDao.flowLocal()
                AppConst.bookGroupAudioId -> appDb.bookDao.flowAudio()
                AppConst.bookGroupNetNoneId -> appDb.bookDao.flowNetNoGroup()
                AppConst.bookGroupLocalNoneId -> appDb.bookDao.flowLocalNoGroup()
                AppConst.bookGroupErrorId -> appDb.bookDao.flowUpdateError()
                else -> appDb.bookDao.flowByGroup(groupId)
            }.conflate().map { list ->
                when (AppConfig.getBookSortByGroupId(groupId)) {
                    1 -> list.sortedByDescending {
                        it.latestChapterTime
                    }
                    2 -> list.sortedWith { o1, o2 ->
                        o1.name.cnCompare(o2.name)
                    }
                    3 -> list.sortedBy {
                        it.order
                    }
                    else -> list.sortedByDescending {
                        it.durChapterTime
                    }
                }
            }.flowOn(Dispatchers.Default).catch {
                AppLog.put("书架更新出错", it)
            }.conflate().collect { list ->
                books = list
                booksAdapter.notifyDataSetChanged()
                binding.tvEmptyMsg.isGone = getItemCount() > 0
                delay(100)
            }
        }
    }

    fun back(): Boolean {
        if (groupId != -100L) {
            groupId = -100L
            initBooksData()
            return true
        }
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        SearchActivity.start(requireContext(), query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    override fun gotoTop() {
        if (AppConfig.isEInkMode) {
            binding.rvBookshelf.scrollToPosition(0)
        } else {
            binding.rvBookshelf.smoothScrollToPosition(0)
        }
    }

    override fun onItemClick(position: Int) {
        when (val item = getItem(position)) {
            is Book -> when {
                item.isAudio ->
                    startActivity<AudioPlayActivity> {
                        putExtra("bookUrl", item.bookUrl)
                    }
                else -> startActivity<ReadBookActivity> {
                    putExtra("bookUrl", item.bookUrl)
                }
            }
            is BookGroup -> {
                groupId = item.groupId
                initBooksData()
            }
        }
    }

    override fun onItemLongClick(position: Int) {
        when (val item = getItem(position)) {
            is Book -> startActivity<BookInfoActivity> {
                putExtra("name", item.name)
                putExtra("author", item.author)
            }
            is BookGroup -> showDialogFragment(GroupEditDialog(item))
        }
    }

    override fun isUpdate(bookUrl: String): Boolean {
        return activityViewModel.isUpdate(bookUrl)
    }

    override fun getItemCount(): Int {
        return if (groupId == AppConst.rootGroupId) {
            bookGroups.size + books.size
        } else {
            books.size
        }
    }

    override fun getItemType(position: Int): Int {
        if (groupId != AppConst.rootGroupId) {
            return 0
        }
        if (position < bookGroups.size) {
            return 1
        }
        return 0
    }

    override fun getItem(position: Int): Any? {
        if (groupId != AppConst.rootGroupId) {
            return books.getOrNull(position)
        }
        if (position < bookGroups.size) {
            return bookGroups[position]
        }
        return books.getOrNull(position - bookGroups.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.UP_BOOKSHELF) {
            booksAdapter.notification(it)
        }
        observeEvent<String>(EventBus.BOOKSHELF_REFRESH) {
            booksAdapter.notifyDataSetChanged()
        }
    }
}