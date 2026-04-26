add_library(CemuAudio
	IAudioAPI.cpp
	IAudioAPI.h
	IAudioInputAPI.cpp
	IAudioInputAPI.h
)

set_property(TARGET CemuAudio PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

if(WIN32)
	target_sources(CemuAudio PRIVATE
		DirectSoundAPI.cpp
		DirectSoundAPI.h
		XAudio2API.cpp
		XAudio2API.h
	)
	if(NOT CMAKE_CXX_COMPILER_ID MATCHES "Clang")
        target_sources(CemuAudio PRIVATE
			XAudio27API.cpp
			XAudio27API.h
		)
	endif()
	if(WIN32 AND CMAKE_CXX_COMPILER_ID MATCHES "Clang")
	    # 1. DO NOT apply -fms-compatibility globally if using MSYS2 headers.
	    # Instead, we only use extensions for specific MSVC-isms.
	    target_compile_options(CemuAudio PRIVATE -fms-extensions)
	
	    # 2. Fix the XAudio2 DLL define
	    target_compile_definitions(CemuAudio PRIVATE XAUDIO2_DLL=L"xaudio2_9.dll")
	
	    # 3. FIX: Only add the MSYS2 include path if it's not already at the top.
	    # But more importantly, ensure the DirectX_2010 dependency is LAST.
	    target_include_directories(CemuAudio SYSTEM AFTER PRIVATE "${CMAKE_SOURCE_DIR}/dependencies/DirectX_2010")
	
	    # 4. If XAudio2API.cpp specifically needs SAL annotations (__out, etc), 
	    # we define them as empty for Clang instead of using MSVC-compatibility mode.
	    target_compile_definitions(CemuAudio PRIVATE 
	        "__out=" 
	        "__in=" 
	        "__in_opt=" 
	        "__out_opt="
	        "__deref_out="
	    )
	endif()
endif()

if(ENABLE_CUBEB)
	target_sources(CemuAudio PRIVATE
		CubebAPI.cpp
		CubebAPI.h
		CubebInputAPI.cpp
		CubebInputAPI.h
	)
	#add_compile_definitions(HAS_CUBEB)
endif()

target_include_directories(CemuAudio PUBLIC "../")

target_link_libraries(CemuAudio PRIVATE
	CemuCommon
	CemuGui
)

if(ENABLE_CUBEB)
	# PUBLIC because cubeb.h/cubeb.h is included in CubebAPI.h
	target_link_libraries(CemuAudio PUBLIC cubeb::cubeb)
endif()
