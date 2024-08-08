package cloud.app.avp.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import cloud.app.avp.MainActivityViewModel.Companion.applyContentInsets
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentSettingsBinding
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.onAppBarChangeListener
import cloud.app.avp.utils.setupTransition

abstract class BaseSettingsFragment : Fragment() {

    abstract val title: String?
    abstract val transitionName: String?
    abstract val creator: () -> PreferenceFragmentCompat

    private var binding: FragmentSettingsBinding by autoCleared()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets {
            binding.fragmentContainer.applyContentInsets(it)
        }

        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.title.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.title.title = title
        childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, creator())
            .commit()

    }

}
