package com.example.maite.repository

import com.example.maite.ApiClient
import com.example.maite.model.AuthApi
import com.example.maite.model.EmailCheckResponse
import com.example.maite.model.EmailCheckResult
import com.example.maite.model.SignupRequest
import com.example.maite.model.SignupResponse
import com.example.maite.model.SignupResult
import com.example.maite.model.SmsAuthResponse
import com.example.maite.model.SmsAuthResult
import com.example.maite.model.SmsAuthSendRequest
import com.example.maite.model.SmsAuthVerifyRequest
import com.example.maite.model.LoginRequest
import com.example.maite.model.LoginResponse
import com.example.maite.model.LoginResult
import com.example.maite.model.GoogleLoginRequest
import com.example.maite.model.GoogleLoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.util.Log
import android.content.Context
import com.example.maite.UserInfoResponse
import com.example.maite.UserInfoResult
import com.example.maite.model.SocialSignupRequest
import com.example.maite.model.SocialSignupResponse
import com.example.maite.model.SocialSignupResult
import com.example.maite.model.FindIdResponse
import com.example.maite.model.FindIdResult
import com.example.maite.model.FindIdSendRequest
import com.example.maite.model.FindIdVerifyRequest
import com.example.maite.model.ResetPasswordSendRequest
import com.example.maite.model.ResetPasswordVerifyRequest
import com.example.maite.model.ResetPasswordUpdateRequest
import com.example.maite.model.ResetPasswordResponse
import com.example.maite.model.ResetPasswordResult

