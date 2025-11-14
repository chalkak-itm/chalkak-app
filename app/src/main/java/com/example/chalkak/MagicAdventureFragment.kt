package com.example.chalkak

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

class MagicAdventureFragment : Fragment() {
    private var photoUri: Uri? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            val intent = Intent(requireContext(), ImagePreviewActivity::class.java).apply {
                putExtra("image_uri", photoUri)
            }
            startActivity(intent)
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val intent = Intent(requireContext(), ImagePreviewActivity::class.java).apply {
                putExtra("image_uri", it)
            }
            startActivity(intent)
        }
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile("capture_", ".jpg", requireContext().cacheDir).apply {
            deleteOnExit()
        }
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        photoUri?.let { uri ->
            takePicture.launch(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_magic_adventure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val takePhoto: LinearLayout = view.findViewById(R.id.btn_take_photo)
        val upload: LinearLayout = view.findViewById(R.id.btn_upload)
        val backButton: ImageButton = view.findViewById(R.id.btn_back)

        takePhoto.setOnClickListener {
            launchCamera()
        }
        upload.setOnClickListener {
            pickImage.launch("image/*")
        }

        backButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToFragment(HomeFragment(), "home")
        }
    }
}

