package com.example.chalkak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment

class QuizFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quiz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            (activity as? MainActivity)?.navigateToFragment(HomeFragment(), "home")
        }

        // Placeholder: set demo content (will be replaced with DB data)
        val choices = listOf(R.id.choice1, R.id.choice2, R.id.choice3, R.id.choice4)
        choices.forEachIndexed { idx, rid ->
            view.findViewById<android.widget.TextView>(rid)?.text = "Choice ${idx + 1}"
        }
        view.findViewById<ImageView>(R.id.img_quiz)?.setImageResource(R.drawable.demo)
    }
}

