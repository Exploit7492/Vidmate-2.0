package com.cobalt.android.ui.common

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.cobalt.android.R
import com.cobalt.android.util.QrCodeGenerator

/**
 * Shows a QR code for a share link, so another device can scan it instead of
 * receiving a message. Mirrors VidMate's QR share sheet. The link is passed in
 * as an argument so the dialog is reusable from any screen that has a URL.
 */
class QrShareDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_qr_share, container, false)
        val content = requireArguments().getString(ARG_CONTENT).orEmpty()
        val bitmap: Bitmap? = QrCodeGenerator.generate(content)
        val image = view.findViewById<ImageView>(R.id.qrImage)
        if (bitmap != null) image.setImageBitmap(bitmap)
        else image.visibility = View.GONE
        view.findViewById<TextView>(R.id.qrLink).text = content
        view.findViewById<TextView>(R.id.qrClose).setOnClickListener { dismiss() }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply { setCanceledOnTouchOutside(true) }

    companion object {
        const val TAG = "QrShareDialogFragment"
        private const val ARG_CONTENT = "arg_content"

        fun newInstance(content: String) = QrShareDialogFragment().apply {
            arguments = Bundle().apply { putString(ARG_CONTENT, content) }
        }
    }
}
