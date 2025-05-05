package com.example.maite.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        // ViewModel 초기화 (AndroidViewModel 방식)
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application))
            .get(MaiteListViewModel::class.java)

        // 어댑터 초기화 및 클릭 리스너 설정
        setupRecyclerView()

        // ViewModel에서 LiveData 관찰
        observeViewModel()

        // 새로고침 버튼이나 SwipeRefreshLayout 추가 고려 (선택 사항)
        // binding.swipeRefreshLayout.setOnRefreshListener {
        //     viewModel.loadMaiteList()
        // }

        binding.doneBtn.setOnClickListener {
            val bottomSheet = CreateMaiteBottomSheet.newInstance()
            bottomSheet.show(childFragmentManager, CreateMaiteBottomSheet.TAG)
        }
    }

    private fun setupRecyclerView() {
        adapter = MaiteListAdapter { maiteListItem ->
            // RoomItem에서 변환된 MaiteListItem 사용
            // 만약 RoomItem 자체를 전달해야 한다면 ListDetailFragment 및 어댑터 수정 필요
            val fragment = ListDetailFragment.newInstance(maiteListItem)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.listRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ListFragment.adapter
        }
    }

    private fun observeViewModel() {
        // Maite 목록 관찰
        viewModel.maiteList.observe(viewLifecycleOwner) { maiteList ->
            adapter.submitList(maiteList)
            // 데이터 유무에 따라 빈 상태 UI 표시 (선택 사항)
            // binding.emptyView.visibility = if (maiteList.isEmpty()) View.VISIBLE else View.GONE
        }

        // 오류 메시지 관찰 (선택 사항: Toast 메시지 표시)
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                // 오류 발생 시 사용자에게 알림
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding.listRV.adapter = null // 메모리 누수 방지를 위해 어댑터 참조 해제
        _binding = null
    }
}