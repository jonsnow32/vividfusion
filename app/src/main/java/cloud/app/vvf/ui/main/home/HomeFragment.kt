package cloud.app.vvf.ui.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.vvf.R
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.databinding.FragmentHomeBinding
import cloud.app.vvf.ui.detail.loadWith
import cloud.app.vvf.ui.main.configureFeedUI
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.firstVisible
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.scrollTo
import cloud.app.vvf.utils.setupTransition
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
  private var binding by autoCleared<FragmentHomeBinding>()
  private val viewModel by activityViewModels<HomeViewModel>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentHomeBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)
    applyInsetsMain(binding.appBarLayoutCustom, binding.recyclerView)
    configureFeedUI<DatabaseClient>(
      R.string.home,
      viewModel,
      binding.recyclerView,
      binding.swipeRefresh,
      binding.tabLayout
    )

    binding.recyclerView.scrollTo(viewModel.recyclerPosition, viewModel.recyclerOffset) {
    }

    binding.selectedExtension.setOnClickListener { button ->
      val databaseExtensions =
        viewModel.extListFlow.value?.filter { it.types.contains(ExtensionType.DATABASE) }
      showDropdownMenu(button, databaseExtensions)
    }

    observe(viewModel.extListFlow) { list ->
      val dbList = list?.filter { it.types.contains(ExtensionType.DATABASE) }
      binding.selectedExtension.isGone = dbList.isNullOrEmpty()
    }
    observe(viewModel.dbExtFlow) {
      binding.selectedExtension.loadWith(it?.icon?.toImageHolder())
    }
  }

  private fun showDropdownMenu(view: View, extensions: List<Extension<*>>?) {
    val activity = activity ?: return

    val dropdownItems = extensions?.map {
      DropdownItem(it.icon, it.name, it.id)
    } ?: return

    val layoutInflater = LayoutInflater.from(activity)
    val popupView = layoutInflater.inflate(R.layout.home_extension_menu, null)
    val listView = popupView.findViewById<ListView>(R.id.dropdown_list_view)

    val adapter = ExtensionMenuAdapter(activity, dropdownItems)
    listView.adapter = adapter


    val popupWindow = PopupWindow(
      popupView,
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
      true
    )
    popupWindow.elevation = 10f

    listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
//      Toast.makeText(activity, "Selected: ${dropdownItems[position].text}", Toast.LENGTH_SHORT)
//        .show()
      
      popupWindow.dismiss()
      viewModel.dbExtFlow.value = extensions[position].asType()
    }

    popupWindow.showAsDropDown(view)
  }


  override fun onStop() {
    val position = binding.recyclerView.firstVisible();
    viewModel.recyclerPosition = position
    viewModel.recyclerOffset =
      binding.recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.top ?: 0
    super.onStop()
  }
}
