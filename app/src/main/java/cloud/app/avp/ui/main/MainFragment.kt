package cloud.app.avp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cloud.app.avp.MainViewModel
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentMainBinding
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.navigate
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {
  private var binding by autoCleared<FragmentMainBinding>()
  private val mainViewModel by activityViewModels<MainViewModel>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentMainBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
  }
}
