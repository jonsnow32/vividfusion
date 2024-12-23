package cloud.app.vvf.ui.main.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cloud.app.vvf.databinding.FragmentLibraryBinding
import cloud.app.vvf.utils.autoCleared

class LibraryFragment : Fragment() {
  val viewModel by viewModels<LibraryViewModel>()
  private var binding by autoCleared<FragmentLibraryBinding>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentLibraryBinding.inflate(inflater, container, false)
    return binding.root
  }

}
