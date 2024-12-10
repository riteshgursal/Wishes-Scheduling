package com.example.birthday

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WishAdapter(private val wishList: List<UploadActivity.WishData>) :
    RecyclerView.Adapter<WishAdapter.WishViewHolder>() {

    class WishViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.nameTextView)
        val mobile: TextView = itemView.findViewById(R.id.mobileTextView)
        val date: TextView = itemView.findViewById(R.id.dateTextView)
        val time: TextView = itemView.findViewById(R.id.timeTextView)
        val message: TextView = itemView.findViewById(R.id.messageTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wish, parent, false)
        return WishViewHolder(view)
    }

    override fun onBindViewHolder(holder: WishViewHolder, position: Int) {
        val wish = wishList[position]
        holder.name.text = wish.name
        holder.mobile.text = wish.mobileNumber
        holder.date.text = wish.date
        holder.time.text = wish.time
        holder.message.text = wish.message
    }

    override fun getItemCount(): Int = wishList.size
}
