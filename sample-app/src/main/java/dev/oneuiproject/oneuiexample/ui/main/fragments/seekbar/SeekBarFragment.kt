package dev.oneuiproject.oneuiexample.ui.main.fragments.seekbar

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SeslSeekBar
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentSeekBarBinding
import dev.oneuiproject.oneui.ktx.updateDualColorRange
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared

class SeekBarFragment : AbsBaseFragment(R.layout.fragment_seek_bar) {

    private val binding by autoCleared { FragmentSeekBarBinding.bind(requireView()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seekbarSeamless = view.findViewById<SeslSeekBar>(R.id.seekbar_level_seamless)
        seekbarSeamless.setSeamless(true)

        val seekBarOverlap = view.findViewById<SeslSeekBar>(R.id.fragment_seekbar_overlap)
        seekBarOverlap.updateDualColorRange(70)
    }
}