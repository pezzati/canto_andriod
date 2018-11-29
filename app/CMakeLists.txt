cmake_minimum_required(VERSION 3.4.1)

# convert SDK path to forward slashes on Windows
file(TO_CMAKE_PATH ${PATH_TO_SUPERPOWERED} PATH_TO_SUPERPOWERED)

set(CMAKE_VERBOSE_MAKEFILE on)

include_directories(src/main/cpp)
include_directories(${PATH_TO_SUPERPOWERED})
#include_directories(${PATH_TO_FFMPEG}/${ANDROID_ABI}/include)

#[[add_library(
        avutil
        SHARED
        IMPORTED
)
#set_target_properties(avutil PROPERTIES LINKER_LANGUAGE CXX)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ${PATH_TO_FFMPEG}/${ANDROID_ABI}/libavutil.so)

add_library(
        avformat
        SHARED
        IMPORTED
)
#set_target_properties(avformat PROPERTIES LINKER_LANGUAGE CXX)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ${PATH_TO_FFMPEG}/${ANDROID_ABI}/libavformat.so)

add_library(
        avcodec
        SHARED
        IMPORTED
)
#set_target_properties(avformat PROPERTIES LINKER_LANGUAGE CXX)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ${PATH_TO_FFMPEG}/${ANDROID_ABI}/libavcodec.so)

add_library(
        swresample
        SHARED
        IMPORTED
)
#set_target_properties(avformat PROPERTIES LINKER_LANGUAGE CXX)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ${PATH_TO_FFMPEG}/${ANDROID_ABI}/libswresample.so)]]

add_library(
        Dubsmash
        SHARED
        src/main/cpp/Dubsmash.cpp
        ${PATH_TO_SUPERPOWERED}/AndroidIO/SuperpoweredAndroidAudioIO.cpp
)

add_library(
        Karaoke
        SHARED
        src/main/cpp/Karaoke.cpp
        ${PATH_TO_SUPERPOWERED}/AndroidIO/SuperpoweredAndroidAudioIO.cpp
)

add_library(
        Edit
        SHARED
        src/main/cpp/EditDubsmash.cpp
        ${PATH_TO_SUPERPOWERED}/AndroidIO/SuperpoweredAndroidAudioIO.cpp
)

# link the native library against the following libraries
target_link_libraries(
        Dubsmash
        ${PATH_TO_SUPERPOWERED}/libSuperpoweredAndroid${ANDROID_ABI}.a
        OpenSLES
        log
        android
)

target_link_libraries(
        Karaoke
        ${PATH_TO_SUPERPOWERED}/libSuperpoweredAndroid${ANDROID_ABI}.a
        OpenSLES
        log
        android
)

target_link_libraries(
        Edit
        #        avutil
        #        avformat
        #        avcodec
        ${PATH_TO_SUPERPOWERED}/libSuperpoweredAndroid${ANDROID_ABI}.a
        OpenSLES
        log
        android
)