package com.example.arapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton


class MenuFragment : Fragment(R.layout.fragment_menu) {
    private lateinit var textButton: ExtendedFloatingActionButton

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)
        textButton = view.findViewById<ExtendedFloatingActionButton?>(R.id.textButton).apply {
            setOnClickListener {
                Navigation.findNavController(view).navigate(R.id.home_to_text)
                textButton.text="Clicked"
            }
        }
        return view
    }




}