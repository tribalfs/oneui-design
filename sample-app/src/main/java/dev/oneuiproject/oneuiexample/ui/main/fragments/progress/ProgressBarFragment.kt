package dev.oneuiproject.oneuiexample.ui.main.fragments.progress

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SeslProgressBar
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentProgressBinding
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.launchAndRepeatWithViewLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

class ProgressBarFragment : AbsBaseFragment(R.layout.fragment_progress) {

    private val binding by autoCleared { FragmentProgressBinding.bind(requireView()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listOf(
            binding.fragmentProgressbar1,
            binding.fragmentProgressbar2,
            binding.fragmentProgressbar3,
            binding.fragmentProgressbar4,
            binding.fragmentProgressbar5
        ).forEachIndexed { i, item ->
            launchAndRepeatWithViewLifecycle {
                if (i != 4) item.setMode(SeslProgressBar.MODE_CIRCLE)
                for (i in 1..100) {
                    item.progress = i
                    delay(80)
                    ensureActive()
                }
            }
        }
    }
}