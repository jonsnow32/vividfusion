package cloud.app.vvf.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.ExceptionActivity
import cloud.app.vvf.R
import cloud.app.vvf.common.exceptions.AppPermissionRequiredException
import cloud.app.vvf.common.exceptions.LoginRequiredException
import cloud.app.vvf.common.exceptions.MissingApiKeyException
import cloud.app.vvf.databinding.ItemApiKeyRequiredBinding
import cloud.app.vvf.databinding.ItemErrorBinding
import cloud.app.vvf.databinding.ItemLoginRequiredBinding
import cloud.app.vvf.databinding.ItemNotLoadingBinding
import cloud.app.vvf.databinding.ItemPermissionRequireBinding
import cloud.app.vvf.databinding.SkeletonItemContainerBinding
import cloud.app.vvf.databinding.SkeletonItemMediaRecyclerGridBinding
import cloud.app.vvf.ui.exception.ExceptionFragment.Companion.getTitle
import cloud.app.vvf.ui.setting.ExtensionSettingFragment
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.requestPermission
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack

class MediaLoadStateAdapter(val listener: Listener? = null, val isAdapterForContainer: Boolean) :
  LoadStateAdapter<MediaLoadStateAdapter.LoadStateViewHolder>() {

  interface Listener {
    fun onRetry()
    fun onError(view: View, error: Throwable)
    fun onLoginRequired(view: View, error: LoginRequiredException)
    fun onApiKeyEnter(view: View, error: MissingApiKeyException)
    fun onRequestPermission(error: AppPermissionRequiredException)
  }

  class LoadStateViewHolder(val mediaLoadStateView: MediaLoadStateView) :
    RecyclerView.ViewHolder(mediaLoadStateView.root)

  sealed class MediaLoadStateView(val root: View) {
    data class NotLoadingView(val binding: ItemNotLoadingBinding, val listener: Listener?) :
      MediaLoadStateView(binding.root) {
      override fun bind(loadState: LoadState) {
        binding.retry.setOnClickListener {
          listener?.onRetry()
        }
      }
    }

    data class ContainerLoadingView(val binding: SkeletonItemContainerBinding) :
      MediaLoadStateView(binding.root) {
      override fun bind(loadState: LoadState) {}
    }

    data class GridItemsLoadingView(val binding: SkeletonItemMediaRecyclerGridBinding) :
      MediaLoadStateView(binding.root) {
      override fun bind(loadState: LoadState) {}
    }

    data class ErrorView(val binding: ItemErrorBinding, val listener: Listener?) :
      MediaLoadStateView(binding.root) {
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

    data class PermissionRequireView(
      val binding: ItemPermissionRequireBinding,
      val listener: Listener?
    ) :
      MediaLoadStateView(binding.root) {
      override fun bind(loadState: LoadState) {
        val error =
          (loadState as LoadState.Error).error as AppPermissionRequiredException

        binding.error.run {
          transitionName = loadState.error.hashCode().toString()
          text = context.getTitle(loadState.error)
        }
        binding.requestPermissionBtn.setOnClickListener {
          listener?.onRequestPermission(error)
        }
      }
    }

    data class LoginRequiredView(val binding: ItemLoginRequiredBinding, val listener: Listener?) :
      MediaLoadStateView(binding.root) {
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

    data class ApiKeyRequiredView(val binding: ItemApiKeyRequiredBinding, val listener: Listener?) :
      MediaLoadStateView(binding.root) {
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

  override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return LoadStateViewHolder(
      when (getStateViewType(loadState)) {

        0 -> MediaLoadStateView.ContainerLoadingView(
          SkeletonItemContainerBinding.inflate(inflater, parent, false)
        )

        1 -> MediaLoadStateView.NotLoadingView(
          ItemNotLoadingBinding.inflate(inflater, parent, false), listener
        )

        2 -> MediaLoadStateView.ErrorView(
          ItemErrorBinding.inflate(inflater, parent, false),
          listener
        )

        3 -> MediaLoadStateView.LoginRequiredView(
          ItemLoginRequiredBinding.inflate(inflater, parent, false),
          listener
        )

        4 -> MediaLoadStateView.ApiKeyRequiredView(
          ItemApiKeyRequiredBinding.inflate(inflater, parent, false), listener
        )

        5 -> MediaLoadStateView.GridItemsLoadingView( // Handle GridLoadingView
          SkeletonItemMediaRecyclerGridBinding.inflate(inflater, parent, false)
        )

        6 -> MediaLoadStateView.PermissionRequireView(
          ItemPermissionRequireBinding.inflate(inflater, parent, false),
          listener
        )

        else -> throw IllegalStateException()
      }

    )
  }


  override fun getStateViewType(loadState: LoadState): Int {
    return when (loadState) {
      is LoadState.Loading -> if (isAdapterForContainer) 0 else 5
      is LoadState.NotLoading -> 1
      is LoadState.Error -> {
        when (loadState.error) {
          is LoginRequiredException -> 3
          is MissingApiKeyException -> 4
          is AppPermissionRequiredException -> 6
          else -> 2
        }
      }
    }
  }

  override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
    holder.mediaLoadStateView.bind(loadState)
  }

  constructor (fragment: Fragment, isContainerAdapter: Boolean = true, retry: () -> Unit) : this(
    object : Listener {
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
        val extension = fragment.navigate(
          ExtensionSettingFragment.newInstance(error.extensionId, error.clientName),
          view
        )
      }

      override fun onRequestPermission(error: AppPermissionRequiredException) {
        val activity = fragment.requireActivity()

        activity.requestPermission(error.permissionName, onCancel = {
          fragment.createSnack(R.string.permission_denied)
        }, onGranted = {
          retry()
        })
      }

    },
    isContainerAdapter
  )
}
