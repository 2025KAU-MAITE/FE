package com.example.maite

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.adapter.MateAdapter
import com.example.maite.databinding.FragmentMateListBinding
import com.example.maite.model.MateItem
import com.example.maite.model.ServerMateItem
import com.example.maite.model.toMateItem
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MateListFragment : Fragment() {

    private var _binding: FragmentMateListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var mateAdapter: MateAdapter
    private lateinit var apiService: MaiteApiService

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
        
        // API 서비스 초기화
        apiService = ApiClient.getClient(requireContext()).create(MaiteApiService::class.java)
        
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
                val response = apiService.getMates()
                
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    val serverMateList = responseBody.result ?: emptyList()
                    
                    // ServerMateItem을 MateItem으로 변환
                    val mates = serverMateList.map { serverMate: ServerMateItem ->
                        serverMate.toMateItem()
                    }
                    
                    // 어댑터에 데이터 설정
                    mateAdapter.submitList(mates)
                    
                    // 빈 목록 처리
                    if (mates.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                    }
                } else {
                    // 실패 처리
                    Toast.makeText(requireContext(), "친구 목록을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: IOException) {
                // 네트워크 오류
                Log.e("MateListFragment", "네트워크 오류", e)
                Toast.makeText(requireContext(), "네트워크 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
                binding.tvEmpty.visibility = View.VISIBLE
            } catch (e: HttpException) {
                // API 오류
                Log.e("MateListFragment", "API 오류: ${e.code()}", e)
                Toast.makeText(requireContext(), "서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                binding.tvEmpty.visibility = View.VISIBLE
            } catch (e: Exception) {
                // 기타 오류
                Log.e("MateListFragment", "친구 목록 로드 오류", e)
                Toast.makeText(requireContext(), "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
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
                // userId를 long으로 변환
                val userId = mate.userId
                
                // 현재는 UserApiService에 deleteMate 메서드가 없으므로 임시로 기능 처리
                // 실제 API 구현 시 API 호출 부분 추가 필요
                
                // 임시로 성공 처리
                Toast.makeText(requireContext(), "${mate.name}님이 친구 목록에서 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                
                // 가째 데이터 업데이트를 위해 임시로 삭제된 항목을 제외한 새 리스트 생성
                val currentList = mateAdapter.currentList.toMutableList()
                currentList.removeAll { it.id == mate.id }
                mateAdapter.submitList(currentList)
                
                // 빈 목록 처리
                if (currentList.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                }
                
                // 프로필 화면의 친구 수 업데이트를 위해 이벤트 발생
                val mainActivity = requireActivity() as? MainActivity
                mainActivity?.refreshProfileFragment()
                
            } catch (e: Exception) {
                Log.e("MateListFragment", "친구 삭제 오류", e)
                Toast.makeText(requireContext(), "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
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