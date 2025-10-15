package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class EmptyQueueFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_empty_queue, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGoToBooking = view.findViewById<Button>(R.id.btn_go_to_booking)

        btnGoToBooking.setOnClickListener {
            (activity as MainActivity).navigateToFragment(BookingFragment())
        }
    }
}
