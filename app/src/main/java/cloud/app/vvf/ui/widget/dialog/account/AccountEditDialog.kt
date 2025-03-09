package cloud.app.vvf.ui.widget.dialog.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cloud.app.vvf.R
import cloud.app.vvf.databinding.AccountEditDialogBinding
import cloud.app.vvf.datastore.account.Account
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.dismissSafe
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.putSerialized
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountEditDialog : DockingDialog() {
  var binding by autoCleared<AccountEditDialogBinding>()
  val account: Account? by lazy { arguments?.getSerialized<Account>("account") }
  var newAccount: Account? = null

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = AccountEditDialogBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    account?.apply {
      binding.accountName.setText(name)
      binding.accountImage.setImageResource(avatar)
      binding.lockAccount.isChecked = lockPin != null

      binding.title.text = getString(R.string.edit_account)
    } ?: run {
      binding.title.text = getString(R.string.create_account)
    }

    binding.cancelBtt.setOnClickListener { v ->
      dialog.dismissSafe(activity)
    }
    binding.applyBtt.setOnClickListener {
      newAccount = Account(
        name = binding.accountName.text.toString(),
        avatar = R.drawable.ic_person,
        lockPin = if (binding.pin.text.toString().isEmpty()) null else binding.pin.text.toString()
      )
      dialog.dismissSafe(activity)
    }
    binding.lockAccount.setOnCheckedChangeListener { buttonView, isChecked ->
      binding.pin.visibility = if (isChecked) View.VISIBLE else View.GONE
    }
  }

  override fun getResultBundle(): Bundle? {
    return if (newAccount != null) {
      Bundle().apply {
        putSerialized("newAccount", newAccount)
      }
    } else {
      null
    }
  }

  companion object {
    fun newInstance(account: Account? = null): AccountEditDialog {
      val args = Bundle()
      args.putSerialized("account", account)
      val fragment = AccountEditDialog()
      fragment.arguments = args
      return fragment
    }
  }
}