class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"
    private val authApi = ApiClient.getClient(context).create(AuthApi::class.java)
    
    // 테스트용 이메일 목록 (사용 중인 이메일로 간주)
    private val usedEmails = listOf("test@example.com", "used@gmail.com", "taken@naver.com")
    
    // 테스트용 인증번호
    private val testVerificationCode = "123456"
    
    /**
     * 이메일 중복 확인 (서버 API 연동)
     */
    suspend fun checkEmailDuplicate(email: String): EmailCheckResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "서버 이메일 중복 확인 API 호출: $email")
                
                // 실제 API 호출
                val response = authApi.checkEmailDuplicate(email)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}, duplicated=${response.result.duplicated}")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "이메일 중복 확인 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                EmailCheckResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = EmailCheckResult(
                        duplicated = false,
                        message = "이메일 중복 확인 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }
    
    /**
     * SMS 인증번호 발송 (서버 API 연동)
     */
    suspend fun sendSmsAuth(phoneNumber: String): SmsAuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "서버 SMS 인증번호 발송 API 호출: $phoneNumber")
                
                // POST 요청에 필요한 데이터 생성
                val request = SmsAuthSendRequest(phonenumber = phoneNumber)
                Log.d(TAG, "SMS 인증 요청 데이터: $request")
                
                // 실제 API 호출 전 로그
                Log.d(TAG, "API 호출 시작: auth/signup/send-code 엔드포인트로 요청 전송")
                
                // 실제 API 호출
                val response = authApi.sendSmsAuth(request)
                
                // API 응답 상세 로깅
                Log.d(TAG, "API 응답 받음: isSuccess=${response.isSuccess}, code=${response.code}, message=${response.message}")
                Log.d(TAG, "응답 결과 상세: ${response.result}")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "SMS 인증번호 발송 API 오류: ${e.message}", e)
                Log.e(TAG, "상세 예외 정보: ${e.javaClass.name}")
                if (e.cause != null) {
                    Log.e(TAG, "원인 예외: ${e.cause?.message}", e.cause)
                }
                
                // API 호출 실패 시 오류 응답 생성
                SmsAuthResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = SmsAuthResult(
                        message = "SMS 인증번호 발송 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }
    
    /**
     * SMS 인증번호 확인 (서버 API 연동)
     */
    suspend fun verifySmsAuth(phoneNumber: String, verificationCode: String): SmsAuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "서버 SMS 인증번호 확인 API 호출: 전화번호=$phoneNumber, 인증번호=$verificationCode")
                
                // POST 요청에 필요한 데이터 생성
                val request = SmsAuthVerifyRequest(
                    phonenumber = phoneNumber,
                    verificationCode = verificationCode
                )
                
                // 실제 API 호출
                val response = authApi.verifySmsAuth(request)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                Log.d(TAG, "결과: message=${response.result.message}")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "SMS 인증번호 확인 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                SmsAuthResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = SmsAuthResult(
                        message = "SMS 인증번호 확인 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }

    /**
     * 회원가입 처리 (서버 API 연동)
     */
    suspend fun signup(email: String, password: String, name: String, phoneNumber: String, address: String): SignupResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "회원가입 API 호출: 이메일=$email, 이름=$name, 전화번호=$phoneNumber, 주소=$address")
                
                // POST 요청에 필요한 데이터 생성
                val request = SignupRequest(
                    email = email,
                    password = password,
                    name = name,
                    phoneNumber = phoneNumber,
                    address = address
                )
                
                // 실제 API 호출
                val response = authApi.signup(request)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                Log.d(TAG, "회원가입 결과: userId=${response.result.userId}, email=${response.result.email}, registered=${response.result.registered}")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "회원가입 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                SignupResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = SignupResult(
                        userId = 0,
                        email = email,
                        name = name,
                        registeredAt = "",
                        message = "회원가입 처리 중 오류가 발생했습니다",
                        registered = false
                    )
                )
            }
        }
    }

    /**
     * 로그인 처리 (서버 API 연동)
     */
    suspend fun login(email: String, password: String): LoginResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "로그인 API 호출: 이메일=$email")
                
                // POST 요청에 필요한 데이터 생성
                val request = LoginRequest(
                    email = email,
                    password = password
                )
                
                // 실제 API 호출
                val response = authApi.login(request)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                Log.d(TAG, "로그인 결과: accessToken=${response.result.accessToken.take(10)}...")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "로그인 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                LoginResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = LoginResult(
                        accessToken = "",
                        message = "로그인 처리 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }

    /**
     * Google 로그인 처리 (서버 API 연동)
     * 서버에서 HTTP 200이면 로그인 성공, 500이면 미등록 사용자
     */
    suspend fun googleLogin(idToken: String, accessToken: String): LoginResponse {
        return withContext(Dispatchers.IO) {
            try {
                // 로그 보안을 위해 토큰의 일부만 출력
                Log.d(TAG, "Google 로그인 API 호출: idToken 길이=${idToken.length}, accessToken 길이=${accessToken.length}")
                Log.d(TAG, "ID 토큰 프리뷰: ${idToken.take(20)}...")
                Log.d(TAG, "액세스 토큰 프리뷰: ${accessToken.take(20)}...")
                
                // POST 요청에 필요한 데이터 생성
                val request = GoogleLoginRequest(
                    idToken = idToken,
                    accessToken = accessToken
                )
                
                // 요청 데이터 로깅
                Log.d(TAG, "Google 로그인 요청 객체: idToken=${idToken.take(15)}..., accessToken=${accessToken.take(15)}...")
                
                // 실제 API 호출
                Log.d(TAG, "Google 로그인 API 요청: URL=/auth/login-google")
                
                // API 호출 및 응답 처리
                val response = try {
                    val apiResponse = authApi.googleLogin(request)
                    // 성공 응답 상세 로깅
                    Log.d(TAG, "Google 로그인 API 성공 응답: HTTP 200")
                    Log.d(TAG, "응답 데이터: isSuccess=${apiResponse.isSuccess}, code=${apiResponse.code}, message=${apiResponse.message}")
                    apiResponse
                } catch (e: retrofit2.HttpException) {
                    Log.e(TAG, "Google 로그인 API HTTP 에러: ${e.code()}")
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e(TAG, "에러 응답: $errorBody")
                    
                    // 서버 에러이지만 응답 내용이 "미등록 사용자"인 경우 회원가입 처리로 유도
                    if (e.code() == 500 || e.code() == 403) {
                        val needSignup = errorBody?.contains("not registered") == true || 
                                         errorBody?.contains("미등록") == true ||
                                         errorBody?.contains("signup") == true ||
                                         errorBody?.contains("가입") == true
                                      
                        if (needSignup) {
                            return@withContext LoginResponse(
                                isSuccess = false,
                                code = "NEED_SIGNUP",
                                message = "Google 계정으로 가입이 필요합니다",
                                result = LoginResult(
                                    accessToken = "",
                                    message = "미등록 Google 사용자입니다. 회원가입이 필요합니다."
                                )
                            )
                        }
                    }
                    throw e  // 상위 catch 블록에서 처리하도록 다시 throw
                }
                
                // GoogleLoginResponse를 LoginResponse로 변환
                // 서버 응답에 따라 처리
                LoginResponse(
                    isSuccess = response.isSuccess,
                    code = response.code,
                    message = response.message,
                    result = LoginResult(
                        accessToken = response.result.accessToken ?: "",
                        message = response.result.message
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Google 로그인 API 오류: ${e.message}", e)
                
                // 예외 유형 및 응답 코드에 따라 다르게 처리
                if (e is retrofit2.HttpException) {
                    val errorCode = e.code()
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    Log.e(TAG, "HTTP 에러: $errorCode, 에러 본문: $errorBody")
                    
                    // 에러 본문에서 필요한 정보 추출 시도
                    val isUserExist = errorBody.contains("already exist") || 
                                      errorBody.contains("이미 존재") || 
                                      errorBody.contains("registered")
                                      
                    if (isUserExist) {
                        // 이미 가입된 계정이라면 로그인 시도로 처리
                        return@withContext LoginResponse(
                            isSuccess = true,  // 기존 계정이 있으므로 성공으로 처리
                            code = "USER_EXISTS",
                            message = "이미 가입된 Google 계정입니다. 로그인을 진행합니다.",
                            result = LoginResult(
                                accessToken = "", // 토큰은 없지만 로그인 처리 시도
                                message = "기존 계정으로 로그인을 시도하세요."
                            )
                        )
                    } else {
                        // 그 외 HTTP 에러는 회원가입 필요로 처리
                        return@withContext LoginResponse(
                            isSuccess = false,
                            code = "NEED_SIGNUP",
                            message = "Google 계정으로 가입이 필요합니다",
                            result = LoginResult(
                                accessToken = "",
                                message = "미등록 Google 사용자입니다. 회원가입이 필요합니다."
                            )
                        )
                    }
                }
                
                // 기타 예외는 일반 오류로 처리
                LoginResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "로그인 처리 중 오류가 발생했습니다: ${e.message}",
                    result = LoginResult(
                        accessToken = "",
                        message = "오류가 발생했습니다. 다시 시도해주세요."
                    )
                )
            }
        }
    }

    suspend fun getUserInfo(token: String): UserInfoResponse {
        return withContext(Dispatchers.IO) {
            try {
                authApi.getUserInfo("Bearer $token")
            } catch (e: Exception) {
                UserInfoResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "사용자 정보 조회 실패: ${e.message}",
                    result = UserInfoResult(
                        userId = 0L,
                        email = "",
                        name = ""
                    )
                )
            }
        }
    }

    /**
     * 소셜 로그인 회원가입 처리 (서버 API 연동)
     */
    suspend fun completeSocialSignup(
        email: String,
        name: String,
        provider: String,
        phoneNumber: String,
        address: String
    ): SocialSignupResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "소셜 로그인 회원가입 API 호출: 이메일=$email, 이름=$name, 제공자=$provider, 전화번호=$phoneNumber, 주소=$address")
                
                // 요청 URL을 로그로 출력
                val requestUrl = "auth/complete-social-signup?email=$email&name=$name&provider=$provider&phonenumber=$phoneNumber&address=$address"
                Log.d(TAG, "요청 URL: $requestUrl")
                
                // 토큰들을 가져옴
                val idToken = com.example.maite.model.SignupDataHolder.idToken
                val accessToken = com.example.maite.model.SignupDataHolder.accessToken
                
                // 토큰이 없으면 로그 출력
                if (idToken.isEmpty()) {
                    Log.e(TAG, "ID 토큰이 비어 있습니다. 소셜 로그인 과정에서 토큰이 저장되지 않았을 수 있습니다.")
                }
                
                if (accessToken.isEmpty()) {
                    Log.e(TAG, "Access 토큰이 비어 있습니다. 소셜 로그인 과정에서 액세스 토큰이 저장되지 않았을 수 있습니다.")
                }
                
                val authHeader = if (idToken.isNotEmpty()) "Bearer $idToken" else null
                
                // API 문서에 따라 쿼리 파라미터 사용
                // SocialSignupRequest 객체 사용 안함 (쿼리 파라미터로 전달)
                
                // 요청 내용 로깅 - 쿼리 파라미터
                Log.d(TAG, "요청 쿼리 파라미터: email=$email, name=$name, provider=$provider, phonenumber=$phoneNumber, address=$address")
                
                // idToken과 인증 헤더는 사용하지 않음 (API 문서에 따라 쿼리 파라미터만 필요)
                
                // 실제 API 호출 - 쿼리 파라미터로 전달 (API 문서에 따름)
                val response = authApi.completeSocialSignup(
                    email = email,
                    name = name,
                    provider = provider,
                    phonenumber = phoneNumber,
                    address = address
                )
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                Log.d(TAG, "회원가입 결과: userId=${response.result.userId}, email=${response.result.email}, registered=${response.result.registered}")
                
                response
            } catch (e: Exception) {
                // 상세한 오류 처리
                when (e) {
                    is retrofit2.HttpException -> {
                        val errorCode = e.code()
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e(TAG, "소셜 로그인 회원가입 API HTTP 오류: 코드=$errorCode, 본문=$errorBody", e)
                        
                        // 오류 메시지 추출 시도
                        var errorMessage = "서버 연결 오류: HTTP $errorCode"
                        try {
                            // JSON 파싱 시도
                            if (!errorBody.isNullOrEmpty()) {
                                val jsonObject = org.json.JSONObject(errorBody)
                                if (jsonObject.has("message")) {
                                    errorMessage = jsonObject.getString("message")
                                    Log.d(TAG, "서버 오류 메시지: $errorMessage")
                                }
                            }
                        } catch (jsonEx: Exception) {
                            Log.e(TAG, "JSON 파싱 오류", jsonEx)
                        }
                        
                        // 특정 에러 코드에 따른 처리
                        when (errorCode) {
                            400 -> {
                                // 이미 존재하는 이메일 등의 케이스
                                Log.e(TAG, "잘못된 요청 (400 Bad Request): $errorMessage")
                            }
                            403 -> {
                                Log.e(TAG, "권한 오류 (403 Forbidden): API 호출 권한이 없습니다.")
                            }
                        }
                        
                        // 적절한 오류 응답 반환
                        return@withContext SocialSignupResponse(
                            isSuccess = false,
                            code = "ERROR_$errorCode",
                            message = errorMessage,
                            result = SocialSignupResult(
                                userId = 0,
                                email = email,
                                name = name,
                                registeredAt = "",
                                message = errorMessage,
                                registered = false,
                                accessToken = null,
                                idToken = null
                            )
                        )
                    }
                    else -> {
                        Log.e(TAG, "소셜 로그인 회원가입 API 오류: ${e.message}", e)
                    }
                }
                
                // 기타 예외 처리를 위한 기본 오류 응답 생성
                SocialSignupResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = SocialSignupResult(
                        userId = 0,
                        email = email,
                        name = name,
                        registeredAt = "",
                        message = "소셜 로그인 회원가입 처리 중 오류가 발생했습니다",
                        registered = false,
                        accessToken = null,
                        idToken = null
                    )
                )
            }
        }
    }
    /**
     * 아이디 찾기 - SMS 인증번호 발송 (서버 API 연동)
     */
    suspend fun sendFindIdSmsAuth(name: String, phoneNumber: String): SmsAuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "아이디 찾기 SMS 인증번호 발송 API 호출: 이름=$name, 전화번호=$phoneNumber")
                
                // POST 요청에 필요한 데이터 생성
                val request = FindIdSendRequest(name = name, phonenumber = phoneNumber)
                Log.d(TAG, "SMS 인증 요청 데이터: $request")
                
                try {
                    // 실제 API 호출 - 토큰 없이 단순 요청
                    val response = authApi.sendFindIdSmsAuth(request)
                    
                    // API 응답 상세 로깅
                    Log.d(TAG, "API 응답 받음: isSuccess=${response.isSuccess}, code=${response.code}, message=${response.message}")
                    Log.d(TAG, "응답 결과 상세: ${response.result}")
                    
                    return@withContext response
                } catch (e: retrofit2.HttpException) {
                    Log.e(TAG, "HTTP 에러 발생: ${e.code()}", e)
                    // HTTP 에러 발생 시 더 자세한 오류 정보 기록
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e(TAG, "에러 응답 바디: $errorBody")
                    
                    return@withContext SmsAuthResponse(
                        isSuccess = false,
                        code = "ERROR_${e.code()}",
                        message = "서버 오류: ${e.message()}",
                        result = SmsAuthResult(
                            message = "SMS 인증번호 발송 중 오류가 발생했습니다 (HTTP ${e.code()})"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "아이디 찾기 SMS 인증번호 발송 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                SmsAuthResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = SmsAuthResult(
                        message = "SMS 인증번호 발송 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }

    /**
     * 아이디 찾기 - SMS 인증번호 확인 (서버 API 연동)
     */
    suspend fun verifyFindId(name: String, phoneNumber: String, verificationCode: String): FindIdResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "아이디 찾기 인증번호 확인 API 호출: 이름=$name, 전화번호=$phoneNumber, 인증번호=$verificationCode")
                
                // POST 요청에 필요한 데이터 생성
                val request = FindIdVerifyRequest(
                    name = name,
                    phonenumber = phoneNumber,
                    verificationCode = verificationCode
                )
                
                try {
                    // 실제 API 호출 - 토큰 필요 없음
                    val response = authApi.verifyFindId(request)
                    
                    Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                    
                    // 서버에서 반환한 실제 이메일 정보 로깅
                    if (response.isSuccess) {
                        Log.d(TAG, "인증 성공, 서버에서 이메일 수신: ${response.result.email}")
                    }
                    return@withContext response
                } catch (e: retrofit2.HttpException) {
                    Log.e(TAG, "HTTP 에러 발생: ${e.code()}", e)
                    // HTTP 에러 발생 시 더 자세한 오류 정보 기록
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e(TAG, "에러 응답 바디: $errorBody")
                    
                    return@withContext FindIdResponse(
                        isSuccess = false,
                        code = "ERROR_${e.code()}",
                        message = "서버 오류: ${e.message()}",
                        result = FindIdResult(
                            status = false,
                            email = "",
                            message = "인증번호 확인 중 오류가 발생했습니다 (HTTP ${e.code()})"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "아이디 찾기 인증번호 확인 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                FindIdResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = FindIdResult(
                        status = false,
                        email = "",
                        message = "아이디 찾기 인증번호 확인 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }

    /**
     * 비밀번호 찾기 - 인증번호 발송 (서버 API 연동)
     */
    suspend fun sendResetPasswordCode(name: String, email: String, phoneNumber: String): SmsAuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "비밀번호 찾기 인증번호 발송 API 호출: 이름=$name, 이메일=$email, 전화번호=$phoneNumber")
                
                // POST 요청에 필요한 데이터 생성
                val request = ResetPasswordSendRequest(
                    name = name,
                    email = email,
                    phonenumber = phoneNumber
                )
                
                try {
                    // 실제 API 호출
                    val response = authApi.sendResetPasswordCode(request)
                    
                    // API 응답 상세 로깅
                    Log.d(TAG, "API 응답 받음: isSuccess=${response.isSuccess}, code=${response.code}, message=${response.message}")
                    Log.d(TAG, "응답 결과 상세: ${response.result}")
                    
                    return@withContext response
                } catch (e: retrofit2.HttpException) {
                    Log.e(TAG, "HTTP 에러 발생: ${e.code()}", e)
                    // HTTP 에러 발생 시 더 자세한 오류 정보 기록
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e(TAG, "에러 응답 바디: $errorBody")
                    
                    return@withContext SmsAuthResponse(
                        isSuccess = false,
                        code = "ERROR_${e.code()}",
                        message = "서버 오류: ${e.message()}",
                        result = SmsAuthResult(
                            message = "인증번호 발송 중 오류가 발생했습니다 (HTTP ${e.code()})"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "비밀번호 찾기 인증번호 발송 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                SmsAuthResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = SmsAuthResult(
                        message = "인증번호 발송 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }
    
    /**
     * 비밀번호 찾기 - 인증번호 확인 (서버 API 연동)
     */
    suspend fun verifyResetPasswordCode(phoneNumber: String, verificationCode: String): ResetPasswordResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "비밀번호 찾기 인증번호 확인 API 호출: 전화번호=$phoneNumber, 인증코드=$verificationCode")
                
                // POST 요청에 필요한 데이터 생성
                val request = ResetPasswordVerifyRequest(
                    phonenumber = phoneNumber,
                    verificationCode = verificationCode
                )
                
                try {
                    // 실제 API 호출
                    val response = authApi.verifyResetPasswordCode(request)
                    
                    Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                    
                    if (response.isSuccess) {
                        Log.d(TAG, "인증 성공, 이메일: ${response.getEmail()}")
                    }
                    
                    return@withContext response
                } catch (e: retrofit2.HttpException) {
                    Log.e(TAG, "HTTP 에러 발생: ${e.code()}", e)
                    // HTTP 에러 발생 시 더 자세한 오류 정보 기록
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e(TAG, "에러 응답 바디: $errorBody")
                    
                    return@withContext ResetPasswordResponse(
                        isSuccess = false,
                        code = "ERROR_${e.code()}",
                        message = "서버 오류: ${e.message()}",
                        result = ResetPasswordResult(
                            status = false,
                            message = "인증번호 확인 중 오류가 발생했습니다 (HTTP ${e.code()})"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "비밀번호 찾기 인증번호 확인 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                ResetPasswordResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = ResetPasswordResult(
                        status = false,
                        message = "인증번호 확인 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }
    
    /**
     * 비밀번호 업데이트 (서버 API 연동)
     */
    suspend fun resetPassword(email: String, password: String): ResetPasswordResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "비밀번호 업데이트 API 호출: 이메일=$email")
                
                // POST 요청에 필요한 데이터 생성
                val request = ResetPasswordUpdateRequest(
                    email = email,
                    password = password
                )
                
                try {
                    // 실제 API 호출
                    val response = authApi.resetPassword(request)
                    
                    Log.d(TAG, "Reset password API response: isSuccess=${response.isSuccess}, message=${response.message}")
                    
                    return@withContext response
                } catch (e: retrofit2.HttpException) {
                    Log.e(TAG, "HTTP 에러 발생: ${e.code()}", e)
                    // HTTP 에러 발생 시 더 자세한 오류 정보 기록
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e(TAG, "에러 응답 바디: $errorBody")
                    
                    return@withContext ResetPasswordResponse(
                        isSuccess = false,
                        code = "ERROR_${e.code()}",
                        message = "서버 오류: ${e.message()}",
                        result = ResetPasswordResult(
                            status = false,
                            message = "비밀번호 업데이트 중 오류가 발생했습니다 (HTTP ${e.code()})"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "비밀번호 업데이트 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                ResetPasswordResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = ResetPasswordResult(
                        status = false,
                        message = "비밀번호 업데이트 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }
}