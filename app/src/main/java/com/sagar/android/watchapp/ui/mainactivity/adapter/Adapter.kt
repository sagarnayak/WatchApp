package com.sagar.android.watchapp.ui.mainactivity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.wearable.Node
import com.sagar.android.watchapp.databinding.ListItemBinding

class Adapter(private val list: ArrayList<Node>, private val callback: Callback) :
    RecyclerView.Adapter<Adapter.Holder>() {

    inner class Holder(private val binding: ListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(node: Node) {
            binding.textViewNode.text = node.displayName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            ListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(list[position])
    }

    interface Callback {
        fun clicked(node: Node)
    }
}