package com.example.maite.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.CreateMaiteBottomSheet
import com.example.maite.ListDetailFragment
import com.example.maite.R
import com.example.maite.viewmodel.MaiteListViewModel
import com.example.maite.databinding.FragmentListBinding

//Fragment 만들 때 기본 코드로 사용
//class ListFragment : Fragment() {
//    private var _binding: FragmentListBinding? = null
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentListBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}

class ListFragment : Fragment() {
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MaiteListViewModel
    private lateinit var adapter: MaiteListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel 초기화
        viewModel = ViewModelProvider(this)[MaiteListViewModel::class.java]

        // 어댑터 초기화
        adapter = MaiteListAdapter { maiteListItem ->
            val fragment = ListDetailFragment.newInstance(maiteListItem)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, fragment)
                .addToBackStack(null)
                .commit()
        }

        // RecyclerView 설정
        binding.listRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ListFragment.adapter
        }

        // ViewModel에서 LiveData 관찰
        viewModel.maiteList.observe(viewLifecycleOwner) { maiteList ->
            adapter.submitList(maiteList)
        }

        viewModel.maiteList.observe(viewLifecycleOwner) { maiteList ->
            // 리스트가 비어있는지 확인
            if (maiteList.isNullOrEmpty()) {
                // 비어있으면: RecyclerView 숨기고, emptyTextView 보이기
                binding.listRV.visibility = View.GONE
                binding.emptyTextView.visibility = View.VISIBLE
            } else {
                // 데이터가 있으면: RecyclerView 보이고, emptyTextView 숨기기
                binding.listRV.visibility = View.VISIBLE
                binding.emptyTextView.visibility = View.GONE
            }
            // 어댑터에 리스트 제출 (리스트가 비어있어도 호출해야 함)
            adapter.submitList(maiteList)
        }

        binding.doneBtn.setOnClickListener {
            val bottomSheet = CreateMaiteBottomSheet.newInstance()
            bottomSheet.show(childFragmentManager, CreateMaiteBottomSheet.TAG)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}