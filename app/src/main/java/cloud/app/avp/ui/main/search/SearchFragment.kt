package cloud.app.avp.ui.main.search

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.avp.databinding.FragmentSearchBinding
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.setupTransition

class SearchFragment : Fragment() {
  private var binding by autoCleared<FragmentSearchBinding>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentSearchBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)
    applyInsetsMain(binding.searchBar, binding.rvRearchResult)

//    binding.mainSearch.setOnQueryTextFocusChangeListener { _, hasFocus ->
//      if (hasFocus) {
//        val imm = context?.getSystemService<InputMethodManager>()
//        imm?.showSoftInput(binding.mainSearch, InputMethodManager.SHOW_IMPLICIT)
//      }
//    }
    binding.mainSearch.setOnFocusChangeListener { view, hasFocus ->
      if (hasFocus) {
        val imm = context?.getSystemService<InputMethodManager>()
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
      }
    }
    binding.mainSearch.requestFocus()
  }
}
