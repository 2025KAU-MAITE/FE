package com.example.maite.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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

        // "MAITE 만들기" 버튼 클릭 리스너 설정
        binding.doneBtn.setOnClickListener {
            // 여기서 새 MAITE를 만드는 프래그먼트나 액티비티로 이동할 수 있습니다
            // 예:
            // findNavController().navigate(R.id.action_listFragment_to_createMaiteFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}