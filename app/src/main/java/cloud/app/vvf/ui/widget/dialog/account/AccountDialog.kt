package cloud.app.vvf.ui.widget.dialog.account

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import cloud.app.vvf.R
import cloud.app.vvf.databinding.AccountDialogBinding
import cloud.app.vvf.databinding.InputPinDialogBinding
import cloud.app.vvf.datastore.account.Account
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.dismissSafe
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.setDefaultFocus
import cloud.app.vvf.utils.showToast
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountDialog : DockingDialog(), AccountAdapter.AccountClickListener {
  var binding by autoCleared<AccountDialogBinding>()
  val viewmodel by activityViewModels<AccountDialogViewModel>()
  val adapter = AccountAdapter(this);
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = AccountDialogBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    observe(viewmodel.accounts) {
      if (it != null)
        adapter.submitList(it)
    }

    binding.accountList.adapter = adapter
    viewmodel.loadAccounts()

    binding.addAccount.setOnClickListener { v ->
      AccountEditDialog.newInstance().show(parentFragmentManager) { resultBundle ->
        resultBundle?.getSerialized<Account>("newAccount")?.let { newAccount ->
          viewmodel.setActiveAccount(context, newAccount) { isSuccess ->
            if (isSuccess) viewmodel.loadAccounts()
            else
              context?.showToast(R.string.create_account_failed)
          }
        }
      }
    }
  }

  override fun onSelectAccount(account: Account) {
    if (account.lockPin.isNullOrEmpty()) {
      viewmodel.setActiveAccount(context, account) {
        dialog.dismissSafe(activity)
        context?.showToast(R.string.switch_account_to)

      }
    } else {
      val context = context ?: return;
      showInputPinDialog(account, context) { pin ->
        if (account.lockPin == pin) {
          viewmodel.setActiveAccount(context, account) {
            dialog.dismissSafe(activity)
            activity?.showToast(getString(R.string.switch_account_to, account.name))
          }
        }
      }
    }
  }

  fun showEditDialog(account: Account) {
    if (account.getSlug() == 0L)
      context?.showToast(R.string.can_not_edit_default_account)
    else {
      AccountEditDialog.newInstance(account).show(parentFragmentManager) { resultBundle ->
        resultBundle?.getSerialized<Account>("newAccount")?.let { newAccount ->
          if (account.getSlug() == newAccount.getSlug()) {
            viewmodel.updateAccount(newAccount)
          } else {
            viewmodel.setActiveAccount(context, newAccount) { isSuccess ->
              if (isSuccess) viewmodel.loadAccounts()
              else context?.showToast(getString(R.string.create_account_failed, newAccount.name))

            }
          }
        }
      }
    }
  }

  override fun onShowMenuOption(account: Account) {
    //showEditDialog(account)
  }

  override fun onEditAccount(account: Account) {


    if (account.lockPin.isNullOrEmpty()) {
      showEditDialog(account)
    } else {
      val context = context ?: return;
      showInputPinDialog(account, context) { pin ->
        if (account.lockPin == pin) {
          showEditDialog(account)
        }
      }
    }


  }

  companion object {
    fun newInstance(title: String, callback: (String) -> Unit): AccountDialog {
      val args = Bundle()
      args.putString("title", title)
      val fragment = AccountDialog()
      fragment.arguments = args
      return fragment
    }

    fun showInputPinDialog(account: Account, context: Context, callback: (String?) -> Unit) {
      val binding = InputPinDialogBinding.inflate(LayoutInflater.from(context))
      val dialog = MaterialAlertDialogBuilder(context)
        .setView(binding.root)
        .setTitle(R.string.enter_pin)
        .setPositiveButton(R.string.yes) { _, _ -> }
        .setNegativeButton(R.string.cancel) { dialog, _ ->
          dialog.dismiss()
        }.show()

      dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
        if (account.lockPin == binding.pinEditText.text.toString()) {
          callback(binding.pinEditText.text.toString())
          dialog.dismiss()
        } else {
          binding.pinEditTextError.visibility = View.VISIBLE
          binding.pinEditTextError.text = context.getString(R.string.pin_wrong)
        }
      }
      dialog.setDefaultFocus()
    }
  }
}
