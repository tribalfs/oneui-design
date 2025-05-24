package dev.oneuiproject.oneuiexample.ui.main.fragments.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentBottomsheetBinding
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared

class FragmentBottomSheet : BottomSheetDialogFragment(R.layout.fragment_bottomsheet) {

    private val binding by autoCleared { FragmentBottomsheetBinding::bind }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            behavior.skipCollapsed = true
            setOnShowListener {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBottomsheetBinding.inflate(inflater, container, false).root

}