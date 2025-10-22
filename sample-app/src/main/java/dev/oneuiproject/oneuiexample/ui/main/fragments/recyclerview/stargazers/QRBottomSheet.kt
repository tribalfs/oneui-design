package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers

import SharingUtils.isSamsungQuickShareAvailable
import SharingUtils.share
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.BundleCompat
import com.google.android.material.snackbar.Snackbar
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.ViewQrBottomsheetBinding
import dev.oneuiproject.oneui.app.SemBottomSheetDialogFragment
import dev.oneuiproject.oneuiexample.data.stargazers.model.Stargazer
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.suggestiveSnackBar
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.StargazerProfileActivity.Companion.KEY_STARGAZER
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util.loadImageFromUrl
import java.io.File
import java.io.FileOutputStream

class QRBottomSheet : SemBottomSheetDialogFragment(R.layout.view_qr_bottomsheet) {

    private val binding by autoCleared { ViewQrBottomsheetBinding.bind(requireView()) }
    private var tempImageFile: File? = null

    companion object {
        fun newInstance(stargazer: Stargazer): QRBottomSheet {
            return QRBottomSheet().apply{
                arguments = Bundle().apply {
                    putParcelable(KEY_STARGAZER, stargazer)
                }
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stargazer = BundleCompat.getParcelable(
            requireArguments(),
            KEY_STARGAZER,
            Stargazer::class.java
        )!!
        binding.title.text = stargazer.getDisplayName()
        binding.noteTv.text = "Scan this QR code on another device to view ${stargazer.getDisplayName()}'s profile."

        binding.qrCode.apply {
            setContent(stargazer.html_url)
            loadImageFromUrl(stargazer.avatar_url)
            invalidate()
        }

        binding.quickShareBtn.apply {
            text =  if (context.isSamsungQuickShareAvailable()) "Quick Share" else "Share"
            setOnClickListener {
                val cacheDir = requireContext().cacheDir
                tempImageFile = File(cacheDir, "${stargazer.getDisplayName()}_qrCode_${System.currentTimeMillis()}.png")
                FileOutputStream(tempImageFile).use { out ->
                    binding.qrCode.drawable.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                tempImageFile!!.share(requireContext(), shareImageResultLauncher)
            }
        }

        binding.saveBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/png"
            intent.putExtra(Intent.EXTRA_TITLE, "${stargazer.getDisplayName()}_qrCode_${System.currentTimeMillis()}.png")
            saveImageResultLauncher.launch(intent)
        }
    }

    private var shareImageResultLauncher = registerForActivityResult(StartActivityForResult()) {
        tempImageFile?.delete()
    }

    private var saveImageResultLauncher  = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = binding.qrCode.drawable.toBitmap()
            requireContext().contentResolver.openOutputStream(result.data!!.data!!)?.use { outputStream ->
                if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                    suggestiveSnackBar("Image saved successfully", duration = Snackbar.LENGTH_SHORT)
                } else {
                    suggestiveSnackBar("Failed to save image", duration = Snackbar.LENGTH_SHORT)
                }
            } ?: run {
                suggestiveSnackBar("Failed to open output stream", duration = Snackbar.LENGTH_SHORT)
            }
        }
    }


}
