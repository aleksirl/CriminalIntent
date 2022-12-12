package com.bignerbranch.android.criminal_intent

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment


private const val ARG_IMAGE = "Image"
class PhotoDialogFragment: DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.photo_dialog_fragment, container ,false)
        val imageView = view.findViewById(R.id.crimePicture) as ImageView

        val photoFileName = arguments?.getSerializable(ARG_IMAGE) as String
        imageView.setImageBitmap(BitmapFactory.decodeFile(requireContext().filesDir.path+ "/"+ photoFileName))

        return view
    }









    companion object {
        fun newInstance(photoFileName: String): PhotoDialogFragment{
            val args = Bundle().apply {
                putSerializable(ARG_IMAGE, photoFileName)
            }
            return PhotoDialogFragment().apply { arguments = args }
        }
    }

}

