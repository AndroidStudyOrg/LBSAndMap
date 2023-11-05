package org.shop.lbsandmap

import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.shop.lbsandmap.databinding.ItemLocationBinding

class RVLocationAdapter(private var items: List<Location>) :
    RecyclerView.Adapter<RVLocationAdapter.LocationInfo>() {

    inner class LocationInfo(val itemBinding: ItemLocationBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(item: Location) {
            with(itemBinding) {
                tvLat.text = item.latitude.toString()
                tvLong.text = item.longitude.toString()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationInfo {
        val binding =
            ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationInfo(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: LocationInfo, position: Int) {
        holder.bind(items[position])
    }
}