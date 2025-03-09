package cloud.app.vvf.ui.widget.dialog.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.databinding.AccountItemBinding
import cloud.app.vvf.datastore.account.Account
import cloud.app.vvf.ui.widget.dialog.account.AccountAdapter.AccountViewHolder

class AccountAdapter(val listener : AccountClickListener) : ListAdapter<Account, AccountViewHolder>(AccountDiffCallback) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AccountViewHolder {
        return AccountViewHolder(
            AccountItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: AccountViewHolder,
        position: Int
    ) {
        val item = getItem(position) ?: return
        holder.bind(item)
        holder.itemView.setOnClickListener { v ->
            listener.onAccountClick(item)
        }
    }


    inner class AccountViewHolder(private val binding: AccountItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(account: Account) {
            binding.accountName.text = account.name
            binding.accountImage.setImageResource(account.avatar)
            binding.lockIcon.visibility = if (account.lockPin != null) View.VISIBLE else View.GONE
            binding.isActive.visibility = if (account.isActive) View.VISIBLE else View.GONE
        }
    }

    interface AccountClickListener {
        fun onAccountClick(account: Account)
    }


    companion object AccountDiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(
            oldItem: Account,
            newItem: Account
        ): Boolean = oldItem.getSlug().equals(newItem)

        override fun areContentsTheSame(
            oldItem: Account,
            newItem: Account
        ): Boolean = oldItem.getSlug().equals(newItem) && oldItem.isActive == newItem.isActive
    }
}
