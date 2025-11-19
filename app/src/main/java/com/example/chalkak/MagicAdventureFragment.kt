package com.example.chalkak

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment

class MagicAdventureFragment : Fragment() {
    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_magic_adventure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ImagePickerHelper
        imagePickerHelper = ImagePickerHelper(
            context = requireContext(),
            packageName = requireContext().packageName,
            onImageSelected = { uri, mainNavTag ->
                val intent = Intent(requireContext(), ImagePreviewActivity::class.java).apply {
                    putExtra("image_uri", uri)
                    putExtra("main_nav_tag", mainNavTag)
                }
                startActivity(intent)
            }
        )
        
        imagePickerHelper.initializeForFragment(this) {
            (activity as? MainActivity)?.getCurrentMainNavigationTag() ?: "home"
        }

        val takePhoto: LinearLayout = view.findViewById(R.id.btn_take_photo)
        val upload: LinearLayout = view.findViewById(R.id.btn_upload)
        val backButton: ImageButton = view.findViewById(R.id.btn_back)

        takePhoto.setOnClickListener {
            imagePickerHelper.launchCamera()
        }
        upload.setOnClickListener {
            imagePickerHelper.pickImage()
        }

        backButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToFragment(HomeFragment(), "home")
        }
    }
}

