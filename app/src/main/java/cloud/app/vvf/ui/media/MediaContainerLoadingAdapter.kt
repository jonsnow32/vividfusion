package cloud.app.vvf.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.ExceptionActivity
import cloud.app.vvf.databinding.ItemErrorBinding
import cloud.app.vvf.databinding.ItemLoginRequiredBinding
import cloud.app.vvf.databinding.ItemNotLoadingBinding
import cloud.app.vvf.databinding.SkeletonItemContainerBinding
import cloud.app.vvf.databinding.SkeletonItemMediaRecyclerGridBinding
import cloud.app.vvf.ui.exception.ExceptionFragment.Companion.getTitle
import cloud.app.vvf.common.exceptions.LoginRequiredException
import cloud.app.vvf.common.exceptions.MissingApiKeyException
import cloud.app.vvf.databinding.ItemApiKeyRequiredBinding
import cloud.app.vvf.ui.setting.ExtensionSettingFragment
import cloud.app.vvf.ui.setting.ManageExtensionsFragment
import cloud.app.vvf.utils.navigate

class MediaContainerLoadingAdapter(val listener: Listener? = null) :
  LoadStateAdapter<MediaContainerLoadingAdapter.LoadViewHolder>() {

  interface Listener {
    fun onRetry()
    fun onError(view: View, error: Throwable)
    fun onLoginRequired(view: View, error: LoginRequiredException)
    fun onApiKeyEnter(view: View, error: MissingApiKeyException)
  }

  class LoadViewHolder(val container: Container) : RecyclerView.ViewHolder(container.root)

  sealed class Container(val root: View) {
    data class NotLoading(val binding: ItemNotLoadingBinding, val listener: Listener?) :
      Container(binding.root) {
      override fun bind(loadState: LoadState) {
        binding.retry.setOnClickListener {
          listener?.onRetry()
        }
      }
    }

    data class Loading(val binding: SkeletonItemContainerBinding) : Container(binding.root) {
      override fun bind(loadState: LoadState) {}
    }


    data class GridLoading(val binding: SkeletonItemMediaRecyclerGridBinding) :
      Container(binding.root) {
      override fun bind(loadState: LoadState) {}
    }

    data class Error(val binding: ItemErrorBinding, val listener: Listener?) :
      Container(binding.root) {
      override fun bind(loadState: LoadState) {
        loadState as LoadState.Error
        binding.error.run {
          transitionName = loadState.error.hashCode().toString()
          text = context.getTitle(loadState.error)
        }
        binding.errorView.setOnClickListener {
          listener?.onError(binding.error, loadState.error)
        }
        binding.retry.setOnClickListener {
          listener?.onRetry()
        }
      }
    }

    data class LoginRequired(val binding: ItemLoginRequiredBinding, val listener: Listener?) :
      Container(binding.root) {
      override fun bind(loadState: LoadState) {
        val error =
          (loadState as LoadState.Error).error as LoginRequiredException
        binding.error.run {
          text = context.getTitle(loadState.error)
        }
        binding.login.transitionName = error.hashCode().toString()
        binding.login.setOnClickListener {
          listener?.onLoginRequired(it, error)
        }
      }
    }

    data class ApiKeyRequired(val binding: ItemApiKeyRequiredBinding, val listener: Listener?) :
      Container(binding.root) {
      override fun bind(loadState: LoadState) {
        val error =
          (loadState as LoadState.Error).error as MissingApiKeyException
        binding.error.run {
          text = context.getTitle(loadState.error)
        }
        binding.enterApiKey.transitionName = error.hashCode().toString()
        binding.enterApiKey.setOnClickListener {
          listener?.onApiKeyEnter(it, error)
        }
      }
    }

    abstract fun bind(loadState: LoadState)
  }

  override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return LoadViewHolder(
      when (getStateViewType(loadState)) {

        0 -> Container.Loading(
          SkeletonItemContainerBinding.inflate(inflater, parent, false)
        )

        1 -> Container.NotLoading(
          ItemNotLoadingBinding.inflate(inflater, parent, false), listener
        )

        2 -> Container.Error(
          ItemErrorBinding.inflate(inflater, parent, false),
          listener
        )

        3 -> Container.LoginRequired(
          ItemLoginRequiredBinding.inflate(inflater, parent, false),
          listener
        )

        4 -> Container.ApiKeyRequired(
          ItemApiKeyRequiredBinding.inflate(inflater, parent, false), listener
        )

        else -> throw IllegalStateException()
      }

    )
  }


  override fun getStateViewType(loadState: LoadState): Int {
    return when (loadState) {
      is LoadState.Loading -> 0
      is LoadState.NotLoading -> 1
      is LoadState.Error -> {
        when (loadState.error) {
          is LoginRequiredException -> 3
          is MissingApiKeyException -> 4
          else -> 2
        }
      }
    }
  }

  override fun onBindViewHolder(holder: LoadViewHolder, loadState: LoadState) {
    holder.container.bind(loadState)
  }

  constructor (fragment: Fragment, retry: () -> Unit) : this(object : Listener {
    override fun onRetry() {
      retry()
    }

    override fun onError(view: View, error: Throwable) {
      ExceptionActivity.start(view.context, error)
    }

    override fun onLoginRequired(view: View, error: LoginRequiredException) {
      ExceptionActivity.start(view.context, error)
    }

    override fun onApiKeyEnter(view: View, error: MissingApiKeyException) {
      //fragment.navigate(ManageExtensionsFragment())
      val extension = fragment.navigate(
        ExtensionSettingFragment.newInstance(error.extensionId, error.clientName),
        view
      )

    }
  })
}
