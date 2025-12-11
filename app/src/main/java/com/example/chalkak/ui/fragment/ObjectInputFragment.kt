package com.example.chalkak.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.chalkak.R
import com.example.chalkak.domain.detection.DetectionResultItem
import com.example.chalkak.ui.activity.MainActivity
import com.example.chalkak.util.ImageLoaderHelper

class ObjectInputFragment : Fragment() {

    private lateinit var imgPreview: ImageView
    private lateinit var edtObjectName: EditText
    private lateinit var btnConfirm: ImageButton
    private lateinit var btnBack: ImageButton

    private var imagePath: String? = null
    private var mainNavTag: String = "home"

    companion object {
        private const val ARG_IMAGE_PATH = "image_path"
        private const val ARG_MAIN_NAV_TAG = "main_nav_tag"

        fun newInstance(imagePath: String?, mainNavTag: String = "home"): ObjectInputFragment {
            return ObjectInputFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_PATH, imagePath)
                    putString(ARG_MAIN_NAV_TAG, mainNavTag)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_object_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get arguments
        arguments?.let {
            imagePath = it.getString(ARG_IMAGE_PATH)
            mainNavTag = it.getString(ARG_MAIN_NAV_TAG, "home")
        }

        // View binding
        imgPreview = view.findViewById(R.id.img_preview)
        edtObjectName = view.findViewById(R.id.edt_object_name)
        btnConfirm = view.findViewById(R.id.btn_confirm)
        btnBack = view.findViewById(R.id.btn_back)
        
        // Set arrow icon to pure white
        btnConfirm.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)

        // Set image using ImageLoaderHelper
        ImageLoaderHelper.loadImageToView(imgPreview, imagePath)

        // Back button
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Confirm button with touch interaction
        btnConfirm.setOnClickListener {
            // Scale animation for touch feedback
            btnConfirm.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    btnConfirm.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
            
            handleConfirm()
        }

        // Handle keyboard "Done" button - only hide keyboard, don't submit
        edtObjectName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Hide keyboard only, don't submit
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(edtObjectName.windowToken, 0)
                true
            } else {
                false
            }
        }

        // Show keyboard when fragment appears
        edtObjectName.requestFocus()
        activity?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    private fun handleConfirm() {
        val objectName = edtObjectName.text.toString().trim()
        if (objectName.isBlank()) {
            Toast.makeText(requireContext(), "Please enter the object name.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate: only English letters and spaces allowed
        if (!objectName.matches(Regex("^[a-zA-Z\\s]+$"))) {
            Toast.makeText(requireContext(), "Please enter only English letters and spaces.", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert to lowercase
        val normalizedObjectName = objectName.lowercase()

        // Create a DetectionResultItem with the user input
        // Since there's no bounding box, we'll use default values (full image)
        val userInputItem = DetectionResultItem(
            label = normalizedObjectName,
            score = 1.0f, // User input is considered 100% confident
            left = 0.0f,
            top = 0.0f,
            right = 1.0f,
            bottom = 1.0f
        )

        // Process detected words to save to DB and fetch from Firebase
        val currentImagePath = imagePath
        if (currentImagePath != null) {
            (activity as? MainActivity)?.processDetectedWords(
                listOf(userInputItem),
                currentImagePath
            )
        }

        // Navigate to DetectionResultFragment
        val fragment = DetectionResultFragment.newInstance(
            imagePath = currentImagePath,
            detectionResults = listOf(userInputItem),
            mainNavTag = mainNavTag
        )
        (activity as? MainActivity)?.navigateToFragment(fragment, "detection_result")
    }
}
