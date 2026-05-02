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
    
    # Only compile XAudio 2.7 if NOT using Clang/MinGW
    # Or if you have a specific way to provide the legacy DXSDK headers
    if(NOT CMAKE_CXX_COMPILER_ID MATCHES "Clang")
        target_sources(CemuAudio PRIVATE
            XAudio27API.cpp
            XAudio27API.h
        )
    else()
        # Ensure Clang knows to skip XAudio 2.7 logic if there are references elsewhere
        target_compile_definitions(CemuAudio PRIVATE SKIP_XAUDIO_2_7)
    endif()

    if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
	    target_compile_definitions(CemuAudio PRIVATE 
	        UNICODE 
	        _UNICODE
	        "__reserved="
	        # Fix for "wrong <xaudio2.h> included!"
	        # Cemu looks for this to skip the #error line
	        XAUDIO2_HELPER_FUNCTIONS 
	        # Fix for "use of undeclared identifier 'XAUDIO2_DLL'"
	        # We tell it to look for the Win10+ system DLL
	        XAUDIO2_DLL=L"xaudio2_9.dll"
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
