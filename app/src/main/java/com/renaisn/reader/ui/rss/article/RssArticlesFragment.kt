package com.renaisn.reader.ui.rss.article


import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.renaisn.reader.R
import com.renaisn.reader.base.VMBaseFragment
import com.renaisn.reader.data.appDb
import com.renaisn.reader.data.entities.RssArticle
import com.renaisn.reader.databinding.FragmentRssArticlesBinding
import com.renaisn.reader.databinding.ViewLoadMoreBinding
import com.renaisn.reader.lib.theme.accentColor
import com.renaisn.reader.lib.theme.primaryColor
import com.renaisn.reader.ui.rss.read.ReadRssActivity
import com.renaisn.reader.ui.widget.recycler.LoadMoreView
import com.renaisn.reader.ui.widget.recycler.VerticalDivider
import com.renaisn.reader.utils.setEdgeEffectColor
import com.renaisn.reader.utils.startActivity
import com.renaisn.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RssArticlesFragment() : VMBaseFragment<RssArticlesViewModel>(R.layout.fragment_rss_articles),
    BaseRssArticlesAdapter.CallBack {

    constructor(sortName: String, sortUrl: String) : this() {
        arguments = Bundle().apply {
            putString("sortName", sortName)
            putString("sortUrl", sortUrl)
        }
    }

    private val binding by viewBinding(FragmentRssArticlesBinding::bind)
    private val activityViewModel by activityViewModels<RssSortViewModel>()
    override val viewModel by viewModels<RssArticlesViewModel>()
    private val adapter: BaseRssArticlesAdapter<*> by lazy {
        when (activityViewModel.rssSource?.articleStyle) {
            1 -> RssArticlesAdapter1(requireContext(), this@RssArticlesFragment)
            2 -> RssArticlesAdapter2(requireContext(), this@RssArticlesFragment)
            else -> RssArticlesAdapter(requireContext(), this@RssArticlesFragment)
        }
    }
    private val loadMoreView: LoadMoreView by lazy {
        LoadMoreView(requireContext())
    }
    private var articlesFlowJob: Job? = null
    override val isGridLayout: Boolean
        get() = activityViewModel.isGridLayout

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.init(arguments)
        initView()
        initData()
    }

    private fun initView() = binding.run {
        refreshLayout.setColorSchemeColors(accentColor)
        recyclerView.setEdgeEffectColor(primaryColor)
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading) {
                scrollToBottom()
            }
        }
        recyclerView.layoutManager = if (activityViewModel.isGridLayout) {
            recyclerView.setPadding(8, 0, 8, 0)
            GridLayoutManager(requireContext(), 2)
        } else {
            recyclerView.addItemDecoration(VerticalDivider(requireContext()))
            LinearLayoutManager(requireContext())
        }
        recyclerView.adapter = adapter
        adapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        refreshLayout.setOnRefreshListener {
            loadArticles()
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                }
            }
        })
        refreshLayout.post {
            refreshLayout.isRefreshing = true
            loadArticles()
        }
    }

    private fun initData() {
        val rssUrl = activityViewModel.url ?: return
        articlesFlowJob?.cancel()
        articlesFlowJob = lifecycleScope.launch {
            appDb.rssArticleDao.flowByOriginSort(rssUrl, viewModel.sortName).collect {
                adapter.setItems(it)
            }
        }
    }

    private fun loadArticles() {
        activityViewModel.rssSource?.let {
            viewModel.loadContent(it)
        }
    }

    private fun scrollToBottom() {
        if (viewModel.isLoading) return
        if (loadMoreView.hasMore && adapter.getActualItemCount() > 0) {
            loadMoreView.startLoad()
            activityViewModel.rssSource?.let {
                viewModel.loadMore(it)
            }
        }
    }

    override fun observeLiveBus() {
        viewModel.loadErrorLiveData.observe(viewLifecycleOwner) {
            loadMoreView.error(it)
        }
        viewModel.loadFinallyLiveData.observe(viewLifecycleOwner) { hasMore ->
            binding.refreshLayout.isRefreshing = false
            if (!hasMore) {
                loadMoreView.noMore()
            }
        }
    }

    override fun readRss(rssArticle: RssArticle) {
        activityViewModel.read(rssArticle)
        startActivity<ReadRssActivity> {
            putExtra("title", rssArticle.title)
            putExtra("origin", rssArticle.origin)
            putExtra("link", rssArticle.link)
        }
    }
}