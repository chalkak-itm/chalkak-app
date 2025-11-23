package com.example.chalkak

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class MagicAdventureFragment : Fragment() {

    companion object {
        private const val ARG_AUTO_LAUNCH_CAMERA = "auto_launch_camera"

        fun newInstance(autoLaunchCamera: Boolean = false): MagicAdventureFragment {
            val fragment = MagicAdventureFragment()
            fragment.arguments = Bundle().apply {
                putBoolean(ARG_AUTO_LAUNCH_CAMERA, autoLaunchCamera)
            }
            return fragment
        }
    }
    
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

        // when come from HomeFragment, check the auto camera
        val autoLaunch = arguments?.getBoolean(ARG_AUTO_LAUNCH_CAMERA, false) ?: false
        if (autoLaunch) {
            // set flag false for reentering
            arguments?.putBoolean(ARG_AUTO_LAUNCH_CAMERA, false)

            // Do camera after loading UI
            view.post {
                ToastHelper.showCenterToast(requireContext(), "Launching camera for Magic Adventure...")
                imagePickerHelper.launchCamera()
            }
        }
        upload.setOnClickListener {
            imagePickerHelper.pickImage()
        }

        backButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToFragment(HomeFragment(), "home")
        }
    }


}

