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

  private var binding by autoCleared<AccountEditDialogBinding>()
  private val args by lazy { requireArguments() }
  private val account: Account? by lazy { args.getSerialized<Account>("account") }

  var newAccount: Account? = null

  private val avatarAdapter by lazy {
    AvatarAdapter(AVATAR_LIST.map { AvatarItem(it, selected = it == account?.avatar) })
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = AccountEditDialogBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    with(binding) {
      account?.let {
        accountName.setText(it.name)
        lockAccount.isChecked = it.lockPin != null
        pin.visibility =  if (it.lockPin != null) View.VISIBLE else View.GONE
        pin.setText(it.lockPin)
        title.text = getString(R.string.edit_account)
      } ?: run {
        title.text = getString(R.string.create_account)
        accountName.hint = RANDOM_NAMES.random()
      }

      cancelBtt.setOnClickListener { dialog.dismissSafe(activity) }

      applyBtt.setOnClickListener {
        val drawableStr = avatarAdapter.items.find { it.selected }?.drawableStr ?: AVATAR_LIST.first()
        newAccount = Account(
          id = account?.id ?: System.currentTimeMillis(),
          name = if (accountName.text.toString()
              .isEmpty()
          ) accountName.hint.toString() else accountName.text.toString(),
          avatar = drawableStr,
          lockPin = if(lockAccount.isChecked) pin.text.toString().takeIf { it.isNotEmpty() } else null,
          isActive = account?.isActive == true
        )
        dialog.dismissSafe(activity)
      }

      lockAccount.setOnCheckedChangeListener { _, isChecked ->
        pin.visibility = if (isChecked) View.VISIBLE else View.GONE
      }

      rvAvatars.adapter = avatarAdapter
    }
  }

  override fun getResultBundle(): Bundle? =
    newAccount.let { Bundle().apply { putSerialized("newAccount", it) } }

  companion object {
    private val AVATAR_LIST = (1..17).map { i ->
      "funemoji_$i"
    }

    private val RANDOM_NAMES = listOf(
      "James", "Emily", "Michael", "Olivia", "William", "Sophia",
      "Benjamin", "Charlotte", "Daniel", "Abigail", "Henry", "Grace",
      "Samuel", "Lily", "Christopher", "Madison", "Andrew"
    )

    fun newInstance(account: Account? = null) = AccountEditDialog().apply {
      arguments = Bundle().apply { putSerialized("account", account) }
    }
  }
}
