package com.example.maite

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResultListener
import com.example.maite.databinding.BottomSheetCreateMaiteBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.maite.R

class CreateMaiteBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateMaiteBinding? = null
    private val binding get() = _binding!!

    private var isDoneButtonEnabled = false

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            checkInputsAndUpdateButtonState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFragmentResultListener()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCreateMaiteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.titleEditText.addTextChangedListener(textWatcher)
        binding.introEditText.addTextChangedListener(textWatcher)

        checkInputsAndUpdateButtonState()

        binding.addBtn.setOnClickListener {
            val inviteBottomSheet = InviteBottomSheet.newInstance()
            inviteBottomSheet.show(parentFragmentManager, InviteBottomSheet::class.java.simpleName)
        }

        binding.doneBtn.setOnClickListener {
            if (isDoneButtonEnabled) {
                val title = binding.titleEditText.text.toString()
                val intro = binding.introEditText.text.toString()

                dismiss()
                Toast.makeText(requireContext(), "MAITE 생성 완료", Toast.LENGTH_SHORT).show()
            }
        }
        updateInvitedUsersUI(0)
    }

    private fun setupFragmentResultListener() {
        setFragmentResultListener(InviteBottomSheet.REQUEST_KEY) { requestKey, bundle ->
            if (requestKey == InviteBottomSheet.REQUEST_KEY) {
                val selectedCount = bundle.getInt(InviteBottomSheet.KEY_SELECTED_COUNT, 0)
                updateInvitedUsersUI(selectedCount)
            }
        }
    }

    private fun updateInvitedUsersUI(count: Int) {
        val childrenToRemove = mutableListOf<View>()
        for (i in 0 until binding.invitedUsersLayout.childCount) {
            val child = binding.invitedUsersLayout.getChildAt(i)
            if (child is ImageView && child.id != R.id.addBtn) {
                childrenToRemove.add(child)
            }
        }
        childrenToRemove.forEach { binding.invitedUsersLayout.removeView(it) }

        if (count > 0) {
            val imageSize = resources.getDimensionPixelSize(R.dimen.invited_profile_img_size)
//            val imageMarginEnd = resources.getDimensionPixelSize(R.dimen.invited_profile_img_size)
            val desiredMarginDp = 8
            val imageMarginEnd = (desiredMarginDp * resources.displayMetrics.density).toInt()

            val addBtn = binding.invitedUsersLayout.findViewById<ImageView>(R.id.addBtn)
            val addBtnIndex = if (addBtn != null) binding.invitedUsersLayout.indexOfChild(addBtn) else 0

            for (i in 0 until count) {
                val imageView = ImageView(requireContext())
                val layoutParams = LinearLayout.LayoutParams(imageSize, imageSize)

                layoutParams.marginEnd = imageMarginEnd

                imageView.layoutParams = layoutParams
                imageView.setImageResource(R.drawable.img_profile_default)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                binding.invitedUsersLayout.addView(imageView, addBtnIndex + i)
            }
        }
        binding.invitedUsersScrollView.post {
            binding.invitedUsersScrollView.fullScroll(View.FOCUS_RIGHT)
        }
    }


    private fun checkInputsAndUpdateButtonState() {
        val title = binding.titleEditText.text.toString().trim()
        val intro = binding.introEditText.text.toString().trim()
        isDoneButtonEnabled = title.isNotEmpty() && intro.isNotEmpty()
        updateDoneButtonAppearance(isDoneButtonEnabled)
    }

    private fun updateDoneButtonAppearance(isEnabled: Boolean) {
        val context = requireContext()
        binding.doneBtn.isClickable = isEnabled
        if (isEnabled) {
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.mainColor))
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.btn_inactive))
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.white))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.titleEditText.removeTextChangedListener(textWatcher)
        binding.introEditText.removeTextChangedListener(textWatcher)
        _binding = null
    }

    companion object {
        const val TAG = "CreateMaiteBottomSheet"
        fun newInstance(): CreateMaiteBottomSheet {
            return CreateMaiteBottomSheet()
        }
    }
}