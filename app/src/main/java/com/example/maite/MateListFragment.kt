package com.example.maite

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.adapter.MateAdapter
import com.example.maite.databinding.FragmentMateListBinding
import com.example.maite.model.MateItem
import com.example.maite.model.ServerMateItem
import com.example.maite.model.toMateItem
import com.example.maite.repository.MateRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MateListFragment : Fragment() {

    private var _binding: FragmentMateListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var mateAdapter: MateAdapter
    private lateinit var mateRepository: MateRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMateListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Repository 초기화
        mateRepository = MateRepository(requireContext())
        
        // 어댑터 초기화
        mateAdapter = MateAdapter { mate ->
            // 친구 삭제 클릭 처리
            deleteMate(mate)
        }
        
        // RecyclerView 설정
        binding.rvMates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mateAdapter
            setHasFixedSize(true)
        }
        
        // 뒤로가기 버튼 설정
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        // 친구 목록 로드
        loadMates()
    }
    
    private fun loadMates() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val mates = mateRepository.getMates()
                
                // 어댑터에 데이터 설정
                mateAdapter.submitList(mates)
                
                // 빈 목록 처리
                if (mates.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                }
                
                Log.d("MateListFragment", "친구 목록 로드 성공: ${mates.size}명")
            } catch (e: Exception) {
                Log.e("MateListFragment", "친구 목록 로드 오류", e)
                binding.tvEmpty.visibility = View.VISIBLE
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun deleteMate(mate: MateItem) {
        // 확인 다이얼로그 표시
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("친구 삭제")
            .setMessage("${mate.name}님을 친구 목록에서 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                performDeleteMate(mate)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun performDeleteMate(mate: MateItem) {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Repository를 통해 실제 API 호출
                val success = mateRepository.deleteMate(mate.userId)
                
                if (success) {
                    // 성공 시 친구 목록 재로드로 확실하게 동기화
                    val updatedMates = mateRepository.getMates()
                    mateAdapter.submitList(updatedMates)
                    
                    // 빈 목록 처리
                    if (updatedMates.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                    }
                    
                    // 프로필 화면의 친구 수 업데이트를 위해 이벤트 발생
                    val mainActivity = requireActivity() as? MainActivity
                    mainActivity?.refreshProfileFragment()
                    
                    Log.d("MateListFragment", "친구 삭제 성공: ${mate.name} (userId: ${mate.userId})")
                } else {
                    // 실패 시 에러 메시지 표시
                    android.widget.Toast.makeText(
                        requireContext(),
                        "친구 삭제에 실패했습니다. 다시 시도해주세요.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    
                    Log.e("MateListFragment", "친구 삭제 실패: ${mate.name} (userId: ${mate.userId})")
                }
                
            } catch (e: Exception) {
                Log.e("MateListFragment", "친구 삭제 중 예외 발생", e)
                android.widget.Toast.makeText(
                    requireContext(),
                    "네트워크 오류가 발생했습니다.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        const val TAG = "MateListFragment"
    }
}